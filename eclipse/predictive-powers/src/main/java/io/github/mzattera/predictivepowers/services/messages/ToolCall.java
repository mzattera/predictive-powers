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

package io.github.mzattera.predictivepowers.services.messages;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import io.github.mzattera.predictivepowers.services.Agent;
import io.github.mzattera.predictivepowers.services.Tool;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * {@link Agent}s can invoke tools. This interface represents a single tool
 * invocation, as part of a message.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
@Getter
@Setter
@ToString
public class ToolCall implements MessagePart {

	/**
	 * Sometimes it is not possible to create a {@link ToolCall} instance, as we
	 * have only the tool name. This class is meant to save the name until an
	 * instance of tool is available.
	 */
	// TODO URGENT is this used at all? Yes, by Anthropic, maybe it is not needed
	@Getter
	@Setter
	@ToString
	public static class ToolCallProxy extends ToolCall {
		private final @NonNull String toolName;

		public ToolCallProxy(@NonNull String id, @NonNull String toolName, Tool tool, @NonNull Map<String, Object> arguments) {
			super(id);
			this.toolName = toolName;
			setTool(tool);
			setArguments(arguments);
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder
	 * 
	 * @author Luna
	 */
	public static final class Builder {
		private String id;
		private Tool tool;
		private Map<String, Object> arguments = new HashMap<>();

		public Builder id(@NonNull String id) {
			this.id = id;
			return this;
		}

		public Builder tool(Tool tool) {
			this.tool = tool;
			return this;
		}

		/** Accepts any map whose values extend Object. */
		public Builder arguments(@NonNull Map<String, ? extends Object> args) {
			this.arguments = new HashMap<>(args);
			return this;
		}

		/**
		 * Parse a JSON string into the arguments map.
		 * 
		 * @throws JsonProcessingException
		 */
		public Builder arguments(@NonNull String json) throws JsonProcessingException {
			this.arguments = JsonSchema.JSON_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
			});
			return this;
		}

		public ToolCall build() {
			ToolCall tc = new ToolCall();
			tc.setId(Objects.requireNonNull(id, "id must not be null"));
			tc.setTool(tool);
			tc.setArguments(arguments);
			return tc;
		}
	}

	/**
	 * Unique ID for this tool call.
	 */
	@NonNull
	private String id;

	/**
	 * The tool being called. Notice it is not always guaranteed this to be set
	 * correctly, as some services might not be able to retrieve the proper Tool
	 * instance; this depends on the service generating the call. If this field is
	 * null, developers need to map this call to the proper tool externally from the
	 * service.
	 */
	private Tool tool;

	/**
	 * Arguments to use in the call, as name/value pairs.
	 */
	@NonNull
	private Map<String, Object> arguments = new HashMap<>();

	/**
	 * 
	 * @return This call arguments as a JSON object.
	 * @throws JsonProcessingException
	 */
	public String getArgumentsAsJson() throws JsonProcessingException {
		return JsonSchema.JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(arguments);
	}

	/**
	 * This method allows to get call arguments represented as an object of given
	 * class.
	 * 
	 * @return This call arguments as the given POJO.
	 * @throws JsonProcessingException
	 */
	public <T> T getArgumentsAsObject(Class<T> c) throws JsonProcessingException {
		return JsonSchema.JSON_MAPPER.readValue(getArgumentsAsJson(), c);
	}

	@Override
	public String getContent() {
		return toString();
	}

	/**
	 * Executes this call. Notice this will work only if {{@link #getTool()} returns
	 * a valid tool. Some services might not be able to retrieve the proper Tool
	 * instance; this depends on the service generating the call. If the tool is
	 * unset developers need to map this call to the proper tool externally from the
	 * service instead of using this method.
	 * 
	 * @return Result of invoking the tool.
	 * @throws Exception If an error occurs while executing the call.
	 */
	public ToolCallResult execute() throws Exception {
		return getTool().invoke(this);
	}
}
