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

package io.github.mzattera.predictivepowers.services;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator;

import io.github.mzattera.predictivepowers.services.messages.ToolCall;
import io.github.mzattera.predictivepowers.services.messages.ToolCallResult;
import lombok.Getter;
import lombok.NonNull;

/**
 * This is a tool that an {@link Agent} can invoke to perform a task.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
public interface Tool {

	/** Used for JSON (de)serialization of function parameters as schema */
	// TODO is this needed?
	@Getter
	public final static JsonSchemaGenerator schemaGenerator = new JsonSchemaGenerator(new ObjectMapper());

	/**
	 * Convenience class for a tool with no parameters.
	 */
	public final static class NoParameters {
	}

	/**
	 * Custom serializer to create JSON schema for function parameters.
	 */
	public static final class ParametersSerializer extends JsonSerializer<Class<?>> {

		@Override
		public void serialize(Class<?> c, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
				throws IOException, JsonProcessingException {
			jsonGenerator.writeTree(schemaGenerator.generateJsonSchema(c));
		}
	}

	/**
	 * Unique identifier for the tool.
	 * 
	 * @return
	 */
	String getId();

	/**
	 * 
	 * @return A verbose description of what the tool does does, so that the agent
	 *         knows when to call it.
	 */
	String getDescription();

	/**
	 * 
	 * @return A class which JSON Schema Object will be used to describe the
	 *         parameters of the tool.
	 * 
	 *         See
	 *         {@link io.github.mzattera.predictivepowers.examples.FunctionCallExample}
	 *         or
	 *         {@linkplain https://platform.openai.com/docs/guides/text-generation/function-calling
	 *         here}. for examples.
	 */
	Class<?> getParameterSchema();

	/**
	 * This must be called by the agent before any invocation to this tool.
	 * 
	 * @return True if the tool was successfully initialized and can now be invoked,
	 *         false otherwise.
	 */
	void init(@NonNull Agent agent) throws ToolInitializationException;

	/**
	 * Invokes (executes) the tool.
	 * 
	 * @param arguments Parameters to pass to the tool for this invocation.
	 * @return The result of calling the tool.
	 */
	ToolCallResult invoke(@NonNull ToolCall call) throws Exception;
}
