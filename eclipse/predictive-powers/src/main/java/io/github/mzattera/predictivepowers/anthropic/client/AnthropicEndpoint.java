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

package io.github.mzattera.predictivepowers.anthropic.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mzattera.predictivepowers.AiEndpoint;
import io.github.mzattera.predictivepowers.anthropic.services.AnthropicChatService;
import io.github.mzattera.predictivepowers.anthropic.services.AnthropicModelService;
import io.github.mzattera.predictivepowers.services.AgentService;
import io.github.mzattera.predictivepowers.services.CompletionService;
import io.github.mzattera.predictivepowers.services.EmbeddingService;
import io.github.mzattera.predictivepowers.services.ImageGenerationService;
import io.github.mzattera.predictivepowers.services.QuestionAnsweringService;
import io.github.mzattera.predictivepowers.services.QuestionExtractionService;
import lombok.Getter;
import lombok.NonNull;

/**
 * This represents an ANTHROP\C endpoint, from which services can be created.
 * 
 * This class is thread-safe.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
public class AnthropicEndpoint implements AiEndpoint {

	// TODO always ensure it returns specific services

	private final static Logger LOG = LoggerFactory.getLogger(AnthropicEndpoint.class);

	@Getter
	private final AnthropicClient client;

	public AnthropicEndpoint() {
		this(new AnthropicClient());
	}

	public AnthropicEndpoint(String apiKey) {
		this(new AnthropicClient(apiKey));
	}

	public AnthropicEndpoint(@NonNull AnthropicClient client) {
		this.client = client;
	}

	@Getter
	private final AnthropicModelService modelService = new AnthropicModelService(this);

	@Override
	public CompletionService getCompletionService() {
		throw new UnsupportedOperationException();
	}

	@Override
	public CompletionService getCompletionService(@NonNull String model) {
		throw new UnsupportedOperationException();
	}

	@Override
	public EmbeddingService getEmbeddingService() {
		throw new UnsupportedOperationException();
	}

	@Override
	public EmbeddingService getEmbeddingService(@NonNull String model) {
		throw new UnsupportedOperationException();
	}

	@Override
	public AnthropicChatService getChatService() {
		return new AnthropicChatService(this);
	}

	@Override
	public AnthropicChatService getChatService(@NonNull String model) {
		return new AnthropicChatService(this, model);
	}

	@Override
	public AgentService getAgentService() {
		// TODO URGENT implement
		throw new UnsupportedOperationException();
	}

	@Override
	public AgentService getAgentService(@NonNull String model) {
		// TODO URGENT implement
		throw new UnsupportedOperationException();
	}

	@Override
	public QuestionExtractionService getQuestionExtractionService() {
		// TODO URGENT implement
		throw new UnsupportedOperationException();
	}

	@Override
	public QuestionExtractionService getQuestionExtractionService(@NonNull String model) {
		// TODO URGENT implement
		throw new UnsupportedOperationException();
	}

	@Override
	public QuestionAnsweringService getQuestionAnsweringService() {
		// TODO URGENT implement
		throw new UnsupportedOperationException();
	}

	@Override
	public QuestionAnsweringService getQuestionAnsweringService(@NonNull String model) {
		// TODO URGENT implement
		throw new UnsupportedOperationException();
	}

	@Override
	public ImageGenerationService getImageGenerationService() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ImageGenerationService getImageGenerationService(@NonNull String model) {
		throw new UnsupportedOperationException();
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
