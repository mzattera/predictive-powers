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
package io.github.mzattera.predictivepowers.services;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.github.mzattera.predictivepowers.EndpointException;
import io.github.mzattera.predictivepowers.services.messages.ChatCompletion;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage.Author;
import io.github.mzattera.predictivepowers.services.messages.JsonSchema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Abstract {@link ChatService} that can be sub-classed to create other services
 * faster (hopefully).
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@RequiredArgsConstructor
public abstract class AbstractChatService implements ChatService {

	// TODO add "slot filling" capabilities: fill a slot in the prompt based on
	// values from a Map, see CompletionService

	@Getter
	@Setter
	private String personality = null;

	@Getter
	private int maxHistoryLength = Integer.MAX_VALUE;

	@Override
	public void setMaxHistoryLength(int l) {
		if (l < 0)
			throw new IllegalArgumentException();
		maxHistoryLength = l;
	}

	@Getter
	private int maxConversationSteps = Integer.MAX_VALUE;

	@Override
	public void setMaxConversationSteps(int l) {
		if (l < 1)
			throw new IllegalArgumentException("Must keep at least 1 message.");
		maxConversationSteps = l;
	}

	@Getter
	private int maxConversationTokens = Integer.MAX_VALUE;

	@Override
	public void setMaxConversationTokens(int n) {
		if (n < 1)
			throw new IllegalArgumentException("Must keep at least 1 token.");
		maxConversationTokens = n;
	}

	@Getter
	private Integer maxNewTokens;

	@Override
	public void setMaxNewTokens(Integer maxNewTokens) {
		if ((maxNewTokens != null) && (maxNewTokens.intValue() < 1))
			throw new IllegalArgumentException("Must generate at least 1 new token.");
	}

	@Override
	public void setResponseFormat(Class<?> schema) {
		setResponseFormat(JsonSchema.fromSchema(schema));
	}

	@Override
	public void setResponseFormat(String schema) throws JsonProcessingException {
		setResponseFormat(JsonSchema.fromSchema(schema));
	}

	@Override
	public ChatCompletion chat(String msg) throws EndpointException {
		return chat(new ChatMessage(Author.USER, msg));
	}

	@Override
	public ChatCompletion complete(String prompt) throws EndpointException {
		return complete(new ChatMessage(Author.USER, prompt));
	}

	@Override
	public void close() {
	}
}