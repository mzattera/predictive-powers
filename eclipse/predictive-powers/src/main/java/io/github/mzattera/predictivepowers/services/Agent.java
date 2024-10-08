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
import lombok.NonNull;

/**
 * This represents an agent (assistant) handled by an {@link AgentService} which
 * is able to hold a conversation with the user.
 * 
 * It is more advanced than {#link ChatService}, as it can invoke {@link Tool}s
 * to complete its tasks and use files in chat messages.
 * 
 * At the moment, the interface does not expose methods to retrieve and manage
 * stored conversations.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public interface Agent extends AutoCloseable {

	/**
	 * Get unique agent ID. Notice this ID is unique only inside one endpoint.
	 */
	String getId();

	/**
	 * The ID of the model used by the agent.
	 */
	String getModel();

	/**
	 * The display name of the assistant.
	 */
	String getName();

	/**
	 * The display name of the assistant.
	 */
	void setName(String name);

	/**
	 * The description of the assistant.
	 */
	String getDescription();

	/**
	 * The description of the assistant.
	 */
	void setDescription(String description);

	/**
	 * The personality for the agent (system instructions).
	 */
	String getPersonality();

	/**
	 * The personality for the agent (system instructions).
	 */
	void setPersonality(String personality);

	/**
	 * Get capabilities available to the agent.
	 * 
	 * Notice this is expected to be an unmodifiable list; use other methods to
	 * populate tools list properly.
	 */
	List<String> getCapabilities();

	/**
	 * Add one capability to the list of capabilities available to the agent.
	 * 
	 * @throws ToolInitializationException if an error happens while initializing
	 *                                     the capability.
	 */
	void addCapability(@NonNull Capability capability) throws ToolInitializationException;

	/**
	 * Remove one capability from list of capabilities available to the agent.
	 * 
	 * @param id The unique ID for the capability.
	 */
	void removeCapability(@NonNull String capabilityId);

	/**
	 * Remove all capabilities available to the agent.
	 */
	void clearCapabilities();

	// We do not add methods to trim history length, as we suppose the agent
	// maintains it autonomously

	/**
	 * These are the messages exchanged in the current conversation.
	 * 
	 * Notice is this not expected to be manipulated.
	 */
	List<? extends ChatMessage> getHistory();

	/**
	 * Starts a new chat, clearing current conversation.
	 */
	void clearConversation();

	/**
	 * Continues current chat, with the provided message.
	 */
	ChatCompletion chat(String message);

	/**
	 * Continues current chat, with the provided message.
	 */
	ChatCompletion chat(ChatMessage message);
}