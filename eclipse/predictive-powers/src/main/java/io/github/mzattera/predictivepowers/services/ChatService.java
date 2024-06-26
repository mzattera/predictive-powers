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

import java.util.List;

import io.github.mzattera.predictivepowers.services.messages.ChatCompletion;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage;

/**
 * This is a service capable of holding conversations with the user. It is more
 * advanced than {#link CompletionService}, as it can both complete a text
 * prompt and hold a conversation. It exposes method to maintain the
 * conversation history.
 * 
 * This service is not supposed to use any tool, just manage text messages back
 * and forth.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public interface ChatService extends AiService {

	/**
	 * Number of top tokens considered within the sample operation to create new
	 * text.
	 */
	Integer getTopK();

	/**
	 * Number of top tokens considered within the sample operation to create new
	 * text.
	 */
	void setTopK(Integer topK);

	/**
	 * Add tokens in the sample for more probable to least probable until the sum of
	 * the probabilities is greater than this.
	 */
	Double getTopP();

	/**
	 * Add tokens in the sample for more probable to least probable until the sum of
	 * the probabilities is greater than this.
	 */
	void setTopP(Double topP);

	/**
	 * The temperature (0-100) of the sampling operation. 1 means regular sampling,
	 * 0 means always take the highest score, 100.0 is getting closer to uniform
	 * probability.
	 */
	Double getTemperature();

	/**
	 * The temperature (0-100) of the sampling operation. 1 means regular sampling,
	 * 0 means always take the highest score, 100.0 is getting closer to uniform
	 * probability.
	 */
	void setTemperature(Double temperature);

	/**
	 * These are the messages exchanged in the current chat. Implementations of this
	 * interface are supposed to keep history updated by adding each user utterance
	 * and the corresponding agent reply.
	 * 
	 * Notice is this not expected to be manipulated; see {@link #setH}
	 * 
	 * Notice this will grow up to {@link #getMaxHistoryLength}, unless cleared.
	 * However, only latest messages are considered when calling the API (see
	 * {@link getMaxConversationLength}).
	 */
	List<? extends ChatMessage> getHistory();

	/**
	 * Maximum number of messages to keep in chat history.
	 */
	int getMaxHistoryLength();

	/**
	 * Maximum number of messages to keep in chat history.
	 */
	void setMaxHistoryLength(int maxHistoryLength);

	/** Personality of the bot holding the conversation. */
	String getPersonality();

	/** Personality of the bot holding the conversation. */
	void setPersonality(String personality);

	/**
	 * Maximum number of history messages to consider when interacting with chat
	 * service (counting last message too).
	 * 
	 * Notice this does NOT limit length of conversation history (see
	 * {@link #getMaxHistoryLength}).
	 */
	int getMaxConversationSteps();

	/**
	 * Maximum number of history messages to consider when interacting with chat
	 * service (counting last message too).
	 * 
	 * Notice this does NOT limit length of conversation history (see
	 * {@link #getMaxHistoryLength}).
	 */
	void setMaxConversationSteps(int l);

	/**
	 * Maximum number of tokens to keep in conversation when interacting with chat
	 * service (counting last message too).
	 * 
	 * For some models, the higher this parameter, the smaller the allowed size of
	 * the reply.
	 */
	int getMaxConversationTokens();

	/**
	 * Maximum number of tokens to keep in conversation when interacting with chat
	 * service (counting last message too).
	 * 
	 * For some models, the higher this parameter, the smaller the allowed size of
	 * the reply.
	 */
	void setMaxConversationTokens(int n);

	/**
	 * Maximum amount of tokens to produce as a reply.
	 */
	Integer getMaxNewTokens();

	/**
	 * Maximum amount of tokens to produce as a reply.
	 */
	void setMaxNewTokens(Integer maxNewTokens);

	/**
	 * This method returns number of tokens that are consumed at each request to
	 * provide directions (e.g. personality) to this service. This is the minimum
	 * token size of each request to the API, in addition to token consumed by
	 * messages.
	 */
	public int getBaseTokens();

	/**
	 * Starts a new chat, clearing current conversation.
	 */
	void clearConversation();

	/**
	 * Continues current chat, with the provided message.
	 * 
	 * The exchange is added to the conversation history.
	 */
	ChatCompletion chat(String msg);

	/**
	 * Continues current chat, with the provided message.
	 * 
	 * The exchange is added to the conversation history.
	 */
	ChatCompletion chat(ChatMessage msg);

	/**
	 * Completes text outside a conversation (executes given prompt ignoring and
	 * without affecting conversation history).
	 * 
	 * Notice this does not consider or affects chat history but bot personality is
	 * used, if provided.
	 */
	ChatCompletion complete(String prompt);

	/**
	 * Completes text outside a conversation (executes given prompt ignoring and
	 * without affecting conversation history).
	 * 
	 * Notice this does not consider or affects chat history but bot personality is
	 * used, if provided.
	 */
	ChatCompletion complete(ChatMessage prompt);
}