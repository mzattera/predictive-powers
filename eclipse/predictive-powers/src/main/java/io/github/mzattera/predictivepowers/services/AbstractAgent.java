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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mzattera.predictivepowers.services.messages.ChatCompletion;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage;
import lombok.NonNull;

/**
 * This is an abstract implementation of an {@link Agent} meant to be
 * sub-classed for easier implmentation of agents.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public abstract class AbstractAgent implements Agent {

	private final static Logger LOG = LoggerFactory.getLogger(AbstractAgent.class);

	protected AbstractAgent() {
	}

	/**
	 * Available and already initialized tools. Maps each tool ID into corresponding
	 * tool instance.
	 */
	protected Map<String, Tool> toolMap = new HashMap<>();

	/**
	 * Available and already initialized capabilities. Maps each capability ID into
	 * corresponding capability instance.
	 */
	protected Map<String, Capability> capabilityMap = new HashMap<>();

	@Override
	public List<String> getCapabilities() {
		return Collections.unmodifiableList(new ArrayList<>(capabilityMap.keySet()));
	}

	/**
	 * Adds a capability. This method automatically closes any previous instance of
	 * a capability with same ID, removing and disposing its tools from the agent as
	 * well. It then initializes the new capability.
	 */
	@Override
	public void addCapability(@NonNull Capability capability) throws ToolInitializationException {

		// Dispose any existing version of the capability
		Capability old = capabilityMap.get(capability.getId());
		if (old != null)
			removeCapability(old.getId());

		capability.init(this);

		for (String toolId : capability.getToolIds()) {
			putTool(capability.getNewToolInstance(toolId));
		}

		capabilityMap.put(capability.getId(), capability);
	}

	/**
	 * Removes a capability. The capability and its tools are disposed and removed
	 * from the agent.
	 */
	@Override
	public void removeCapability(@NonNull String capabilityId) {
		removeCapability(capabilityMap.get(capabilityId));
	}

	private void removeCapability(Capability capability) {
		if (capability == null)
			return;

		for (String toolId : capability.getToolIds())
			removeTool(toolId);
		try {
			capability.close();
		} catch (Exception e) {
			LOG.warn("Error closing capability", e.getMessage());
		}

		capabilityMap.remove(capability.getId());
	}

	/**
	 * Removes all capabilities from the agent. The capability and its tools are
	 * disposed and removed from the agent.
	 */
	@Override
	public void clearCapabilities() {
		for (Capability capability : capabilityMap.values())
			removeCapability(capability);
		capabilityMap.clear();
	}

	/**
	 * This is called when a tool must be added to the agent. Default implementation
	 * disposes any existing instance of the tool, then initializes the new one and
	 * adds it to the tool list.
	 */
	protected void putTool(@NonNull Tool tool) throws ToolInitializationException {

		// Tries if init goes
		tool.init(this);

		// Closes older version of this tool, if any
		removeTool(tool.getId());

		toolMap.put(tool.getId(), tool);
	}

	/**
	 * This is called when a tool must be removed from the agent. Default
	 * implementation disposes the tool after removing it.
	 * 
	 * @return true if the tool has been removed (it existed in the list of agent's
	 *         tools).
	 */
	protected boolean removeTool(@NonNull String toolId) {
		Tool old = toolMap.remove(toolId);
		if (old != null) {
			try {
				old.close();
			} catch (Exception e) {
				LOG.warn("Error closing tool", e.getMessage());
			}
			return true;
		}

		return false;
	}

	@Override
	public ChatCompletion chat(String msg) {
		return chat(new ChatMessage(msg));
	}

	@Override
	public void close() {
	}
}