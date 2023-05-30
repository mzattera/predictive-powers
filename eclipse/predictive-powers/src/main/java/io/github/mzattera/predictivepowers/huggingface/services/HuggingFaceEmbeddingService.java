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
package io.github.mzattera.predictivepowers.huggingface.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.github.mzattera.predictivepowers.huggingface.client.HuggingFaceRequest;
import io.github.mzattera.predictivepowers.huggingface.endpoint.HuggingFaceEndpoint;
import io.github.mzattera.predictivepowers.services.AbstractEmbeddingService;
import io.github.mzattera.predictivepowers.services.EmbeddedText;
import io.github.mzattera.util.LlmUtil;
import lombok.Getter;
import lombok.NonNull;

/**
 * This class creates embeddings using Hugging Face.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class HuggingFaceEmbeddingService extends AbstractEmbeddingService {

	// TODO allow setting options in HuggingFaceRequest

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
	private final HuggingFaceEndpoint endpoint;

	@Override
	public List<EmbeddedText> embed(Collection<String> text) {
		List<EmbeddedText> result = new ArrayList<>();
		
		// Put all pieces of text to be embedded in a list
		List<String> l = new ArrayList<>();
		for (String s : text) {
			l.addAll(LlmUtil.split(s, getMaxTextTokens()));
		}

		HuggingFaceRequest req = new HuggingFaceRequest();
		req.getInputs().addAll(l);
		req.getOptions().setWaitForModel(true); // TODO remove? Improve?

		List<List<Double>> resp = endpoint.getClient().featureExtraction(getModel(), req);
		for (int i = 0; i < req.getInputs().size(); ++i) {
			EmbeddedText e = EmbeddedText.builder().embedding(resp.get(i)).model(getModel())
					.text(req.getInputs().get(i)).build();
			result.add(e);
		}

		return result;
	}
}
