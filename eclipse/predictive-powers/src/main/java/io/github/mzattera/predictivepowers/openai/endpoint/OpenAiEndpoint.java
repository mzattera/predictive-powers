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

	@Override
	public OpenAiCompletionService getCompletionService(@NonNull String model) {

		OpenAiCompletionService svc = getCompletionService();
		svc.setModel(model);
		return svc;
	}

	public OpenAiCompletionService getCompletionService(CompletionsRequest defaultReq) {
		return new OpenAiCompletionService(this, defaultReq);
	}

	@Override
	public OpenAiEmbeddingService getEmbeddingService() {
		return new OpenAiEmbeddingService(this);
	}

	@Override
	public OpenAiEmbeddingService getEmbeddingService(@NonNull String model) {

		OpenAiEmbeddingService svc = getEmbeddingService();
		svc.setModel(model);
		return svc;
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

	@Override
	public OpenAiChatService getChatService(@NonNull String model, String personality) {

		OpenAiChatService svc = getChatService(personality);
		svc.setModel(model);
		return svc;
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
	public OpenAiQuestionExtractionService getQuestionExtractionService() {
		return new OpenAiQuestionExtractionService(this);
	}

	@Override
	public OpenAiQuestionExtractionService getQuestionExtractionService(@NonNull String model) {
		OpenAiQuestionExtractionService svc = getQuestionExtractionService();
		svc.setModel(model);
		return svc;
	}

	@Override
	public OpenAiQuestionAnsweringService getQuestionAnsweringService() {
		return new OpenAiQuestionAnsweringService(this);
	}

	@Override
	public OpenAiQuestionAnsweringService getQuestionAnsweringService(@NonNull String model) {
		OpenAiQuestionAnsweringService svc = getQuestionAnsweringService();
		svc.setModel(model);
		return svc;
	}

	@Override
	public OpenAiImageGenerationService getImageGenerationService() {
		return new OpenAiImageGenerationService(this);
	}

	@Override
	public OpenAiImageGenerationService getImageGenerationService(@NonNull String model) {
		OpenAiImageGenerationService svc = getImageGenerationService();
		svc.setModel(model);
		return svc;
	}

	@Override
	public OpenAiAgentService getAgentService() {
		return new OpenAiAgentService(this);
	}

	@Override
	public OpenAiAgentService getAgentService(@NonNull String model) {

		OpenAiAgentService svc = getAgentService();
		svc.setModel(model);
		return svc;
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
