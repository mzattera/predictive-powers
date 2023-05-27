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

import java.io.Closeable;

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
public class OpenAiEndpoint implements Closeable {

	// TODO add client/endpoint for Azure OpenAi Services

	@Getter
	private final OpenAiClient client;

	/**
	 * Constructor. OpenAiApi key is read from OPENAI_API_KEY system environment
	 * variable.
	 */
	public OpenAiEndpoint() {
		this(new OpenAiClient(null, -1, -1, -1));
	}

	public OpenAiEndpoint(String apiKey) {
		this(new OpenAiClient(apiKey, -1, -1, -1));
	}

	public OpenAiEndpoint(@NonNull OpenAiClient client) {
		this.client = client;
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

	@Override
	public void close() {
		try {
			client.close();
		} catch (Exception e) {
			// TODO log
		}
	}
}
