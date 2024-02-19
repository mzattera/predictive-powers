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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator;

import io.github.mzattera.predictivepowers.services.Tool;
import io.github.mzattera.predictivepowers.services.Tool.ToolParameter;
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

	/**
	 * Utility class to deserialize JSON schema.
	 */
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Getter
	@Setter
	@ToString
	static class JsonSchema {

		@JsonProperty("$schema")
		String schema = "http://json-schema.org/draft-04/schema#";

		String title = "Function Parameters";
		String type = "object";
		boolean additionalProperties = false;
		List<ToolParameter> properties = new ArrayList<>();
		List<String> required;

		JsonSchema(List<? extends ToolParameter> parameters) {
			properties.addAll(parameters);
		}
	}

	/** Used for JSON serialization of function parameters as schema */
	private final static JsonSchemaGenerator schemaGenerator = new JsonSchemaGenerator(new ObjectMapper());

	/**
	 * Custom serializer to create JSON schema for a class.
	 */
	static final class ParametersSerializer extends StdSerializer<Class<?>> {

		private static final long serialVersionUID = 1L;

		public ParametersSerializer() {
			super(Class.class, false);
		}

		@Override
		public void serialize(Class<?> c, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
				throws IOException, JsonProcessingException {
			jsonGenerator.writeTree(schemaGenerator.generateJsonSchema(c));
		}
	}

	/**
	 * Custom serializer to create JSON schema out of a JsonSchema instance.
	 */
	static final class JsonSchemaSerializer extends StdSerializer<JsonSchema> {

		private static final long serialVersionUID = 1L;

		public JsonSchemaSerializer() {
			super(JsonSchema.class, false);
		}

		// TODO Test deserailization as well

		@Override
		public void serialize(JsonSchema schema, JsonGenerator gen, SerializerProvider provider) throws IOException {
			gen.writeStartObject();

			if (schema.schema != null) {
				gen.writeStringField("$schema", schema.schema);
			}
			if (schema.title != null) {
				gen.writeStringField("title", schema.title);
			}
			if (schema.type != null) {
				gen.writeStringField("type", schema.type.toString());
			}
			gen.writeBooleanField("additionalProperties", schema.additionalProperties);

			// Serialize properties
			if (schema.properties != null && !schema.properties.isEmpty()) {
				gen.writeObjectFieldStart("properties");
				for (ToolParameter prop : schema.properties) {
					gen.writeObjectFieldStart(prop.name);
					gen.writeStringField("type", prop.type.toString());
					if (prop.emum != null && !prop.emum.isEmpty()) {
						gen.writeArrayFieldStart("enum");
						for (String enumValue : prop.emum) {
							gen.writeString(enumValue);
						}
						gen.writeEndArray();
					}
					if (prop.description != null) {
						gen.writeStringField("description", prop.description);
					}
					gen.writeEndObject();
				}
				gen.writeEndObject();
			}

			// List required fields

			// Serialize required fields as an array
			gen.writeArrayFieldStart("required");
			for (ToolParameter prop : schema.properties) {
				if (prop.isRequired())
					gen.writeString(prop.getName());
			}
			gen.writeEndArray();

			gen.writeEndObject();
		}
	}

	/**
	 * Custom de-serializer from JSON schema into JsonSchema instance.
	 */
	static final class JsonSchemaDeserializer extends JsonDeserializer<JsonSchema> {

		@Override
		public JsonSchema deserialize(JsonParser jp, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {

			ObjectMapper tmp = new ObjectMapper();
			JsonSchema schema = new JsonSchema();

			JsonNode rootNode = jp.getCodec().readTree(jp);
			schema.type = rootNode.path("type").asText();
			if (!"object".equals(schema.type))
				throw new IllegalArgumentException();
			schema.schema = rootNode.path("$schema").asText();
			schema.title = rootNode.path("title").asText();
			schema.additionalProperties = rootNode.path("additionalProperties").asBoolean();

			JsonNode requiredNode = rootNode.path("required");
			schema.required = new ArrayList<>();
			requiredNode.forEach(jsonNode -> schema.required.add(jsonNode.asText()));

			JsonNode propertiesNode = rootNode.path("properties");
			schema.properties = new ArrayList<>();
			Iterator<Map.Entry<String, JsonNode>> fields = propertiesNode.fields();
			while (fields.hasNext()) {
				Map.Entry<String, JsonNode> entry = fields.next();
				ToolParameter parameter = new ToolParameter();
				parameter.name = entry.getKey();
				JsonNode description = entry.getValue().get("description");
				parameter.description = (description == null) ? null : description.asText();

				String typeStr = entry.getValue().path("type").asText();
				switch (typeStr) {
				case "integer":
					parameter.type = ToolParameter.Type.INTEGER;
					break;
				case "number":
					parameter.type = ToolParameter.Type.DOUBLE;
					break;
				case "boolean":
					parameter.type = ToolParameter.Type.BOOLEAN;
					break;
				case "string":
					parameter.type = ToolParameter.Type.STRING;
					if (entry.getValue().has("enum")) {
						parameter.emum = tmp.convertValue(entry.getValue().path("enum"),
								new TypeReference<List<String>>() {
								});
					} else {
						parameter.type = ToolParameter.Type.STRING;
					}
					break;
				default:
					throw new IOException("Unrecognized parameter type: " + typeStr);
				}

				parameter.required = schema.required.contains(parameter.name);
				schema.properties.add(parameter);
			}

			return schema;
		}

	}

	private final static ObjectMapper JSON_MAPPER;
	static {
		JSON_MAPPER = new ObjectMapper();
		JSON_MAPPER.registerModule(new SimpleModule().addSerializer(new ParametersSerializer()));
		JSON_MAPPER.registerModule(new SimpleModule().addSerializer(new JsonSchemaSerializer()));
		JSON_MAPPER.registerModule(new SimpleModule().addDeserializer(JsonSchema.class, new JsonSchemaDeserializer()));
		JSON_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	/**
	 * Converts a class which schema describes parameters for a Tool into a list of
	 * ToolParameters.
	 * 
	 * Developers can use JSON schema annotations in a class definition to describe
	 * the parameters for the tool, then use this method to create tools parameters
	 * if needed.
	 * 
	 * This should be faster and easier to read than listing all parameters
	 * individually.
	 * 
	 * See {@link io.github.mzattera.predictivepowers.examples.FunctionCallExample}
	 * or
	 * {@linkplain https://platform.openai.com/docs/guides/text-generation/function-calling
	 * here}. for examples.
	 */
	public static List<ToolParameter> getParametersFromSchema(Class<?> c) {

		try {
			// Generate schema from class
			String jsonString = JSON_MAPPER.writeValueAsString(c);

			// Deserailize schema
			return JSON_MAPPER.readValue(jsonString, JsonSchema.class).getProperties();
		} catch (JsonProcessingException e) {
			// Should never happen
			e.printStackTrace(System.err);
			return null;
		}
	}

	public Function(String name, String description, List<? extends ToolParameter> parameters) {
		this.name = name;
		this.description = description;
		this.parameters = new JsonSchema(parameters);
	}

	public Function(String name, String description, Class<?> schema) {
		this(name, description, getParametersFromSchema(schema));
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
	 * {@link #schema} used for serialization. YOu can still use getter and setter
	 * methods to set function parameters using this field.
	 * 
	 * See
	 * {@linkplain https://platform.openai.com/docs/guides/text-generation/function-calling
	 * here}.
	 */
	@NonNull // OpenAi errors otherwise
	@JsonSerialize(using = JsonSchemaSerializer.class)
	@JsonDeserialize(using = JsonSchemaDeserializer.class)
	private JsonSchema parameters;
}