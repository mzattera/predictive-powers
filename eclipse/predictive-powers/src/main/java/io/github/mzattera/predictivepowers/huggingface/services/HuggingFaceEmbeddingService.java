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
import java.util.List;

import io.github.mzattera.predictivepowers.huggingface.client.HuggingFaceEndpoint;
import io.github.mzattera.predictivepowers.huggingface.client.HuggingFaceRequest;
import io.github.mzattera.predictivepowers.huggingface.client.Options;
import io.github.mzattera.predictivepowers.services.AbstractEmbeddingService;
import io.github.mzattera.predictivepowers.services.EmbeddedText;
import io.github.mzattera.predictivepowers.util.ChunkUtil;
import lombok.Getter;
import lombok.NonNull;

/**
 * This class creates embeddings using Hugging Face.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class HuggingFaceEmbeddingService extends AbstractEmbeddingService {

//	public static final String DEFAULT_MODEL = "sentence-transformers/all-MiniLM-L6-v2"; // This is sentence similarity
//	public static final String DEFAULT_MODEL = "facebook/bart-large"; // This is word embedding

	// This was the first one I found that works fine
	public static final String DEFAULT_MODEL = "guidecare/all-mpnet-base-v2-feature-extraction";

	public HuggingFaceEmbeddingService(HuggingFaceEndpoint ep) {
		this.endpoint = ep;
		setModel(DEFAULT_MODEL);
	}

	@NonNull
	@Getter
	protected final HuggingFaceEndpoint endpoint;

	@Override
	public List<EmbeddedText> embed(String text, int chunkSize, int windowSize, int stride) {

		// Chunk accordingly to user's instructions
		// Notice there is no limit in size of the input in this case
		List<String> chunks = ChunkUtil.split(text, chunkSize, windowSize, stride);

		List<EmbeddedText> result = new ArrayList<>(chunks.size());

		// TODO replace with defaultReq instead
		HuggingFaceRequest req = HuggingFaceRequest.builder() //
				.inputs(chunks) //
				.options(new Options(true, true)) //
				.build();

		List<List<Double>> resp = endpoint.getClient().featureExtraction(getModel(), req);
		for (int i = 0; i < req.getInputs().size(); ++i) {
			EmbeddedText e = EmbeddedText.builder() //
					.embedding(resp.get(i)) //
					.model(getModel()) //
					.text(req.getInputs().get(i)).build();
			result.add(e);
		}

		return result;
	}
}
