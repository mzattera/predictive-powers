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
 */package io.github.mzattera.predictivepowers.openai.services;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

import io.github.mzattera.predictivepowers.openai.client.embeddings.Embedding;
import io.github.mzattera.predictivepowers.openai.client.embeddings.EmbeddingsRequest;
import io.github.mzattera.predictivepowers.openai.client.embeddings.EmbeddingsResponse;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.util.ModelUtil;
import io.github.mzattera.predictivepowers.openai.util.TokenUtil;
import io.github.mzattera.predictivepowers.services.AbstractEmbeddingService;
import io.github.mzattera.predictivepowers.services.EmbeddedText;
import io.github.mzattera.util.ExtractionUtil;
import io.github.mzattera.util.LlmUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * This class creates embeddings using OpenAI.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@RequiredArgsConstructor
public class OpenAiEmbeddingService extends AbstractEmbeddingService {

	public static final String DEFAULT_MODEL = "text-embedding-ada-002";

	public OpenAiEmbeddingService(OpenAiEndpoint ep) {
		this(ep, new EmbeddingsRequest());
		defaultReq.setModel(DEFAULT_MODEL);
	}

	@NonNull
	@Getter
	private final OpenAiEndpoint endpoint;

	/**
	 * This request, with its parameters, is used as default setting for each call.
	 * 
	 * You can change any parameter to change these defaults (e.g. the model used)
	 * and the change will apply to all subsequent calls.
	 */
	@Getter
	@NonNull
	protected final EmbeddingsRequest defaultReq;

	@Override
	public String getModel() {
		return defaultReq.getModel();
	}

	@Override
	public void setModel(@NonNull String model) {
		defaultReq.setModel(model);		
	}

	@Override
	public void setMaxTokens(int maxTokens) {
		if ((maxTokens <= 0) || (maxTokens > 8192))
			// TODO is this a limitation of the model? 
			// I think the API description mention this, but it might be assuming user is using text-embedding-ada-002
			throw new IllegalArgumentException("maxTokens must be 0 < maxTokens <= 8192: " + maxTokens);

		super.setMaxTokens(maxTokens);
	}

	/**
	 * Create embeddings for given text.
	 * 
	 * As there is a default maximum length for text, it might be split into several
	 * parts before embeddings are returned.
	 * 
	 * It uses parameters specified in {@link #getDefaultReq()}.
	 */
	@Override
	public List<EmbeddedText> embed(String text) {
		return embed(text, defaultReq);
	}

	/**
	 * Create embeddings for given text.
	 * 
	 * As there is a default maximum length for text, it might be split into several
	 * parts before embeddings are returned.
	 * 
	 * It uses parameters specified in given {@link EmbeddingsRequest}.
	 * 
	 * @param maxTextTokens Maximum length in tokens of each piece of text to be
	 *                      embedded. Text is split accordingly, if needed.
	 */
	public List<EmbeddedText> embed(String text, EmbeddingsRequest req) {
		List<String> l = new ArrayList<>();
		l.add(text);
		return embed(l, req);
	}

	/**
	 * Create embeddings for given text.
	 * 
	 * As there is a default maximum length for text, each element might be split
	 * into several parts before embeddings are returned.
	 * 
	 * It uses parameters specified in {@link #getDefaultReq()}.
	 */
	@Override
	public List<EmbeddedText> embed(Collection<String> text) {
		return embed(text, defaultReq);
	}

