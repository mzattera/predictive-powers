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

import lombok.NonNull;

/**
 * This represents an agent (assistant) handled by an {@link AgentService} which
 * is able to hold a conversation with the user.
 * 
 * It is more advanced than {#link ChatService}, as it can invoke {@link Tool}s
 * that an agent can invoke to complete its tasks and use files in chat messages
 * (see {@link AgentMessage}).
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public interface Agent extends ChatService {

	// TODO URGENT: Add RemoteFile class and add files to agent
	// TODO URGENT: Add methods to handle conversations (threads e.g. creating, continuing, deleting)

	/**
	 * Get unique agent ID.
	 * Notice this ID is unique only inside one endpoint.
	 */
	String getId();

	/**
	 * Get user for which this agent was created, or null if the agent is not bound to a user.
	 * Notice this ID is unique only inside one endpoint.
	 */
	String getUserId();

	/**
	 * Get tools available to the agent.
	 * 
	 * Notice this is an unmodifiable list; use other methods to populate tools list
	 * properly.
	 */
	List<? extends Tool> getTools();

	/**
	 * Set the list of tools available to the agent to the given one. Notice a tool
	 * must be initialized calling its {@link Tool#init(AgentService)} method before
	 * the agent can invoke it.
	 * 
	 * @throws ToolInitializationException if an error happens while initializing
	 *                                     any of the tools.
	 */
	void setTools(@NonNull Collection<? extends Tool> tools) throws ToolInitializationException;

	/**
	 * Add one tool to the list of tools available to the agent. Notice a tool must
	 * be initialized calling its {@link Tool#init(AgentService)} method before the
	 * agent can invoke it.
	 * 
	 * 
	 * @throws ToolInitializationException if an error happens while initializing
	 *                                     the tool.
	 */
	void addTool(@NonNull Tool tool) throws ToolInitializationException;

	/**
	 * Add given tools to the list of tools available to the agent. Notice a tool
	 * must be initialized calling its {@link Tool#init(AgentService)} method before
	 * the agent can invoke it.
	 * 
	 * 
	 * @throws ToolInitializationException if an error happens while initializing
	 *                                     any of the tools.
	 */
	void addTools(@NonNull Collection<? extends Tool> tools) throws ToolInitializationException;

	/**
	 * Remove one tool from list of tools available to the agent.
	 * 
	 * @param id The unique ID for the tool.
	 * @return Removed tool, or null if no such tool existed..
	 */
	Tool removeTool(@NonNull String id);

	/**
	 * Remove one tool from list of tools available to the agent.
	 * 
	 * @return Removed tool, or null if no such tool existed..
	 */
	Tool removeTool(@NonNull Tool tool);

	/**
	 * Remove all tools available to the agent.
	 */
	void clearTools();
}