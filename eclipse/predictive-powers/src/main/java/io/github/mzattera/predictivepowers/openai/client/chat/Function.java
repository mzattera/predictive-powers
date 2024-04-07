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

package io.github.mzattera.predictivepowers.openai.client.chat;

import java.util.List;

import io.github.mzattera.predictivepowers.services.Tool;
import io.github.mzattera.predictivepowers.services.Tool.ToolParameter;
import io.github.mzattera.predictivepowers.services.messages.JsonSchema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * This is a function definition for OpenAI function calling feature in chat
 * completion API.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class Function {

	public Function(String name, String description, List<? extends ToolParameter> parameters) {
		this.name = name;
		this.description = description;
		this.parameters = new JsonSchema(parameters);
	}

	public Function(String name, String description, Class<?> schema) {
		this(name, description, JsonSchema.getParametersFromSchema(schema));
	}

	public Function(Tool tool) {
		this(tool.getId(), tool.getDescription(), tool.getParameters());
	}

	/**
	 * The name of the function to be called. Must be a-z, A-Z, 0-9, or contain
	 * underscores and dashes, with a maximum length of 64.
	 */
	@NonNull
	private String name;

	/**
	 * The description of what the function does.
	 */
	private String description;

	/**
	 * The parameters the functions accepts. This is a string version of
	 * {@link #schema} used for serialization. You can still use getter and setter
	 * methods to set function parameters using this field.
	 * 
	 * See
	 * {@linkplain https://platform.openai.com/docs/guides/text-generation/function-calling
	 * here}.
	 */
	@NonNull // OpenAi errors otherwise
	private JsonSchema parameters;
}