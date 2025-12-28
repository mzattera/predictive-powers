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
package io.github.mzattera.predictivepowers.openai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openai.core.JsonMissing;
import com.openai.models.embeddings.CreateEmbeddingResponse;
import com.openai.models.embeddings.Embedding;
import com.openai.models.embeddings.EmbeddingCreateParams;
import com.openai.models.embeddings.EmbeddingCreateParams.EncodingFormat;

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
 * This class creates embeddings using OpenAI.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class OpenAiEmbeddingService extends AbstractEmbeddingService {

	private final static Logger LOG = LoggerFactory.getLogger(OpenAiEmbeddingService.class);

	public static final String DEFAULT_MODEL = "text-embedding-3-small";

	@NonNull
	@Getter
	protected final OpenAiEndpoint endpoint;

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
	private EmbeddingCreateParams defaultRequest;

	@Override
	public String getModel() {
		return defaultRequest.model().asString();
	}

	@Override
	public void setModel(@NonNull String model) {
		defaultRequest = defaultRequest.toBuilder().model(model).build();
	}

	protected OpenAiEmbeddingService(OpenAiEndpoint ep) {
		this(ep, DEFAULT_MODEL);
	}

	@SuppressWarnings("unchecked")
	protected OpenAiEmbeddingService(OpenAiEndpoint ep, @NonNull String model) {
		this.endpoint = ep;
		this.defaultRequest = EmbeddingCreateParams.builder() //
				.model(model) //
				.input(JsonMissing.of()) //
				.encodingFormat(EncodingFormat.FLOAT).build();
		this.modelService = ep.getModelService();
	}

	@Override
	public List<EmbeddedText> embed(@NonNull Collection<String> text, int chunkSize, int windowSize, int stride)
			throws EndpointException {
		try {
			String model = defaultRequest.model().toString();
			int modelSize = modelService.getContextSize(model);
			Tokenizer tokenizer = modelService.getTokenizer(model);

			// Chunk accordingly to user's instructions
			List<String> chunks = new ArrayList<>();
			for (String t : text)
				chunks.addAll(ChunkUtil.split(t, chunkSize, windowSize, stride, tokenizer));

			// Make sure no chunk is bigger than model's supported size
			List<String> tmp = new ArrayList<>(chunks.size() * 2);
			for (String c : chunks)
				tmp.addAll(ChunkUtil.split(c, modelSize, tokenizer));
			chunks = tmp;

			// Notice ChunkUtil removes empty strings already

			// Embed as many pieces you can in a single call
			List<String> input = new ArrayList<>();
			List<EmbeddedText> result = new ArrayList<>();
			StringBuilder sb = new StringBuilder();
			while (chunks.size() > 0) {
				String s = chunks.remove(0);

				sb.append(s);
				// We have at most 2048 inputs or 300K tokens (with a 20tk overhead per input)
				// ..somehow the prompt token calculation counts even more tokens; 128K seems to
				// provide a very safe limit
				// https://platform.openai.com/docs/api-reference/embeddings/create
				if ((input.size() == 2048) || ((tokenizer.count(sb.toString()) + 20 * input.size()) > 128_000)) {
					// too many tokens, embed what you have
					result.addAll(embed(input));
					input.clear();
					sb.setLength(0);
					sb.append(s);
				}

				// add to next request
				input.add(s);
			}

			// last bit
			if (input.size() > 0) {
				result.addAll(embed(input));
			}

			return result;
		} catch (Exception e) {
			throw OpenAiUtil.toEndpointException(e);
		}
	}

	private List<EmbeddedText> embed(List<String> input) {

		List<EmbeddedText> result = new ArrayList<>();

		EmbeddingCreateParams req = defaultRequest.toBuilder().inputOfArrayOfStrings(input).build();
		CreateEmbeddingResponse res = endpoint.getClient().embeddings().create(req);
		LOG.info("Called OpenAI Embedding Service: " + res.usage());

		for (Embedding e : res.data()) {
			int index = (int) e.index();
			EmbeddedText et = EmbeddedText.builder() //
					.text(input.get(index)) //
					.embedding(e.embedding().stream().map(f -> f.doubleValue()).collect(Collectors.toList())) //
					.model(res.model()).build();
			result.add(et);
		}

		return result;
	}
}
