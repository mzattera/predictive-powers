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

import java.util.Collection;
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
 * @author Massimiliano "Maxi" Zattera
 *
 */
public interface Agent extends AiService {

	// TODO URGENT: Add RemoteFile class and add files to agent -> fro OpenAI only
	// TODO URGENT: Add methods to handle conversations (threads e.g. creating,
	// continuing, deleting)
	// TODO URGENT: OpenAI agent has the tools on its side, but they need to be
	// bound again to tools on the client side. Describe this situation.
	// Optionally we could always use an external Map, also for chat service, but it
	// is less convenient....

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
	 * Sets the {@link ToolProvider} used by this agent to retrieve its tools, when
	 * it needs to invoke them.
	 */
	void setToolProvider(ToolProvider provider);

	/**
	 * Get tools available to the agent.
	 * 
	 * Notice this is expected to be an unmodifiable list; use other methods to
	 * populate tools list properly.
	 */
	List<String> getTools();

	/**
	 * Set the list of tools available to the agent.
	 * 
	 * @throws ToolInitializationException if an error happens while initializing
	 *                                     any of the tools.
	 */
	void setTools(@NonNull Collection<String> toolIds) throws ToolInitializationException;

	/**
	 * Add one tool to the list of tools available to the agent.
	 * 
	 * @throws ToolInitializationException if an error happens while initializing
	 *                                     the tool.
	 */
	void addTool(@NonNull String toolId) throws ToolInitializationException;

	/**
	 * Add given tools to the list of tools available to the agent.
	 * 
	 * @throws ToolInitializationException if an error happens while initializing
	 *                                     any of the tools.
	 */
	void addTools(@NonNull Collection<String> toolId) throws ToolInitializationException;

	/**
	 * Remove one tool from list of tools available to the agent.
	 * 
	 * @param id The unique ID for the tool.
	 */
	void removeTool(@NonNull String toolId);

	/**
	 * Remove all tools available to the agent.
	 */
	void clearTools();

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
}