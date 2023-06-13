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
package io.github.mzattera.predictivepowers.services;

import java.util.ArrayList;
import java.util.List;

import io.github.mzattera.predictivepowers.TokenCounter;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsChoice;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsResponse;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.util.ModelUtil;
import io.github.mzattera.predictivepowers.services.ChatMessage;
import io.github.mzattera.predictivepowers.services.Service;
import io.github.mzattera.predictivepowers.services.TextResponse;
import io.github.mzattera.predictivepowers.services.ChatMessage.Role;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Abstract embedding service that can be sub-classed to create other services
 * faster (hopefully).
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public abstract class AbstractChatService implements ChatService {

	// TODO add "slot filling" capabilities: fill a slot in the prompt based on
	// values from a Map

	@Getter
	protected final List<ChatMessage> history = new ArrayList<>();

	@Getter
	@Setter
	private int maxHistoryLength = 100;

	@Getter
	@Setter
	private String personality = null;

	@Getter
	private int maxConversationSteps = 14;

	public void setMaxConversationSteps(int l) {
		if (l < 1)
			throw new IllegalArgumentException("Must keep at least 1 message.");
		maxConversationSteps = l;
	}

	@Getter
	private int maxConversationTokens;

	public void setMaxConversationTokens(int n) {
		if (n < 1)
			throw new IllegalArgumentException("Must keep at least 1 token.");
		maxConversationTokens = n;
	}

	/**
	 * Starts a new chat, clearing current conversation.
	 */
	public void clearConversation() {
		history.clear();
	}

	/**
	 * Trims given conversation history, so it fits the limits set in this instance.
	 * 
	 * @param counter A {@link TokenCounter} used to count tokens for trimming the chat history.
	 * 
	 * @return A new conversation, including agent personality and as many messages
	 *         as can fit, given current settings.
	 */
	protected List<ChatMessage> trimChat(List<ChatMessage> messages, TokenCounter counter) {
		List<ChatMessage> result = new ArrayList<>(messages.size());
	
		int numTokens = 3; // these are in the chat response, always
		if (getPersonality() != null) {
			ChatMessage m = new ChatMessage(ChatMessage.Role.SYSTEM, getPersonality());
			result.add(m);
			numTokens = counter.count(m);
		}
	
		int numMsg = 0;
		for (int i = messages.size() - 1; i >= 0; --i) {
			if (numMsg >= maxConversationSteps)
				break;
	
			ChatMessage msg = messages.get(i);
			int t = counter.count(msg);
			if ((numMsg > 0) && ((numTokens + t) > maxConversationTokens))
				break;
	
			if (getPersonality() != null) {
				// Skip ChatMessage.Role.SYSTEM message when inserting
				result.add(1, msg);
			} else {
				result.add(0, msg);
			}
			++numMsg;
			numTokens += t;
		}
	
		return result;
	}
}