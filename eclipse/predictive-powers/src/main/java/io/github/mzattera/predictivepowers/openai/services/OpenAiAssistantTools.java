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

/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.services;

import java.util.List;
import java.util.stream.Collectors;

import io.github.mzattera.predictivepowers.services.Agent;
import io.github.mzattera.predictivepowers.services.Capability;
import io.github.mzattera.predictivepowers.services.Tool;
import io.github.mzattera.predictivepowers.services.ToolInitializationException;
import lombok.Getter;
import lombok.NonNull;

/**
 * This capability represents a set of OpenAI tools (e.g. File Search) available
 * to an {@link OpenAiAssistant}.
 */
public final class OpenAiAssistantTools implements Capability {


	public final static String ID = "_OpenAiTool$";

	// TODO URGENT Add code interpreter
	
	@Getter
	private final OpenAiFileSearchTool fileSearchTool = new OpenAiFileSearchTool();
	
	// TODO Urgent see if constructor can be make available only by assistant
	OpenAiAssistantTools(){}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDescription() {
		return "OpenAI buil-in tools available to OpenAI assistants.";
	}

	@Override
	public List<Tool> getTools() {
		return List.of(fileSearchTool);
	}

	@Override
	public List<String> getToolIds() {
		return getTools().stream().map(Tool::getId).collect(Collectors.toList());
	}

	@Override
	public Tool getTool(@NonNull String toolId) {
		for (Tool t : getTools())
			if (toolId.equals(t.getId()))
				return t;
		return null;
	}
	
	@Override
	public void putTool(@NonNull Tool tool) throws ToolInitializationException {
		throw new UnsupportedOperationException(
				"Tools cannot be added to this capability. Use methods available to the buil-in tools instead to enable them");
	}

	@Override
	public void removeTool(@NonNull String toolId) {
		throw new UnsupportedOperationException(
				"Tools cannot be removed from this capability. Use methods available to the buil-in tools instead to disable them");
	}

	@Override
	public void removeTool(@NonNull Tool tool) {
		throw new UnsupportedOperationException(
				"Tools cannot be removed from this capability. Use methods available to the buil-in tools instead to disable them");
	}

	@Override
	public void clear() {
		fileSearchTool.disable();
	}

	private OpenAiAssistant agent = null;

	@Override
	public boolean isInitialized() {
		return (agent != null);
	}

	@Override
	public void init(@NonNull Agent agent) throws ToolInitializationException {
		if (isInitialized())
			throw new ToolInitializationException("Capability " + getId() + " has already been initialized");
		if (isClosed())
			throw new ToolInitializationException("Capability " + getId() + " has already been closed");
		if (agent instanceof OpenAiAssistant)
			this.agent = (OpenAiAssistant) agent;
		else
			throw new ToolInitializationException("Capability " + getId() + " can only be used by OpenAiAssistant instances");
	}

	@Override
	public boolean addListener(@NonNull Listener l) {
		return true;
	}

	@Override
	public boolean removeListener(@NonNull Listener l) {
		return false;
	}

	@Getter
	private boolean closed = false;

	@Override
	public void close() {
		clear();
		closed = true;
	}
}
