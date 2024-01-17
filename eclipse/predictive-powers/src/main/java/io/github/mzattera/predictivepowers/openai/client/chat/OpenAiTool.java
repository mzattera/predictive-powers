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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

import io.github.mzattera.predictivepowers.services.Tool;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * This is a tool for OpenAI API. This is can be a function call for parallel
 * function calls in chat API, or a tool an OpenAI Assistant can use.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
@Getter
@Setter
//@SuperBuilder
@NoArgsConstructor
//@RequiredArgsConstructor
//@AllArgsConstructor
@ToString
public class OpenAiTool extends Tool {

	/** The code interpreter tool, for Assistants. */ 
	public final static OpenAiTool CODE_INTERPRETER = new OpenAiTool(Type.CODE_INTERPRETER);
	
	/** The retrieval tool, for Assistants. */ 
	public final static OpenAiTool RETRIEVAL = new OpenAiTool(Type.CODE_INTERPRETER);  

	public enum Type {

		FUNCTION("function"), CODE_INTERPRETER("code_interpreter"), RETRIEVAL("retrieval");

		private final @NonNull String label;

		private Type(@NonNull String label) {
			this.label = label;
		}

		@Override
		@JsonValue
		public String toString() {
			return label;
		}
	}

	@Override
	@JsonIgnore
	// Suppress serialization of this field
	public String getId() {
		if (type == Type.FUNCTION)
			return function.name;
		return type.toString();
	}

	@NonNull
	Type type;

	Function function;

	private OpenAiTool(Type type) {
		if (type == Type.FUNCTION)
			throw new IllegalArgumentException("Function must be provided");
		this.type = type;
	}

	public OpenAiTool(Function function) {
		this.type = Type.FUNCTION;
		this.function = function;
	}
}
