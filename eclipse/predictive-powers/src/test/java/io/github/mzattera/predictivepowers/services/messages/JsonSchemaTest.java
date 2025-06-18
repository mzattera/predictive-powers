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
package io.github.mzattera.predictivepowers.services.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;

import io.github.mzattera.predictivepowers.services.Tool.ToolParameter;

public final class JsonSchemaTest {

	private JsonSchemaTest() {
	}

	@JsonSchemaDescription("This is a class used for testing")
	@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
	public static class SchemaTestModel {

		// --- String ---
		@JsonPropertyDescription("A required string field")
		@JsonProperty(required = true)
		public String requiredString;

		@JsonPropertyDescription("An optional string field")
		public String optionalString;

		// --- Number (floating point) ---
		@JsonPropertyDescription("A required number field")
		@JsonProperty(required = true)
		public Double requiredNumber;

		@JsonPropertyDescription("An optional number field")
		public Double optionalNumber;

		// --- Integer ---
		@JsonPropertyDescription("A required integer field")
		@JsonProperty(required = true)
		public Integer requiredInteger;

		@JsonPropertyDescription("An optional integer field")
		public Integer optionalInteger;

		// --- Boolean ---
		@JsonPropertyDescription("A required boolean field")
		@JsonProperty(required = true)
		public Boolean requiredBoolean;

		@JsonPropertyDescription("An optional boolean field")
		public Boolean optionalBoolean;

		// --- Array ---
		@JsonPropertyDescription("A required list of strings")
		@JsonProperty(required = true)
		public List<String> requiredArray;

		@JsonPropertyDescription("An optional list of integers")
		public List<Integer> optionalArray;

		// --- Array ---
		@JsonPropertyDescription("A required list of enums")
		@JsonProperty(required = true)
		public List<Status> requiredEnumArray;

		@JsonPropertyDescription("An optional list of enums")
		public List<Status> optionalEnumArray;

		// --- Enum (required) ---
		@JsonPropertyDescription("A required enum field")
		@JsonProperty(required = true)
		public Status requiredStatus;

		// --- Enum (optional) ---
		@JsonPropertyDescription("An optional enum field")
		public Status optionalStatus;

		// --- Field to be ignored ---
		@JsonIgnore
		public String ignoreMe;

		// --- Private field that will appear anyway because of @JsonAutoDetect ---
		@SuppressWarnings("unused")
		private String privateInfo;

		public enum Status {
			ACTIVE, INACTIVE, PENDING
		}

		public static class Nested {
			public String name;
			public int value;
			public Nested recursive;
		}

		public Nested nestedObject;
	}

	@Test
	public void test01() throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		JsonSchemaGenerator jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper);

		// Builds JSON schema from template class
		String inputSchema = objectMapper.writerWithDefaultPrettyPrinter()
				.writeValueAsString(jsonSchemaGenerator.generateJsonSchema(SchemaTestModel.class));

//			System.out.println(inputSchema);
//			System.out.println();

		// This uses deserialization
		JsonSchema schema = JsonSchema.fromSchema(inputSchema);
		List<ToolParameter> params = schema.asParameters();
//			for (ToolParameter p : params)
//				System.out.println(p.toString());

		// This uses serialization
		JsonSchema jsonSchema = new JsonSchema(schema.getTitle(), schema.getDescription(), params);
		String outputSchema = jsonSchema.asJsonSchema();

		// System.out.println(outputSchema);
		// System.out.println();

		// Check cascading serialization and deserialization gives back our initial
		// schema
		assertEquals(inputSchema, outputSchema);
	}

	@Test
	public void test03() throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		JsonSchemaGenerator jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper);

		// Builds JSON schema from template class
		String inputSchema = objectMapper.writerWithDefaultPrettyPrinter()
				.writeValueAsString(jsonSchemaGenerator.generateJsonSchema(SchemaTestModel.class));

//			System.out.println(inputSchema);
//			System.out.println();

		// Close loop
		String outputSchema = JsonSchema.fromSchema(SchemaTestModel.class).asJsonSchema();

		// System.out.println(outputSchema);
		// System.out.println();

		// Check cascading serialization and deserialization gives back our initial
		// schema
		assertEquals(inputSchema, outputSchema);
	}

	private final static String example = "{\r\n" //
			+ "  \"$schema\" : \"http://json-schema.org/draft-04/schema#\",\r\n" //
			+ "  \"title\" : \"Name\",\r\n" //
			+ "  \"type\" : \"object\",\r\n" //
			+ "  \"additionalProperties\" : false,\r\n" //
			+ "  \"properties\" : {\r\n" //
			+ "    \"address\" : {\r\n" //
			+ "      \"type\" : \"object\",\r\n" //
			+ "      \"additionalProperties\" : false,\r\n" //
			+ "      \"properties\" : {\r\n" //
			+ "        \"street\" : {\r\n" //
			+ "          \"type\" : \"string\"\r\n" //
			+ "        }\r\n" //
			+ "      }\r\n" //
			+ "    },\r\n" //
			+ "    \"addressArray\" : {\r\n" //
			+ "      \"type\" : \"array\",\r\n" //
			+ "      \"items\" : {\r\n" //
			+ "        \"type\" : \"object\",\r\n" //
			+ "        \"additionalProperties\" : false,\r\n" //
			+ "        \"properties\" : {\r\n" //
			+ "          \"street\" : {\r\n" //
			+ "            \"type\" : \"string\"\r\n" //
			+ "          }\r\n" //
			+ "        }\r\n" //
			+ "      }\r\n" //
			+ "    }\r\n" //
			+ "  }\r\n" //
			+ "}";

	@Test
	public void test02() throws JsonProcessingException {

		// This uses deserialization
		JsonSchema schema = JsonSchema.fromSchema(example);
		List<ToolParameter> params = schema.asParameters();
//		for (ToolParameter p : params)
//			System.out.println(p.toString());

		// This uses serialization
		JsonSchema jsonSchema = new JsonSchema(schema.getTitle(), params);
		String outputSchema = jsonSchema.asJsonSchema();

//		System.out.println(outputSchema);
//		System.out.println();

		// Check cascading serialization and deserialization gives back our initial
		// schema
		assertEquals(example, outputSchema);
	}
}
