/*
 * Copyright 2023-2025 Massimiliano "Maxi" Zattera
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

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * A capability is a set of {@link Tool}s that work together to provide a
 * specific functionality to {@link Agent}s.
 * 
 * In addition to the mere collection of tools forming a capability,
 * capabilities might provide additional features that the tools can leverage.
 * 
 * For example, a capability might be a local SQL database with a set of
 * functions attached to allow agents to interact withit.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
public interface Capability extends AutoCloseable {

	/**
	 * Event fired before a tool is added to a Capability. Can be cancelled by the
	 * listener.
	 * 
	 * 
	 * @author Luna
	 */
	@RequiredArgsConstructor
	public class ToolAddedEvent {

		@Getter
		@NonNull
		private final Tool tool;

		@Getter
		private boolean cancelled = false;

		@Getter
		private String cancelReason;

		/**
		 * Cancels the event and records a reason.
		 *
		 * @param reason why the tool should not be added
		 */
		public void cancel(@NonNull String reason) {
			this.cancelled = true;
			this.cancelReason = reason;
		}
	}

	/**
	 * Event fired after a tool is removed from a Capability.
	 * 
	 * @author Luna
	 */
	@RequiredArgsConstructor
	public class ToolRemovedEvent {

		@Getter
		@NonNull
		private final Tool tool;
	}

	/**
	 * Interface that all capability listeners must implement.
	 * 
	 * @author Luna
	 */
	public static interface Listener {

		/**
		 * Called before a tool is added. Can be cancelled via
		 * {@link ToolAddedEvent#cancel(String)}.
		 */
		void onToolAdded(@NonNull ToolAddedEvent event);

		/**
		 * Called after a tool is removed.
		 */
		void onToolRemoved(@NonNull ToolRemovedEvent event);
	}

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
	 * Get list of tools available from this capability.
	 * 
	 * Notice this is expected to be an unmodifiable list; use
	 * {@link #putTool(Tool)} and {@link #removeTool(Tool)} to manage the list
	 * properly.
	 * 
	 * @return List of tool IDs.
	 */
	List<Tool> getTools();

	/**
	 * Get IDs of tools available from this capability.
	 */
	List<String> getToolIds();

	/**
	 * Retrieves a tool from the capability.
	 * 
	 * @param toolId Unique ID of the tool to retrieve.
	 * @return The required tool, or null if it cannot be found.
	 */
	Tool getTool(@NonNull String toolId);

	/**
	 * Add one tool to the list of tools available from this capability. If the
	 * capability is already initialized, the tool is also initialized. This will
	 * trigger {@link Listener#onToolAdded(Tool)}.
	 */
	void putTool(@NonNull Tool tool) throws ToolInitializationException;

	/**
	 * Remove one tool from list of tools provided by this capability; the tool is
	 * automatically closed. This will trigger {@link Listener#onToolRemoved(Tool)}
	 * before the tool is closed.
	 * 
	 * @param id The unique ID for the tool.
	 */
	void removeTool(@NonNull String toolId);

	/**
	 * Remove one tool from list of tools provided by this capability; the tool is
	 * automatically closed. This will trigger {@link Listener#onToolRemoved(Tool)}
	 * before the tool is closed.
	 */
	void removeTool(@NonNull Tool tool);

	/**
	 * Remove all tools from this capability; all tools are automatically closed.
	 * This will trigger {@link Listener#onToolRemoved(Tool)} for each tool.
	 */
	void clear();

	/**
	 * 
	 * @return True if the capability was already initialized.
	 */
	boolean isInitialized();

	/**
	 * This must be called by the agent once and only once before any tool provided
	 * by the capability can be used.
	 * 
	 * This will in turn initialize the tools in the capability and trigger
	 * {@link Listener#onToolAdded(Tool)} for each tool in the capability.
	 * 
	 * @param The agent to which this capability is attached.
	 * @throws ToolInitializationException If any tool failed to initialize, or the
	 *                                     capability was already initialized or
	 *                                     closed.
	 */
	void init(@NonNull Agent agent) throws ToolInitializationException;

	/**
	 * Add a {@link Listener} for this capability.
	 * 
	 * @return true if the listener was not in the list already.
	 */
	boolean addListener(@NonNull Listener l);

	/**
	 * Remove given {@link Listener}.
	 * 
	 * @return true if the listener was in the list.
	 */
	boolean removeListener(@NonNull Listener l);

	/**
	 * 
	 * @return True if the capability was already closed.
	 */
	boolean isClosed();

	/**
	 * Disposes this capability.
	 * 
	 * This will dispose tools provided by the capability as well.
	 */
	@Override
	void close() throws Exception;
}