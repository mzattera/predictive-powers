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

import io.github.mzattera.predictivepowers.services.ChatMessage.Author;
import io.github.mzattera.predictivepowers.services.ModelService.Tokenizer;
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
	protected final ModelService modelService;

	@Getter
	protected final List<ChatMessage> history = new ArrayList<>();

	@Getter
	@Setter
	private int maxHistoryLength = 1000;

	@Getter
	@Setter
	private String personality = null;

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

	@Override
	public ChatCompletion chat(String msg) {
		return chat(new ChatMessage(Author.USER, msg));
	}

	@Override
	public ChatCompletion complete(String prompt) {
		return complete(new ChatMessage(Author.USER, prompt));
	}

	/**
	 * Trims given list of messages (typically a conversation history), so it fits
	 * the limits set in this instance (that is, {@link #maxConversationSteps} and
	 * {@link #maxConversationTokens}).
	 * 
	 * @param addPersonality If true, personality will be added as a system message
	 *                       at the beginning of the resulting conversation.
	 * 
	 * @return A new conversation with as many messages as can fit, given this
	 *         instance settings.
	 */
	protected List<ChatMessage> trimConversation(List<ChatMessage> messages) {
		return trimConversation(messages, maxConversationSteps, maxConversationTokens);
	}

	/**
	 * Trims given list of messages (typically a conversation history), so it fits
	 * the given limits set in this instance.
	 * 
	 * @return A new conversation with as many messages as can fit, based on given
	 *         limits.
	 */
	protected List<ChatMessage> trimConversation(List<ChatMessage> messages, int maxConversationSteps,
			int maxConversationTokens) {

		List<ChatMessage> result = new ArrayList<>(messages.size());
		Tokenizer counter = modelService.getTokenizer(getModel());
		int steps = 0;

		for (int i = messages.size() - 1; i >= 0; --i) {
			if (steps >= maxConversationSteps)
				break;

			result.add(0, messages.get(i));

			if (counter.count(result) > maxConversationTokens) {
				// remove last msg, it exceeded number of tokens, then exit
				result.remove(result.size() - 1);
				break;
			}

			++steps;
		}

		return result;
	}
}