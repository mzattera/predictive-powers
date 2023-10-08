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

import io.github.mzattera.predictivepowers.services.ChatMessage.Role;
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
	// values from a Map

	@Getter
	protected final ModelService modelService;

	@Getter
	protected final List<ChatMessage> history = new ArrayList<>();

	@Getter
	@Setter
	private int maxHistoryLength = 30;

	@Getter
	@Setter
	private String personality = null;

	@Getter
	private int maxConversationSteps = 15;

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
	public TextCompletion chat(String msg) {
		return chat(new ChatMessage(Role.USER, msg));
	}

	@Override
	public TextCompletion complete(String prompt) {
		return complete(new ChatMessage(Role.USER, prompt));
	}

	/**
	 * Trims given list of messages (typically a conversation history), so it fits
	 * the limits set in this instance (that is, maximum conversation steps and
	 * tokens).
	 * 
	 * @param addPersonality If true, personality will be added as a system message
	 *                       at the beginning of the resulting conversation.
	 * 
	 * @return A new conversation, including agent personality and as many messages
	 *         as can fit, given current settings.
	 */
	protected List<ChatMessage> trimChat(List<ChatMessage> messages, boolean addPersonality) {
		List<ChatMessage> result = new ArrayList<>(messages.size());
		Tokenizer counter = modelService.getTokenizer(getModel());

		boolean personalityAdded = false;
		if (addPersonality && (getPersonality() != null)) {
			result.add(new ChatMessage(ChatMessage.Role.SYSTEM, getPersonality()));
			personalityAdded = true;
		}

		int steps = 0; // Only history counts against steps.

		for (int i = messages.size() - 1; i >= 0; --i) {
			if (steps >= maxConversationSteps)
				break;

			ChatMessage msg = messages.get(i);
			if (personalityAdded) {
				// Insert after ChatMessage.Role.SYSTEM at the beginning of conversation
				result.add(1, msg);
			} else {
				result.add(0, msg);
			}

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