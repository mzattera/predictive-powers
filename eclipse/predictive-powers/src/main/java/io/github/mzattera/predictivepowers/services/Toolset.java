/*
 * Copyright 2024-2025 Massimiliano "Maxi" Zattera
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

/**
 * 
 */
package io.github.mzattera.predictivepowers.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Basic implementation of a {@link Capability}. This can be subclassed to
 * implement other capabilities.
 * 
 * This is a collection of unrelated tools, backed by a Map.
 */
public class Toolset implements Capability {

	// TODO Add tests to test adding/removing tools with cancellation

	private final static Logger LOG = LoggerFactory.getLogger(Toolset.class);

	/** ID for default toolset */
	public static final String DEFAULT_ID = "$tools";

	@Getter
	@NonNull
	private final String id;

	@Getter
	@Setter
	@NonNull
	private String description;

	private Map<String, Tool> tools = new HashMap<>();

	/**
	 * If this has been initialized, this is the {@link Agent} containing it.
	 */
	@Getter(AccessLevel.PROTECTED)
	private Agent agent;

	/**
	 * Listeners for this capability.
	 */
	@Getter(AccessLevel.PROTECTED)
	private List<Listener> listeners = new ArrayList<>();

	/**
	 * Creates an empty capability with default ID.
	 */
	public Toolset() {
		this(DEFAULT_ID, null, new ArrayList<>());
	}

	/**
	 * Creates an empty capability.
	 */
	protected Toolset(@NonNull String id) {
		this(id, null, new ArrayList<>());
	}

	/**
	 * Creates a capability with default ID containing all of the given tools.
	 */
	public Toolset(@NonNull Collection<? extends Tool> tools) {
		this(DEFAULT_ID, null, tools);
	}

	/**
	 * Creates a capability containing all of the given tools.
	 */
	public Toolset(@NonNull String id, @NonNull Collection<? extends Tool> tools) {
		this(id, null, tools);
	}

	/**
	 * Creates a capability containing all of the given tools.
	 */
	public Toolset(@NonNull String id, String description, @NonNull Collection<? extends Tool> tools) {

		this.id = id;
		this.description = (description == null ? "Capability: " + id : description);
		this.tools = new HashMap<>();
		tools.forEach(t -> this.tools.put(t.getId(), t));
	}

	@Override
	public List<Tool> getTools() {
		return Collections.unmodifiableList(new ArrayList<>(tools.values()));
	}

	@Override
	public List<String> getToolIds() {
		return List.copyOf(tools.keySet());
	}

	@Override
	public Tool getTool(@NonNull String toolId) {
		return tools.get(toolId);
	}

	@Override
	public void putTool(@NonNull Tool tool) throws ToolInitializationException {

		// In case we need to dispose a previous instance of the tool
		if (tools.containsKey(tool.getId()))
			removeTool(tool);

		if (getAgent() != null) {
			// Capability is already attached to an agent, the tool must be initialized now
			tool.init(getAgent());
		}

		// Notify listeners of new tool availability; notice they can complain
		ToolAddedEvent evt = new ToolAddedEvent(tool);
		for (Listener l : listeners) {
			l.onToolAdded(evt);
			if (evt.isCancelled()) // Something went wrong
				throw new ToolInitializationException(evt.getCancelReason());
		}

		// If all went well, add the tool
		tools.put(tool.getId(), tool);
	}

	@Override
	public void removeTool(@NonNull String toolId) {
		Tool tool = tools.get(toolId);
		if (tool != null)
			removeTool(tool);
	}

	@Override
	public void removeTool(@NonNull Tool tool) {

		if(tools.remove(tool.getId()) == null)
				return;
		
		ToolRemovedEvent evt = new ToolRemovedEvent(tool);
		listeners.forEach(l -> l.onToolRemoved(evt));

		// Dispose the tool
		try {
			tool.close();
		} catch (Exception e) {
			LOG.warn("Error closing tool " + tool.getId(), e);
		}
	}

	@Override
	public void clear() {
		getTools().forEach(t -> removeTool(t));
	}

	@Override
	public boolean isInitialized() {
		return (agent != null);
	}

	@Override
	public void init(@NonNull Agent agent) throws ToolInitializationException {
		if (isInitialized())
			throw new ToolInitializationException("Capability " + id + " has already been initialized");
		if (isClosed())
			throw new ToolInitializationException("Capability " + id + " has already been closed");

		this.agent = agent;
		for (Tool t : tools.values()) {
			t.init(agent); // Notice this throws an exception if the tool was already initialized

			// Notify listeners of new tool availability; notice they can complain
			ToolAddedEvent evt = new ToolAddedEvent(t);
			for (Listener l : listeners) {
				l.onToolAdded(evt);
				if (evt.isCancelled()) // Something went wrong
					throw new ToolInitializationException(evt.getCancelReason());
			}
		}
	}

	@Override
	public boolean addListener(@NonNull Listener l) {
		return listeners.add(l);
	}

	@Override
	public boolean removeListener(@NonNull Listener l) {
		return listeners.remove(l);
	}

	@Getter
	@Setter(AccessLevel.PROTECTED)
	private boolean closed = false;

	@Override
	public void close() {
		clear();
		closed = true;
	}
}
