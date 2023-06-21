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

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator;

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
	 * Custom serializer to create JSON schema for function parameters.
	 */
	private static class ParametersSerializer extends JsonSerializer<Class<?>> {

		// Note it is safe to have these static
		private final static JsonSchemaGenerator GENERATOR = new JsonSchemaGenerator(new ObjectMapper());

		@Override
		public void serialize(Class<?> c, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
				throws IOException, JsonProcessingException {
			jsonGenerator.writeTree(GENERATOR.generateJsonSchema(c));
		}
	}

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
	 * be serialized as a JSON Schema object.
	 */
	@JsonSerialize(using = ParametersSerializer.class, as = Class.class)
	Class<?> parameters;
}
