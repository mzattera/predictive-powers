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

package io.github.mzattera.predictivepowers;

import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.embeddings.EmbeddingsRequest;
import io.github.mzattera.predictivepowers.services.ChatService;
import io.github.mzattera.predictivepowers.services.CompletionService;
import io.github.mzattera.predictivepowers.services.EmbeddingService;
import io.github.mzattera.predictivepowers.services.QuestionAnsweringService;
import io.github.mzattera.predictivepowers.services.QuestionExtractionService;
import lombok.Getter;
import lombok.NonNull;

/**
 * This represents an OpenAI end point, from which APIs can be created.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
public class OpenAiEndpoint {

	// TODO add endpoint for Azure OpenAi Services

	@Getter
	private final OpenAiClient client;

	private OpenAiEndpoint(@NonNull String apiKey) {
		this(new OpenAiClient(apiKey));
	}

	private OpenAiEndpoint(@NonNull OpenAiClient client) {
		this.client = client;
	}

	// TODO add configuration of API client (e.g. timeout)

	/**
	 * Create a new end point.
	 * 
	 * @return An end point instance, reading API key from "OPENAI_API_KEY"
	 *         environment parameter.
	 */
	public static OpenAiEndpoint getInstance() {
		return new OpenAiEndpoint(System.getenv("OPENAI_API_KEY"));
	}

	/**
	 * Create a new end point.
	 * 
	 * @return An end point instance that uses given API key.
	 */
	public static OpenAiEndpoint getInstance(@NonNull String apiKey) {
		return new OpenAiEndpoint(apiKey);
	}

	/**
	 * Create a new end point.
	 * 
	 * @return An end point instance that uses given API client.
	 */
	public static OpenAiEndpoint getInstance(@NonNull OpenAiClient client) {
		return new OpenAiEndpoint(client);
	}

	public CompletionService getCompletionService() {
		return new CompletionService(this);
	}

	public CompletionService getCompletionService(CompletionsRequest defaultReq) {
		return new CompletionService(this, defaultReq);
	}

	public EmbeddingService getEmbeddingService() {
		return new EmbeddingService(this);
	}

	public EmbeddingService getEmbeddingService(@NonNull EmbeddingsRequest defaultReq) {
		return new EmbeddingService(this, defaultReq);
	}

	public ChatService getChatService() {
		return new ChatService(this);
	}

	public ChatService getChatService(String personality) {
		ChatService s = getChatService();
		s.setPersonality(personality);
		return s;
	}

	public ChatService getChatService(ChatCompletionsRequest defaultReq) {
		return new ChatService(this, defaultReq);
	}

	public ChatService getChatService(ChatCompletionsRequest defaultReq, String personality) {
		ChatService s = getChatService(defaultReq);
		s.setPersonality(personality);
		return s;
	}

	public QuestionExtractionService getQuestionExtractionService() {
		return new QuestionExtractionService(this);
	}

	public QuestionExtractionService getQuestionExtractionService(@NonNull ChatService cs) {
		return new QuestionExtractionService(this, cs);
	}

	public QuestionAnsweringService getQuestionAnsweringService() {
		return new QuestionAnsweringService(this);
	}

	public QuestionAnsweringService getQuestionAnsweringService(@NonNull ChatService cs) {
		return new QuestionAnsweringService(this, cs);
	}
}
