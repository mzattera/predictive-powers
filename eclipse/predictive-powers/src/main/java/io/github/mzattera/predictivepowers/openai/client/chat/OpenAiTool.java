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

import io.github.mzattera.predictivepowers.services.Agent;
import io.github.mzattera.predictivepowers.services.Tool;
import io.github.mzattera.predictivepowers.services.ToolInitializationException;
import io.github.mzattera.predictivepowers.services.messages.ToolCall;
import io.github.mzattera.predictivepowers.services.messages.ToolCallResult;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * This is a tool for OpenAI API. This is can be a function call for parallel
 * function calls in chat API, or a tool an OpenAI Assistant can use.
 * 
 * Note this implements the {@link Tool} interface, to allow abstracting OpenAI
 * APIs into sevices.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
//@RequiredArgsConstructor
@Getter
@Setter
@ToString
public class OpenAiTool implements Tool {

	/** The code interpreter tool, for Assistants. */
	public final static OpenAiTool CODE_INTERPRETER = new OpenAiTool(Type.CODE_INTERPRETER);

	/** The retrieval tool, for Assistants. */
	public final static OpenAiTool RETRIEVAL = new OpenAiTool(Type.RETRIEVAL);

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

	/**
	 * For easier interoperability and abstraction, an OpenAiTool can be built as a
	 * wrapper around any Tool instance. If this was the case, this is the wrapped
	 * Tool.
	 */
	@JsonIgnore
	@Getter(AccessLevel.PROTECTED)
	@Setter(AccessLevel.NONE)
	protected Tool wrappedTool;

	@NonNull
	private Type type;

	@NonNull
	private Function function;

	@Override
	@JsonIgnore
	public String getId() {
		if (wrappedTool != null)
			return wrappedTool.getId();
		if (type == Type.FUNCTION)
			return function.getName();
		return type.toString();
	}

	@JsonIgnore
	@Override
	public String getDescription() {
		if (wrappedTool != null)
			return wrappedTool.getDescription();
		if (type == Type.FUNCTION)
			return function.getDescription();
		return ("This is the " + type + " tool available to OpenAI assistants.");
	}

	@JsonIgnore
	@Override
	public Class<?> getParameterSchema() {
		if (wrappedTool != null)
			return wrappedTool.getParameterSchema();
		if (type == Type.FUNCTION)
			return function.getParameters();
		return Tool.NoParameters.class;
	}

	private OpenAiTool(Type type) {
		if (type == Type.FUNCTION)
			throw new IllegalArgumentException("Function must be provided");
		this.type = type;
		this.wrappedTool = null;
	}

	/**
	 * Create a "function" from its description. Notice that if you use this
	 * construction than {@ #invoke(Map)} will throw UnsoupportedMethodException
	 * when called, unless overridden in a subclass.
	 * 
	 * Notice that an easier method to create OpenAiTools is to create a class that
	 * implements the Tool interface, and wrap an OpenAiTool around it, so you get a
	 * reusable Tool that can also be used with other APIs (hopefully).
	 */
	OpenAiTool(Function function) {
		this.type = Type.FUNCTION;
		this.function = function;
		this.wrappedTool = null;
	}

	/**
	 * Create an instance of this class as a wrapper around given Tool. This will
	 * automatically invoke the tool when {@ #invoke(Map)} is called for this
	 * instance.
	 */
	public OpenAiTool(Tool tool) {
		this.type = Type.FUNCTION;
		this.function = Function.builder() //
				.name(tool.getId()) //
				.description(tool.getDescription()) //
				.parameters(tool.getParameterSchema()).build();
		this.wrappedTool = tool;
	}

	@Override
	public boolean equals(Object o) {
		if ((o == null) || !(o instanceof OpenAiTool))
			return false;
		return this.getId().equals(((OpenAiTool) o).getId());
	}

	@Override
	public int hashCode() {
		return this.getId().hashCode();
	}

	@Override
	public void init(@NonNull Agent agent) throws ToolInitializationException {
		if (wrappedTool != null)
			wrappedTool.init(agent);
	}

	/**
	 * Unless this instance has been built as a wrapper, this method will throw
	 * UnsupportedOperationException.
	 * 
	 * @throws Exception
	 */
	@Override
	public ToolCallResult invoke(@NonNull ToolCall call) throws Exception {
		if (wrappedTool != null)
			return wrappedTool.invoke(call);
		throw new UnsupportedOperationException();
	}
}
