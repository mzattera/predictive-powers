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

package io.github.mzattera.predictivepowers.services.messages;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import io.github.mzattera.predictivepowers.services.Agent;
import io.github.mzattera.predictivepowers.services.Tool;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

/**
 * {@link Agent}s can invoke tools. This class represents a single tool
 * invocation, as part of a message.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
public final class ToolCall implements MessagePart {

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder
	 * 
	 * @author Luna
	 */
	public static final class Builder {
		private @NonNull String id;
		private Tool tool;
		private @NonNull String toolId;
		private @NonNull Map<String, Object> arguments = new HashMap<>();

		public Builder id(@NonNull String id) {
			this.id = id;
			return this;
		}

		public Builder tool(@NonNull Tool tool) {
			if ((toolId != null) && (!toolId.equals(tool.getId())))
				throw new IllegalArgumentException("Provided Tool must have same ID that was provided");
			this.tool = tool;
			this.toolId = tool.getId();
			return this;
		}

		public Builder toolId(@NonNull String toolId) {
			if ((tool != null) && (!toolId.equals(tool.getId())))
				throw new IllegalArgumentException("Provided Tool ID conflicts with provided ID");
			this.toolId = toolId;
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

		public Builder addArgument(@NonNull String name, Object value) throws JsonProcessingException {
			this.arguments.put(name, value);
			return this;
		}

		public ToolCall build() {
			return new ToolCall(id, tool, toolId, Map.copyOf(arguments));
		}
	}

	/**
	 * Unique ID for this tool call.
	 */
	private final @NonNull String id;

	/**
	 * The {@link Tool} being called.
	 * 
	 * Notice this can be null. If this happen, the developer should map this call to the proper tool.
	 */
	@JsonIgnore
	private final transient Tool tool;

	/**
	 * The ID of the {@link Tool} being called.
	 * 
	 * Notice this is always provided.
	 */
	private final @NonNull String toolId;

	/**
	 * Arguments to use in the call, as name/value pairs.
	 */
	// TODO URGENT This can probably be a Map<String,String> that will be immutable
	// ensuring deep immutability
	private final @NonNull Map<String, Object> arguments;

	@Override
	public Type getType() {
		return Type.TOOL_CALL;
	}

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
