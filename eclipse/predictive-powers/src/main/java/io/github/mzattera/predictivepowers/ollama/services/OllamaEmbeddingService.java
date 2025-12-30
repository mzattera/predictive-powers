/*
 * Copyright 2023-2025 Massimiliano "Maxi" Zattera
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
package io.github.mzattera.predictivepowers.ollama.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import io.github.mzattera.ollama.client.model.EmbedRequest;
import io.github.mzattera.ollama.client.model.EmbedResponse;
import io.github.mzattera.predictivepowers.EndpointException;
import io.github.mzattera.predictivepowers.services.AbstractEmbeddingService;
import io.github.mzattera.predictivepowers.services.EmbeddedText;
import io.github.mzattera.predictivepowers.services.ModelService;
import io.github.mzattera.predictivepowers.services.ModelService.Tokenizer;
import io.github.mzattera.predictivepowers.util.ChunkUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * This class creates embeddings using Ollama.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class OllamaEmbeddingService extends AbstractEmbeddingService {

	// TODO URGENT we do not have such a6 thing...either we take the first or
	// pretend user specifies a model
	public static final String DEFAULT_MODEL = "embeddinggemma:300m";

	/** How many strings we can embed at once */
	public static final int MAX_INPUTS_PER_CALL = 1024;

	@NonNull
	@Getter
	protected final OllamaEndpoint endpoint;

	@NonNull
	private final ModelService modelService;

	/**
	 * This request, with its parameters, is used as default setting for each call.
	 * 
	 * You can change any parameter to change these defaults (e.g. the model used)
	 * and the change will apply to all subsequent calls.
	 */
	@Getter
	@Setter
	@NonNull
	private EmbedRequest defaultRequest;

	@Override
	public String getModel() {
		return defaultRequest.getModel();
	}

	@Override
	public void setModel(@NonNull String model) {
		defaultRequest.setModel(model);
	}

	protected OllamaEmbeddingService(OllamaEndpoint ep) {
		this(ep, DEFAULT_MODEL);
	}

	protected OllamaEmbeddingService(OllamaEndpoint ep, @NonNull String model) {
		this.endpoint = ep;
		this.defaultRequest = new EmbedRequest().truncate(true).model(model);
		this.modelService = ep.getModelService();
	}

	@Override
	public List<EmbeddedText> embed(@NonNull Collection<String> text, int chunkSize, int windowSize, int stride)
			throws EndpointException {
		try {
			// Tries to get a tokenizer, falling back to char tokenizer :(
			String model = defaultRequest.getModel();
			Tokenizer tokenizer = modelService.getTokenizer(model, OllamaModelService.FALLBACK_TOKENIZER);

			// Chunk accordingly to user's instructions
			List<String> chunks = new ArrayList<>();
			for (String t : text)
				chunks.addAll(ChunkUtil.split(t, chunkSize, windowSize, stride, tokenizer));

			// Make sure no chunk is bigger than model's supported size
			int modelSize = modelService.getContextSize(model, -1);
			if (modelSize > 0) {
				List<String> tmp = new ArrayList<>(chunks.size() * 2);
				for (String c : chunks)
					tmp.addAll(ChunkUtil.split(c, modelSize, tokenizer));
				chunks = tmp;
			}

			// Embed as many pieces you can in a single call
			List<String> input = new ArrayList<>();
			List<EmbeddedText> result = new ArrayList<>();
			while (chunks.size() > 0) {
				input.add(chunks.remove(0));
				if (input.size() == MAX_INPUTS_PER_CALL) {
					// too many tokens, embed what you have
					result.addAll(embed(input));
					input.clear();
				}
			}

			// last bit
			if (input.size() > 0) {
				result.addAll(embed(input));
			}

			return result;
		} catch (Exception e) {
			throw OllamaUtil.toEndpointException(e);
		}
	}

	private List<EmbeddedText> embed(List<String> input) {

		defaultRequest.setInput(input);
		EmbedResponse resp = endpoint.getClient().embed(defaultRequest);

		if (resp.getEmbeddings().size() != input.size())
			throw new EndpointException("Number of embeddings does not match number of input strings");

		List<EmbeddedText> result = new ArrayList<>(input.size());
		for (int i = 0; i < input.size(); ++i) {
			EmbeddedText emb = EmbeddedText.builder() //
					.embedding(resp.getEmbeddings().get(i).stream().map(f -> Double.valueOf(f))
							.collect(Collectors.toList())) //
					.model(defaultRequest.getModel()) //
					.text(input.get(i)).build();
			result.add(emb);
		}

		return result;
	}
}
