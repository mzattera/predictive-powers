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

package io.github.mzattera.predictivepowers.huggingface.endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mzattera.predictivepowers.Endpoint;
import io.github.mzattera.predictivepowers.huggingface.client.HuggingFaceClient;
import io.github.mzattera.predictivepowers.huggingface.client.nlp.TextGenerationRequest;
import io.github.mzattera.predictivepowers.huggingface.services.HuggingFaceChatService;
import io.github.mzattera.predictivepowers.huggingface.services.HuggingFaceCompletionService;
import io.github.mzattera.predictivepowers.huggingface.services.HuggingFaceEmbeddingService;
import io.github.mzattera.predictivepowers.huggingface.services.HuggingFaceImageGenerationService;
import io.github.mzattera.predictivepowers.huggingface.services.HuggingFaceModelService;
import io.github.mzattera.predictivepowers.huggingface.services.HuggingFaceQuestionAnsweringService;
import io.github.mzattera.predictivepowers.services.QuestionExtractionService;
import lombok.Getter;
import lombok.NonNull;

/**
 * This represents a Hugging Face endpoint, from which services can be created.
 * 
 * This class is thread-safe.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
public class HuggingFaceEndpoint implements Endpoint {

	// TODO always ensure it returns Hugging Face specific services

	private final static Logger LOG = LoggerFactory.getLogger(HuggingFaceEndpoint.class);

	@Getter
	private final HuggingFaceClient client;

	public HuggingFaceEndpoint() {
		this(new HuggingFaceClient());
	}

	public HuggingFaceEndpoint(String apiKey) {
		this(new HuggingFaceClient(apiKey, -1, -1, -1));
	}

	public HuggingFaceEndpoint(@NonNull HuggingFaceClient client) {
		this.client = client;
	}

	@Override
	public HuggingFaceModelService getModelService() {
		return new HuggingFaceModelService(this);
	}

	@Override
	public HuggingFaceCompletionService getCompletionService() {
		return new HuggingFaceCompletionService(this);
	}

	public HuggingFaceCompletionService getCompletionService(TextGenerationRequest defaultReq) {
		return new HuggingFaceCompletionService(this, defaultReq);
	}

	@Override
	public HuggingFaceEmbeddingService getEmbeddingService() {
		return new HuggingFaceEmbeddingService(this);
	}

	@Override
	public HuggingFaceChatService getChatService() {
		return new HuggingFaceChatService(this);
	}

	@Override
	public HuggingFaceChatService getChatService(String personality) {
		HuggingFaceChatService s = getChatService();
		s.setPersonality(personality);
		return s;
	}

	@Override
	public QuestionExtractionService getQuestionExtractionService() {
		throw new UnsupportedOperationException();
	}

	@Override
	public HuggingFaceQuestionAnsweringService getQuestionAnsweringService() {
		return new HuggingFaceQuestionAnsweringService(this);
	}

	@Override
	public HuggingFaceImageGenerationService getImageGenerationService() {
		return new HuggingFaceImageGenerationService(this);
	}

	@Override
	public synchronized void close() {
		try {
			client.close();
		} catch (Exception e) {
			LOG.warn("Error while closing endpoint", e);
		}
	}
}
