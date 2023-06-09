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

package io.github.mzattera.predictivepowers.openai.endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mzattera.predictivepowers.AiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.embeddings.EmbeddingsRequest;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiCompletionService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiEmbeddingService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiImageGenerationService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiQuestionAnsweringService;
import io.github.mzattera.predictivepowers.services.QuestionExtractionService;
import lombok.Getter;
import lombok.NonNull;

/**
 * This represents an OpenAI endpoint, from which services can be created.
 * 
 * This class is thread-safe.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
public class OpenAiEndpoint implements AiEndpoint {

	// TODO add client/endpoint for Azure OpenAI Services
	// TODO always ensure it returns OpenAI specific services

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

	@Override
	public OpenAiModelService getModelService() {
		return new OpenAiModelService(this);
	}

	@Override
	public OpenAiCompletionService getCompletionService() {
		return new OpenAiCompletionService(this);
	}

	public OpenAiCompletionService getCompletionService(CompletionsRequest defaultReq) {
		return new OpenAiCompletionService(this, defaultReq);
	}

	@Override
	public OpenAiEmbeddingService getEmbeddingService() {
		return new OpenAiEmbeddingService(this);
	}

	public OpenAiEmbeddingService getEmbeddingService(@NonNull EmbeddingsRequest defaultReq) {
		return new OpenAiEmbeddingService(this, defaultReq);
	}

	@Override
	public OpenAiChatService getChatService() {
		return new OpenAiChatService(this);
	}

	@Override
	public OpenAiChatService getChatService(String personality) {
		OpenAiChatService s = getChatService();
		s.setPersonality(personality);
		return s;
	}

	public OpenAiChatService getChatService(ChatCompletionsRequest defaultReq) {
		return new OpenAiChatService(this, defaultReq);
	}

	public OpenAiChatService getChatService(ChatCompletionsRequest defaultReq, String personality) {
		OpenAiChatService s = getChatService(defaultReq);
		s.setPersonality(personality);
		return s;
	}

	@Override
	public QuestionExtractionService getQuestionExtractionService() {
		return new QuestionExtractionService(this);
	}

	public QuestionExtractionService getQuestionExtractionService(@NonNull OpenAiChatService cs) {
		return new QuestionExtractionService(cs);
	}

	@Override
	public OpenAiQuestionAnsweringService getQuestionAnsweringService() {
		return new OpenAiQuestionAnsweringService(this);
	}

	public OpenAiQuestionAnsweringService getQuestionAnsweringService(@NonNull OpenAiChatService cs) {
		return new OpenAiQuestionAnsweringService(cs);
	}

	@Override
	public OpenAiImageGenerationService getImageGenerationService() {
		return new OpenAiImageGenerationService(this);
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
