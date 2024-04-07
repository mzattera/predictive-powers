/*
 * Copyright 2023 Massimiliano "Maxi" Zattera
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.mzattera.predictivepowers.openai.services;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

import io.github.mzattera.predictivepowers.openai.client.AzureOpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.embeddings.Embedding;
import io.github.mzattera.predictivepowers.openai.client.embeddings.EmbeddingsRequest;
import io.github.mzattera.predictivepowers.openai.client.embeddings.EmbeddingsResponse;
import io.github.mzattera.predictivepowers.services.AbstractEmbeddingService;
import io.github.mzattera.predictivepowers.services.EmbeddedText;
import io.github.mzattera.predictivepowers.services.ModelService;
import io.github.mzattera.predictivepowers.services.ModelService.Tokenizer;
import io.github.mzattera.util.ChunkUtil;
import io.github.mzattera.util.ExtractionUtil;
import lombok.Getter;
import lombok.NonNull;

/**
 * This class creates embeddings using OpenAI.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class OpenAiEmbeddingService extends AbstractEmbeddingService {

	public static final String DEFAULT_MODEL = "text-embedding-3-small";

	@NonNull
	@Getter
	protected final OpenAiEndpoint endpoint;

	@NonNull
	private final ModelService modelService;

	/**
	 * Register the deploy ID if we are running in MS Azure See
	 * {@link AzureOpenAiModelService}.
	 */
	private void register() {
		if (endpoint instanceof AzureOpenAiEndpoint) {
			String model = getModel();
			if (modelService.get(model) == null) {
				// Do a "fake" call to read base model ID (see AzureOpenAiModelService JavaDoc).
				EmbeddingsRequest req = EmbeddingsRequest.builder().model(model).build();
				req.getInput().add("x");
				EmbeddingsResponse resp = endpoint.getClient().createEmbeddings(req);
				((AzureOpenAiModelService)modelService).map(model, resp.getModel());
			}
		}
	}

	/**
	 * This request, with its parameters, is used as default setting for each call.
	 * 
	 * You can change any parameter to change these defaults (e.g. the model used)
	 * and the change will apply to all subsequent calls.
	 */
	@Getter
	@NonNull
	private final EmbeddingsRequest defaultReq;
	
	public OpenAiEmbeddingService(OpenAiEndpoint ep) {
		this(ep, DEFAULT_MODEL);
	}

	public OpenAiEmbeddingService(OpenAiEndpoint ep, @NonNull String model) {
		this(ep, EmbeddingsRequest.builder().model(DEFAULT_MODEL).build());
	}

	public OpenAiEmbeddingService(OpenAiEndpoint ep, EmbeddingsRequest embeddingsRequest) {
		this.endpoint = ep;
		this.defaultReq = embeddingsRequest;
		this.modelService = ep.getModelService();
		register();
	}

	@Override
	public String getModel() {
		return defaultReq.getModel();
	}

	@Override
	public void setModel(@NonNull String model) {
		defaultReq.setModel(model);
		register();
	}

	Integer dimensions = null;

	/**
	 * 
	 * @return Dimensions for the generated embeddings, or null if this is left to
	 *         model default.
	 */
	public Integer getDimensions() {
		return defaultReq.getDimensions();
	}

	/**
	 * Set dimensions for the generated embeddings, not all models support this.
	 * Refer to API documentation for possible values.
	 * 
	 * @param dimensions Dimensions for the generated embeddings, or null to use
	 *                   model default.
	 * @return
	 */
	public void setDimensions(Integer dimensions) {
		// See https://openai.com/blog/new-embedding-models-and-api-updates
		defaultReq.setDimensions(dimensions);
	}

	public List<EmbeddedText> embed(String text, EmbeddingsRequest req) {
		return embed(text, getDefaultTextTokens(), 1, 1, req);
	}

	// This is the only method that one must implement when extending
	// AbstractEmbeddingService, it makes sure the default request is passed
	@Override
	public List<EmbeddedText> embed(String text, int chunkSize, int windowSize, int stride) {
		return embed(text, chunkSize, windowSize, stride, defaultReq);
	}

	public List<EmbeddedText> embed(String text, int chunkSize, int windowSize, int stride, EmbeddingsRequest req) {

		String model = req.getModel();
		int modelSize = modelService.getContextSize(model);
		Tokenizer tokenizer = modelService.getTokenizer(model);

		// Chunk accordingly to user's instructions
		List<String> chunks = ChunkUtil.split(text, chunkSize, windowSize, stride, tokenizer);

		// Make sure no chunk is bigger than model's supported size
		List<String> tmp = new ArrayList<>(chunks.size() * 2);
		for (String c : chunks)
			tmp.addAll(ChunkUtil.split(c, modelSize, tokenizer));
		chunks = tmp;

		// Embed as many pieces you can in a single call
		req.getInput().clear();
		List<EmbeddedText> result = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		while (chunks.size() > 0) {
			String s = chunks.remove(0);

			sb.append(s);
			if (tokenizer.count(sb.toString()) > modelSize) {
				// too many tokens, embed what you have
				result.addAll(embed(req));
				req.getInput().clear();
				sb.setLength(0);
			}

			// add to next request
			req.getInput().add(s);
		}

		// last bit
		if (req.getInput().size() > 0) {
			result.addAll(embed(req));
		}

		return result;
	}

	public List<EmbeddedText> embed(Collection<String> text, EmbeddingsRequest req) {
		return embed(text, getDefaultTextTokens(), 1, 1, req);
	}

	public List<EmbeddedText> embed(Collection<String> text, int chunkSize, int windowSize, int stride,
			EmbeddingsRequest req) {
		List<EmbeddedText> result = new ArrayList<>();
		for (String s : text)
			result.addAll(embed(s, chunkSize, windowSize, stride, req));
		return result;

	}

	public List<EmbeddedText> embedFile(File file, EmbeddingsRequest req)
			throws IOException, SAXException, TikaException {
		return embed(ExtractionUtil.fromFile(file), req);
	}

	public Map<File, List<EmbeddedText>> embedFolder(File folder, EmbeddingsRequest req)
			throws IOException, SAXException, TikaException {
		if (!folder.isDirectory() || !folder.canRead()) {
			throw new IOException("Cannot read folder: " + folder.getCanonicalPath());
		}

		Map<File, List<EmbeddedText>> result = new HashMap<>();
		for (File f : folder.listFiles()) {
			if (f.isFile())
				result.put(f, embedFile(f, req));
			else
				result.putAll(embedFolder(f, req));
		}

		return result;
	}

	public List<EmbeddedText> embedURL(String url, EmbeddingsRequest req)
			throws MalformedURLException, IOException, SAXException, TikaException, URISyntaxException {
		return embedURL((new URI(url)).toURL(), req);
	}

	public List<EmbeddedText> embedURL(URL url, EmbeddingsRequest req) throws IOException, SAXException, TikaException {
		return embed(ExtractionUtil.fromUrl(url), req);
	}

	private List<EmbeddedText> embed(EmbeddingsRequest req) {

		List<EmbeddedText> result = new ArrayList<>(req.getInput().size());
		EmbeddingsResponse res = endpoint.getClient().createEmbeddings(req);

		for (Embedding e : res.getData()) {
			int index = e.getIndex();
			
			// TODO URGENT do not use the model use the "base" model returned by ModelService, to cover fro azure cases
			
			EmbeddedText et = EmbeddedText.builder().text(req.getInput().get(index)).embedding(e.getEmbedding())
					.model(res.getModel()).build();
			result.add(et);
		}

		return result;
	}	
}
