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

import lombok.NonNull;

/**
 * This represents an agent (sometime assistant), possibly handled by an
 * {@link AgentService} which is able to hold a conversation with the user.
 * 
 * It is more advanced than {#link ChatService}, as it can invoke {@link Tool}s.
 * 
 * At the moment, the interface does not expose methods to retrieve and manage
 * stored conversations.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public interface Agent extends ChatService, Capability.Listener {

	/**
	 * Get unique agent ID. Notice this ID is unique only inside one endpoint.
	 */
	String getId();

	/**
	 * The display name of the agent.
	 */
	String getName();

	/**
	 * The display name of the agent.
	 */
	void setName(String name);

	/**
	 * The description of the agent.
	 */
	String getDescription();

	/**
	 * The description of the agent.
	 */
	void setDescription(String description);

	/**
	 * Get capabilities available to the agent.
	 * 
	 * @return IDs of capabilities available to the agent.
	 */
	List<String> getCapabilityIDs();

	/**
	 * Get one capabilities available to the agent.
	 * 
	 * @param capabilityId
	 * @return Capability with given ID or null if it cannot be found.
	 */
	Capability getCapability(@NonNull String capabilityId);

	/**
	 * Adds a capability. This method automatically closes any previous instance of
	 * a capability with same ID, removing and disposing its tools from the agent as
	 * well. It then initializes the new capability.
	 * 
	 * @throws ToolInitializationException If an error happens while initializing
	 *                                     the capability.
	 */
	void addCapability(@NonNull Capability capability) throws ToolInitializationException;

	/**
	 * Removes a capability. The capability and its tools are disposed and removed
	 * from the agent.
	 * 
	 * @param id The unique ID for the capability.
	 */
	void removeCapability(@NonNull String capabilityId);

	/**
	 * Removes a capability. The capability and its tools are disposed and removed
	 * from the agent.
	 */
	void removeCapability(@NonNull Capability capabilityId);

	/**
	 * Remove all capabilities available to the agent.
	 */
	void clearCapabilities();
}