	/**
	 * Create embeddings for given text.
	 * 
	 * As there is a default maximum length for text, each element might be split
	 * into several parts before embeddings are returned.
	 * 
	 * It uses parameters specified in given {@link EmbeddingsRequest}.
	 * 
	 * @param maxTextTokens Maximum length in tokens of each piece of text to be
	 *                      embedded (in tokens). Text is split accordingly, if
	 *                      needed.
	 */
	public List<EmbeddedText> embed(Collection<String> text, EmbeddingsRequest req) {

		int modelSize = Math.min(ModelUtil.getContextSize(req.getModel()), 8192);

		req.getInput().clear();

		// Put all pieces of text to be embedded in a list
		List<String> l = new ArrayList<>();
		for (String s : text) {
			l.addAll(LlmUtil.split(s, getMaxTextTokens()));
		}

		// Embed as many pieces you can in a single call
		List<EmbeddedText> result = new ArrayList<>();
		int tok = 0;
		while (l.size() > 0) {
			String s = l.remove(0);
			int t = TokenUtil.count(s);

			if ((tok + t) > modelSize) {
				// too many tokens, embed what you have
				result.addAll(embed(req));
				req.getInput().clear();
				tok = 0;
			}

			// add to next request
			req.getInput().add(s);
			tok += t;
		}

		// last bit
		if (req.getInput().size() > 0) {
			result.addAll(embed(req));
		}

		return result;
	}

	private List<EmbeddedText> embed(EmbeddingsRequest req) {

		List<EmbeddedText> result = new ArrayList<>(req.getInput().size());
		EmbeddingsResponse res = endpoint.getClient().createEmbeddings(req);

		for (Embedding e : res.getData()) {
			int index = e.getIndex();
			EmbeddedText et = EmbeddedText.builder().text(req.getInput().get(index)).embedding(e.getEmbedding())
					.model(res.getModel()).build();
			result.add(et);
		}

		return result;
	}

	/**
	 * Embed content of given file.
	 * 
	 * @param file
	 * @throws IOException
	 * @throws SAXException
	 * @throws TikaException
	 */
	@Override
	public List<EmbeddedText> embedFile(File file) throws IOException, SAXException, TikaException {
		return embedFile(file, defaultReq);
	}

	/**
	 * Embed content of given file.
	 * 
	 * @param file
	 * @throws IOException
	 * @throws SAXException
	 * @throws TikaException
	 */
	public List<EmbeddedText> embedFile(File file, EmbeddingsRequest req)
			throws IOException, SAXException, TikaException {
		String content = ExtractionUtil.fromFile(file);
		return embed(content, req);
	}

	/**
	 * Embeds all files in given folder, including contents of its sub-folders.
	 * 
	 * @param folderName
	 * @return
	 * @throws IOException
	 * @throws SAXException
	 * @throws TikaException
	 * @returns A Map from each embedded file into its contents.
	 */
	@Override
	public Map<File, List<EmbeddedText>> embedFolder(File folder) throws IOException, SAXException, TikaException {
		return embedFolder(folder, defaultReq);
	}

	/**
	 * Embeds all files in given folder, including contents of its sub-folders.
	 * 
	 * @param folderName
	 * @return
	 * @throws IOException
	 * @throws SAXException
	 * @throws TikaException
	 * @returns A Map from each embedded file into its contents.
	 */
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

	/**
	 * Embeds text of given web page.
	 * 
	 * @throws TikaException
	 * @throws SAXException
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	@Override
	public List<EmbeddedText> embedURL(String url)
			throws MalformedURLException, IOException, SAXException, TikaException {
		return embedURL(new URL(url), defaultReq);
	}

	/**
	 * Embeds text of given web page.
	 * 
	 * @throws TikaException
	 * @throws SAXException
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	public List<EmbeddedText> embedURL(String url, EmbeddingsRequest req)
			throws MalformedURLException, IOException, SAXException, TikaException {
		return embedURL(new URL(url), req);
	}

	/**
	 * Embeds text of given web page.
	 * 
	 * @throws TikaException
	 * @throws SAXException
	 * @throws IOException
	 */
	@Override
	public List<EmbeddedText> embedURL(URL url) throws IOException, SAXException, TikaException {
		return embedURL(url, defaultReq);
	}

	/**
	 * Embeds text of given web page.
	 * 
	 * @throws TikaException
	 * @throws SAXException
	 * @throws IOException
	 */
	public List<EmbeddedText> embedURL(URL url, EmbeddingsRequest req) throws IOException, SAXException, TikaException {
		return embed(ExtractionUtil.fromUrl(url));
	}

}
