/*
 * Copyright 2023-2024 Massimiliano "Maxi" Zattera
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
package io.github.mzattera.predictivepowers.huggingface.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import io.github.mzattera.hfinferenceapi.client.model.EmbeddingData;
import io.github.mzattera.hfinferenceapi.client.model.EmbeddingsRequest;
import io.github.mzattera.hfinferenceapi.client.model.EmbeddingsRequest.TruncationDirectionEnum;
import io.github.mzattera.predictivepowers.EndpointException;
import io.github.mzattera.predictivepowers.huggingface.util.HuggingFaceUtil;
import io.github.mzattera.predictivepowers.services.AbstractEmbeddingService;
import io.github.mzattera.predictivepowers.services.EmbeddedText;
import io.github.mzattera.predictivepowers.services.ModelService;
import io.github.mzattera.predictivepowers.services.ModelService.Tokenizer;
import io.github.mzattera.predictivepowers.util.CharTokenizer;
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
public class HuggingFaceEmbeddingService extends AbstractEmbeddingService {

//	private final static Logger LOG = LoggerFactory.getLogger(HuggingFaceEmbeddingService.class);

	public static final String DEFAULT_MODEL = "Qwen/Qwen3-Embedding-8B:nebius";

	/** How many strings we can embed at once */
	public static final int MAX_INPUTS_PER_CALL = 1024;

	@NonNull
	@Getter
	protected final HuggingFaceEndpoint endpoint;

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
	private EmbeddingsRequest defaultRequest;

	@Override
	public String getModel() {
		return defaultRequest.getModel();
	}

	@Override
	public void setModel(@NonNull String model) {
		defaultRequest.setModel(model);
	}

	public HuggingFaceEmbeddingService(HuggingFaceEndpoint ep) {
		this(ep, DEFAULT_MODEL);
	}

	public HuggingFaceEmbeddingService(HuggingFaceEndpoint ep, @NonNull String model) {
		this.endpoint = ep;
		this.defaultRequest = new EmbeddingsRequest().truncate(true).truncationDirection(TruncationDirectionEnum.RIGHT).model(model);
		this.modelService = ep.getModelService();
	}

	@Override
	public List<EmbeddedText> embed(@NonNull Collection<String> text, int chunkSize, int windowSize, int stride)
			throws EndpointException {

		// Tries to get a tokenizer, falling back to char tokenizer :(
		String model = defaultRequest.getModel();
		Tokenizer tokenizer = modelService.getTokenizer(model, CharTokenizer.getInstance());

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
	}

	private List<EmbeddedText> embed(List<String> input) throws EndpointException {

		String model = defaultRequest.getModel();
		try {
			List<EmbeddedText> result = new ArrayList<>();

			String[] parts = HuggingFaceUtil.parseModel(model);
			defaultRequest.setInput(input);
			defaultRequest.setModel(parts[0]);

			for (EmbeddingData e : endpoint.getClient().featureExtraction(parts[1], defaultRequest).getData()) {
				int index = e.getIndex();
				EmbeddedText et = EmbeddedText.builder() //
						.text(input.get(index)) //
						.embedding(e.getEmbedding().stream().map(f -> f.doubleValue()).collect(Collectors.toList())) //
						.model(model).build();
				result.add(et);
			}

			return result;
		} catch (Exception e) {
			throw HuggingFaceUtil.toEndpointException(e);
		} finally {
			// Restore model value
			defaultRequest.setModel(model);
		}
	}
}
