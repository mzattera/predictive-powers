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

import java.util.Collection;
import java.util.function.Supplier;

import lombok.NonNull;

/**
 * A capability is a set of {@link Tool}s that work together to provide a
 * specific functionality to {@link Agent}s.
 * 
 * In addition to the mere collection of tools forming a capability,
 * capabilities might provide additional features that the tools can leverage.
 * 
 * For example, a capability might be a local SQL database with attached a set
 * of functions that allow agents to query it.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
public interface Capability extends AutoCloseable {

	/**
	 * 
	 * @return Unique identifier for the capability.
	 */
	String getId();

	/**
	 * 
	 * @return A verbose description of what the capability provides, so that the
	 *         agent knows when to use it.
	 */
	String getDescription();

	/**
	 * Get tools available from this capability.
	 * 
	 * Notice this is expected to be an unmodifiable list; use other methods to
	 * populate tools list properly.
	 * 
	 * @return List of tool IDs.
	 */
	Collection<String> getToolIds();

	/**
	 * Get a new instance of a tool.
	 * 
	 * @param toolId Unique Id for the tool;
	 * 
	 * @return A new instance of the tool, or null if the tool is not provided by
	 *         this capability.
	 */
	Tool getNewToolInstance(@NonNull String toolId);

	/**
	 * Add one tool to the list of tools available from this capability.
	 * 
	 * @param toolId     Unique id for the tool.
	 * @param capability A factory method to create instances of the tool, when
	 *                   needed. capability capabilities.
	 */
	void putTool(@NonNull String toolId, @NonNull Supplier<? extends Tool> capability);

	/**
	 * Remove one tool from list of tools available from this capability.
	 * 
	 * @param id The unique ID for the tool.
	 */
	void removeTool(@NonNull String toolId);

	/**
	 * Remove all tools from this capability. This does not affect tools provided by
	 * capabilities.
	 */
	void clear();

	/**
	 * 
	 * @return True if the capability was already initialized.
	 */
	boolean isInitialized();

	/**
	 * This must be called by the agent once and only once before the capability can
	 * provide any tool.
	 * 
	 * @param The agent to which this capability is attached.
	 */
	void init(@NonNull Agent agent) throws ToolInitializationException;
}