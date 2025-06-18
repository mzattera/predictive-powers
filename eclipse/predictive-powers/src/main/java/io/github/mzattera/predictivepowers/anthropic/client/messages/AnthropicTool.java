/*
 * Copyright 2024 Massimiliano "Maxi" Zattera
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
package io.github.mzattera.predictivepowers.anthropic.client.messages;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.mzattera.predictivepowers.services.Agent;
import io.github.mzattera.predictivepowers.services.Capability;
import io.github.mzattera.predictivepowers.services.Tool;
import io.github.mzattera.predictivepowers.services.ToolInitializationException;
import io.github.mzattera.predictivepowers.services.messages.JsonSchema;
import io.github.mzattera.predictivepowers.services.messages.ToolCall;
import io.github.mzattera.predictivepowers.services.messages.ToolCallResult;
import lombok.NonNull;

/**
 * This class is a {@link Tool} that Anthropic API can use.
 */
public class AnthropicTool implements Tool {

	/**
	 * Utility class to (de)serialize JSON schema. This hides parametrs that are not
	 * required.
	 */
	static class AnthropicJsonSchema extends JsonSchema {
		AnthropicJsonSchema(@NonNull List<? extends ToolParameter> parameters) {
			super(parameters);
		}
	}

	/**
	 * For easier interoperability and abstraction, an AnthropicTool can be built as
	 * a wrapper around any Tool instance. If this was the case, this is the wrapped
	 * Tool.
	 */
	@JsonIgnore
	private final Tool wrappedTool;

	/**
	 * Builds an instance as a wrapper around an existing tool.
	 */
	public AnthropicTool(Tool tool) {
		wrappedTool = tool;
	}

	@Override
	public void close() throws Exception {
		wrappedTool.close();
	}

	@Override
	public boolean isClosed() {
		return wrappedTool.isClosed();
	}

	@Override
	@JsonProperty("name")
	public String getId() {
		return wrappedTool.getId();
	}

	@Override
	public String getDescription() {
		return wrappedTool.getDescription();
	}

	@JsonIgnore
	@Override
	public List<ToolParameter> getParameters() {
		return wrappedTool.getParameters();
	}

	// Used for serialization
	public AnthropicJsonSchema getInputSchema() {
		return new AnthropicJsonSchema(wrappedTool.getParameters());
	}

	@JsonIgnore
	@Override
	public void setCapability(Capability capability) {
		wrappedTool.setCapability(capability);
	}

	@JsonIgnore
	@Override
	public Capability getCapability() {
		return wrappedTool.getCapability();
	}

	@JsonIgnore
	@Override
	public boolean isInitialized() {
		return wrappedTool.isInitialized();
	}

	@Override
	public void init(@NonNull Agent agent) throws ToolInitializationException, IllegalStateException {
		wrappedTool.init(agent);
	}

	@Override
	public ToolCallResult invoke(@NonNull ToolCall call) throws Exception {
		return wrappedTool.invoke(call);
	}
}
