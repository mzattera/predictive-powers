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

import java.util.List;

/**
 * Instances of this interface manage a chats with an agent.
 * 
 * Chat history is kept in memory and updated as chat progresses. At the same
 * time, methods to use chat service as a simple completion service are
 * provided.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public interface ChatService extends Service {

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
//
//	/**
//	 * Maximum amount of tokens to produce (not including the prompt).
//	 */
//	Integer getMaxNewTokens();
//
//	/**
//	 * Maximum amount of tokens to produce (not including the prompt).
//	 */
//	void setMaxNewTokens(Integer maxNewTokens);
	
	/**
	 * These are the messages exchanged in the current chat.
	 * 
	 * They can be manipulated in order to alter chat flow.
	 * 
	 * Notice this will grow up to {@link #maxHistoryLength}, unless cleared.
	 * However, only latest messages are considered when calling the API (see
	 * {@link maxConversationLength}).
	 */
	List<ChatMessage> getHistory();

	/** Maximum number of steps to keep in chat history. */
	int getMaxHistoryLength();

	/** Maximum number of steps to keep in chat history. */
	void setMaxHistoryLength(int maxHistoryLength);

	/** Personality of the agent. If null, agent has NO personality. */
	String getPersonality();

	/** Personality of the agent. If null, agent has NO personality. */
	void setPersonality(String personality);

	/**
	 * Maximum number of steps in the conversation to consider when interacting with
	 * chat service.
	 * 
	 * Notice this does NOT limit length of conversation history (see
	 * {@link #maxHistoryLength}).
	 */
	int getMaxConversationSteps();

	/**
	 * Maximum number of steps in the conversation to consider when interacting with
	 * chat service.
	 * 
	 * Notice this does NOT limit length of conversation history (see
	 * {@link #maxHistoryLength}).
	 */
	void setMaxConversationSteps(int l);

	/**
	 * Maximum number of tokens to keep in conversation steps when interacting with
	 * chat service.
	 * 
	 * The higher this parameter, the smaller the allowed size of the reply.
	 * 
	 * As there is no Java code available for an exact calculation, this is
	 * approximated.
	 */
	int getMaxConversationTokens();

	/**
	 * Maximum number of tokens to keep in conversation steps when interacting with
	 * chat service.
	 * 
	 * The higher this parameter, the smaller the allowed size of the reply.
	 * 
	 * As there is no Java code available for an exact calculation, this is
	 * approximated.
	 */
	void setMaxConversationTokens(int n);

	/**
	 * Starts a new chat, clearing current conversation.
	 */
	void clearConversation();

	/**
	 * Continues current chat, with the provided message.
	 * 
	 * The exchange is added to the conversation history.
	 */
	TextResponse chat(String msg);

	/**
	 * Completes text (executes given prompt).
	 * 
	 * Notice this does not consider or affects chat history but agent personality
	 * is used, if provided.
	 */
	TextResponse complete(String prompt);

	/**
	 * Completes given conversation, using this service as a completion service.
	 * 
	 * Notice this does not consider or affects chat history. In addition, agent
	 * personality is NOT considered, but can be injected as first message in the
	 * list, for service that support this.
	 */
	TextResponse complete(List<ChatMessage> messages);

}