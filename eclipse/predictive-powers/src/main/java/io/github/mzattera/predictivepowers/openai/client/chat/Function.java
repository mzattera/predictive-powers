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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.mzattera.predictivepowers.services.Tool;
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
@Getter
@Setter
@Builder
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
@ToString
public class Function {

	/**
	 * The name of the function to be called. Must be a-z, A-Z, 0-9, or contain
	 * underscores and dashes, with a maximum length of 64.
	 */
	@NonNull
	String name;

	/**
	 * The description of what the function does.
	 */
	String description;

	/**
	 * The parameters the functions accepts.
	 * 
	 * This is a class which fields describe the function parameters and that will
	 * be serialized as a JSON Schema object. Please notice that at the time being
	 * OpenAI seems to support only parameters that are either native Java types,
	 * String, and enumerations.
	 */
	@JsonSerialize(using = Tool.ParametersSerializer.class, as = Class.class)
	@NonNull // OpenAi errors otherwise
	Class<?> parameters;
}
