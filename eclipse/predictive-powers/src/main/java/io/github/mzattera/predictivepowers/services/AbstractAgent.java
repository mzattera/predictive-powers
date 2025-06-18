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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mzattera.predictivepowers.services.Capability.ToolAddedEvent;
import io.github.mzattera.predictivepowers.services.Capability.ToolRemovedEvent;
import lombok.NonNull;

/**
 * This is an abstract implementation of an {@link Agent} meant to be
 * sub-classed for easier implementation of agents.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public abstract class AbstractAgent extends AbstractChatService implements Agent {

	private final static Logger LOG = LoggerFactory.getLogger(AbstractAgent.class);

	protected AbstractAgent() {
	}

	/**
	 * Available and already initialized capabilities. Maps each capability ID into
	 * corresponding capability instance.
	 */
	protected Map<String, Capability> capabilityMap = new HashMap<>();

	/**
	 * Available and already initialized tools, across all capabilities. Maps each
	 * tool ID into corresponding tool instance.
	 */
	protected Map<String, Tool> toolMap = new HashMap<>();

	@Override
	public List<String> getCapabilityIDs() {
		return List.copyOf(capabilityMap.keySet());
	}

	@Override
	public Capability getCapability(@NonNull String capabilityId) {
		return capabilityMap.get(capabilityId);
	}

	@Override
	public void addCapability(@NonNull Capability capability) throws ToolInitializationException {

		// Dispose any existing version of the capability
		Capability old = capabilityMap.get(capability.getId());
		if (old != null)
			removeCapability(old);

		// Register as a listener, so tools in the capability will be added
		// automatically
		capability.addListener(this);
		capability.init(this);
		capabilityMap.put(capability.getId(), capability);
	}

	@Override
	public void removeCapability(@NonNull String capabilityId) {
		Capability capability = capabilityMap.get(capabilityId);
		if (capability != null)
			removeCapability(capability);
	}

	@Override
	public void removeCapability(@NonNull Capability capability) {

		try {
			capability.close();
		} catch (Exception e) {
			LOG.warn("Error closing capability", e.getMessage());
		}
		capability.removeListener(this);
		capabilityMap.remove(capability.getId());
	}

	/**
	 * Removes all capabilities from the agent. The capability and its tools are
	 * disposed and removed from the agent.
	 */
	@Override
	public void clearCapabilities() {
		Iterator<String> it = capabilityMap.keySet().iterator();
		while(it.hasNext()) {
			removeCapability(it.next());			
		}
	}

	/**
	 * This is called when a new tool is made available. It adds the tool to the
	 * tool map.
	 */
	@Override
	public void onToolAdded(@NonNull ToolAddedEvent evt) {
		Tool tool = evt.getTool();
		toolMap.put(tool.getId(), tool);
		LOG.debug("Added tool " + evt.getTool().getId());
	}

	/**
	 * This is called when a tool is removed. It removes the tool from the tool map.
	 */
	@Override
	public void onToolRemoved(@NonNull ToolRemovedEvent evt) {
		toolMap.remove(evt.getTool().getId());
		LOG.debug("Removed tool " + evt.getTool().getId());
	}

	@Override
	public void close() {
		try {
			clearCapabilities();
		} catch (Exception e) {
			// Paranoid
			LOG.warn("Error while disposing capabiliites", e);
		}
	}
}