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
package io.github.mzattera.predictivepowers.openai.client.chat;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator;

import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * 
 */
/**
 * Format of the returned answer. Currently supports JSON and plain text.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
@ToString
public class ResponseFormat {

	/** Used for JSON serialization of function parameters as schema */
	private final static JsonSchemaGenerator SCHEMA_GENERATOR = new JsonSchemaGenerator(OpenAiClient.getJsonMapper());

	/** Denotes response format = text */
	public static final ResponseFormat TEXT = new ResponseFormat(Type.TEXT);

	/** Denotes response format = json_object */
	public static final ResponseFormat JSON_OBJECT = new ResponseFormat(Type.JSON_OBJECT);

	public enum Type {
		TEXT("text"), JSON_OBJECT("json_object"), JSON_SCHEMA("json_schema");

		private final String label;

		Type(String label) {
			this.label = label;
		}

		@JsonValue
		@Override
		public String toString() {
			return label;
		}
	}

	@NonNull
	private Type type;

	// Below parameters are for response format = json_schema

	private String name;
	private String description;
	private String schema;
	private Boolean strict;

	private ResponseFormat(@NonNull Type type) {
		this.type = type;
	}

	/**
	 * This constructor is to build a response format of type json_schema. The name
	 * of the schema and other optional parameters must be provided.
	 * 
	 * @param name
	 * @param description
	 * @param schema      The schema for the response format, described as String
	 *                    containing a JSON Schema object.
	 * @param strict
	 */
	public ResponseFormat(@NonNull String name, String description, String schema, Boolean strict) {
		this(Type.JSON_SCHEMA);
		this.name = name;
		this.schema = schema;
		this.strict = strict;
	}

	/**
	 * This constructor is to build a response format of type json_schema. The name
	 * of the schema and other optional parameters must be provided.
	 * 
	 * @param name
	 * @param description
	 * @param c           A class for a POJO that describes the The schema for the
	 *                    response format, described as String containing a JSON
	 *                    Schema object.
	 * @param strict
	 * 
	 * @throws JsonProcessingException
	 */
	public ResponseFormat(@NonNull String name, String description, Class<?> c, Boolean strict)
			throws JsonProcessingException {
		this(Type.JSON_SCHEMA);
		this.name = name;
		this.schema = OpenAiClient.getJsonMapper().writerWithDefaultPrettyPrinter()
				.writeValueAsString(SCHEMA_GENERATOR.generateJsonSchema(c));
		this.strict = strict;
	}
}
