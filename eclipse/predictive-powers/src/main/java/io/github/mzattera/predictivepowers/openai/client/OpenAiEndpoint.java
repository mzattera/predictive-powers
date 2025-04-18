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

package io.github.mzattera.predictivepowers.openai.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mzattera.predictivepowers.AiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiAgentService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiCompletionService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiEmbeddingService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiImageGenerationService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiQuestionAnsweringService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiQuestionExtractionService;
import lombok.Getter;
import lombok.NonNull;

/**
 * This represents an OpenAI API endpoint, from which services can be created.
 * 
 * This class is thread-safe.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
public class OpenAiEndpoint implements AiEndpoint {

	private final static Logger LOG = LoggerFactory.getLogger(OpenAiEndpoint.class);

	@Getter
	private final OpenAiClient client;

	public OpenAiEndpoint() {
		this(new OpenAiClient());
	}

	public OpenAiEndpoint(String apiKey) {
		this(new OpenAiClient(apiKey));
	}

	public OpenAiEndpoint(@NonNull OpenAiClient client) {
		this.client = client;
	}

	// Mode service is state-less, we do not need to create a new model service each
	// time.
	private final OpenAiModelService modelService = new OpenAiModelService(this);

	@Override
	public OpenAiModelService getModelService() {
		return modelService;
	}

	@Override
	public OpenAiCompletionService getCompletionService() {
		return new OpenAiCompletionService(this);
	}

	@Override
	public OpenAiCompletionService getCompletionService(@NonNull String model) {
		return new OpenAiCompletionService(this, model);
	}

	@Override
	public OpenAiEmbeddingService getEmbeddingService() {
		return new OpenAiEmbeddingService(this);
	}

	@Override
	public OpenAiEmbeddingService getEmbeddingService(@NonNull String model) {
		return new OpenAiEmbeddingService(this, model);
	}

	@Override
	public OpenAiChatService getChatService() {
		return new OpenAiChatService(this);
	}

	@Override
	public OpenAiChatService getChatService(@NonNull String model) {
		return new OpenAiChatService(this, model);
	}

	@Override
	public OpenAiQuestionExtractionService getQuestionExtractionService() {
		return new OpenAiQuestionExtractionService(this);
	}

	@Override
	public OpenAiQuestionExtractionService getQuestionExtractionService(@NonNull String model) {
		return new OpenAiQuestionExtractionService(this, model);
	}

	@Override
	public OpenAiQuestionAnsweringService getQuestionAnsweringService() {
		return new OpenAiQuestionAnsweringService(this);
	}

	@Override
	public OpenAiQuestionAnsweringService getQuestionAnsweringService(@NonNull String model) {
		return new OpenAiQuestionAnsweringService(this, model);
	}

	@Override
	public OpenAiImageGenerationService getImageGenerationService() {
		return new OpenAiImageGenerationService(this);
	}

	@Override
	public OpenAiImageGenerationService getImageGenerationService(@NonNull String model) {
		return new OpenAiImageGenerationService(this, model);
	}

	@Override
	public OpenAiAgentService getAgentService() {
		return new OpenAiAgentService(this);
	}

	@Override
	public OpenAiAgentService getAgentService(@NonNull String model) {
		return new OpenAiAgentService(this, model);
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
