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

import java.util.ArrayList;
import java.util.List;

import io.github.mzattera.predictivepowers.services.AbstractTool;
import io.github.mzattera.predictivepowers.services.Agent;
import io.github.mzattera.predictivepowers.services.ToolInitializationException;
import io.github.mzattera.predictivepowers.services.messages.ToolCall;
import io.github.mzattera.predictivepowers.services.messages.ToolCallResult;
import lombok.Getter;
import lombok.NonNull;

/**
 * Common base class for built-in tools available to OpenAiAssistants.
 * 
 * 
 */
public class OpenAiAssistantTool extends AbstractTool {

	/**
	 * If true, tool will be enabled and available to the assistant.
	 */
	@Getter
	private boolean enabled = false;

	/**
	 * Enables this tool making it available to the assistant.
	 * 
	 * @return This tool.
	 */
	public OpenAiAssistantTool enable() {
		enabled = true;
		return this;
	}

	/**
	 * Disables the tool such that the assistant won-t use it.
	 */
	public void disable() {
		enabled = false;
	}

	protected OpenAiAssistantTool(@NonNull String id) {
		this(id, id, new ArrayList<>());
	}

	protected OpenAiAssistantTool(@NonNull String id, String description) {
		this(id, description, new ArrayList<>());
	}

	protected OpenAiAssistantTool(@NonNull String id, String description, @NonNull List<? extends ToolParameter> parameters) {
		super(id, description, parameters);
	}

	protected OpenAiAssistantTool(@NonNull String id, String description, @NonNull Class<?> schema) {
		super(id, description, schema);
	}

	@Override
	protected OpenAiAssistant getAgent() {
		return (OpenAiAssistant) super.getAgent();
	}

	@Override
	protected void setAgent(Agent agent) {
		if (agent instanceof OpenAiAssistant)
			super.setAgent(agent);
		else
			throw new IllegalArgumentException("This tool can only be used by an OpenAiAssistant instance");
	}

	@Override
	public void init(@NonNull Agent agent) throws ToolInitializationException {
		if (agent instanceof OpenAiAssistant)
			super.init(agent);
		else
			throw new IllegalArgumentException("This tool can only be used by an OpenAiAssistant instance");
	}

	@Override
	public ToolCallResult invoke(@NonNull ToolCall call) throws Exception {
		throw new UnsupportedOperationException("This tool is only executed on the server side");
	}
}
