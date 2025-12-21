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

/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.services;

import java.util.ArrayList;
import java.util.List;

import io.github.mzattera.predictivepowers.services.AbstractTool;
import io.github.mzattera.predictivepowers.services.Agent;
import io.github.mzattera.predictivepowers.services.ToolInitializationException;
import io.github.mzattera.predictivepowers.services.messages.ToolCall;
import io.github.mzattera.predictivepowers.services.messages.ToolCallResult;
import lombok.NonNull;

/**
 * Common base class for built-in server-side tools available to
 * OpenAiAssistants.
 * 
 * These tools cannot be added or removed, just enabled or disabled, as they are
 * always available to OpenAI assistants.
 */
public abstract class OpenAiAssistantTool extends AbstractTool {

	/**
	 * Enables this tool making it available to the assistant.
	 * 
	 * @return This tool.
	 */
	public abstract OpenAiAssistantTool enable();

	/**
	 * Disables the tool such that the assistant won't use it.
	 */
	public abstract void disable();

	/**
	 * 
	 * @returns True if the tool is enabled.
	 */
	public abstract boolean isEnabled();

	protected OpenAiAssistantTool(@NonNull String id, String description) {
		super(id, description, new ArrayList<>());
	}

	@Override
	public List<ToolParameter> getParameters() {
		// TODO maybe in a future far far away we implement this...
		throw new UnsupportedOperationException();
	}

	@Override
	protected void setAgent(Agent agent) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void init(@NonNull Agent agent) throws ToolInitializationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ToolCallResult invoke(@NonNull ToolCall call) throws Exception {
		throw new UnsupportedOperationException("This tool is only executed on the server side");
	}
}
