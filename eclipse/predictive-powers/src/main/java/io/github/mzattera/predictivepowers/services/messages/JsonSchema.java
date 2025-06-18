/*
 * Copyright 2023-2024 Massimiliano "Maxi" Zattera
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;
import com.openai.core.JsonValue;

import io.github.mzattera.predictivepowers.services.Tool;
import io.github.mzattera.predictivepowers.services.Tool.ToolParameter;
import io.github.mzattera.predictivepowers.services.Tool.ToolParameter.Type;
import io.github.mzattera.predictivepowers.services.messages.JsonSchema.JsonSchemaDeserializer;
import io.github.mzattera.predictivepowers.services.messages.JsonSchema.JsonSchemaSerializer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * This class can be used to create JSON Schema which is used in several APIs to
 * describe output format for a model or to describe {@link Tool} parameters.
 * 
 * The class provide methods to convert {@link ToolParameter}s into JSON Schema
 * and vice-versa, including using a Class as template to describe the
 * parameters (see {@link #getParameters(Class)}). Note, that we use
 * {@link ToolParameter} class itself to describe JSON schema fields.
 * 
 * Instances of this class can be created so that schema fields are easily
 * accessible, if needed.
 * 
 * @see <a href=
 *      "https://json-schema.org/understanding-json-schema">Understanding JSON
 *      Schema</a>
 * 
 * @author Massimiliano "Maxi" Zattera
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Setter
@ToString
@JsonSerialize(using = JsonSchemaSerializer.class)
@JsonDeserialize(using = JsonSchemaDeserializer.class)
public class JsonSchema {

	private final static Logger LOG = LoggerFactory.getLogger(JsonSchema.class);

	/**
	 * Deserializer
	 * 
	 * @author Luna
	 */
	public static class JsonSchemaDeserializer extends JsonDeserializer<JsonSchema> {

		@Override
		public JsonSchema deserialize(JsonParser p, DeserializationContext ctx) throws IOException {

			ObjectMapper om = (ObjectMapper) p.getCodec();
			JsonNode rootNode = om.readTree(p);

			// Collect all definitions
			Map<String, ToolParameter> defs = new HashMap<>();
			JsonNode defsNode = rootNode.get("definitions");
			if (defsNode != null && defsNode.isObject()) {
				defsNode.fields().forEachRemaining(e -> defs.put(e.getKey(), parseDefinition(e.getValue(), om)));
			}

			// Build main schema
			JsonSchema schema = new JsonSchema();
			schema.setSchema(rootNode.path("$schema").asText("http://json-schema.org/draft-04/schema#"));
			schema.setTitle(rootNode.path("title").asText(null));

			ToolParameter root = new ToolParameter();
			root.setType(Type.OBJECT);
			fillObject(rootNode, root, defs, om);
			schema.setRoot(root);

			return schema;
		}

		private ToolParameter parseDefinition(JsonNode n, ObjectMapper om) {
			ToolParameter tp = new ToolParameter();
			tp.setType(Type.OBJECT);
			fillObject(n, tp, Collections.emptyMap(), om);
			return tp;
		}

		private void fillObject(JsonNode node, ToolParameter target, Map<String, ToolParameter> defs, ObjectMapper om) {

			if (node.has("description"))
				target.setDescription(node.get("description").asText());

			// required
			Set<String> req = new HashSet<>();
			if (node.has("required"))
				node.get("required").forEach(r -> req.add(r.asText()));

			// properties
			JsonNode props = node.get("properties");
			if (props == null)
				return;
			props.fields().forEachRemaining(e -> {
				ToolParameter child = parseParam(e.getValue(), defs, om);
				child.setName(e.getKey());
				child.setRequired(req.contains(e.getKey()));
				target.getObjectFields().add(child);
			});
		}

		private ToolParameter parseParam(JsonNode node, Map<String, ToolParameter> defs, ObjectMapper om) {
			ToolParameter p = new ToolParameter();

			/* ----------------- $ref ---------------------------------------- */
			if (node.has("$ref")) {
				String ref = node.get("$ref").asText(); // es. #/definitions/Address
				String name = ref.substring(ref.lastIndexOf('/') + 1);
				p.setType(Type.OBJECT);

				// manteniamo il segnale di $ref sfruttando arrayItemType
				ToolParameter refTp = defs.getOrDefault(name, new ToolParameter());
				refTp.setName(name);
				p.setObjectType(refTp);
				return p;
			}

			/* ----------------- explicit type ----------------------------- */
			String t = node.path("type").asText("object");
			switch (t) {
			case "string":
				if (node.has("enum")) {
					p.setType(Type.ENUMERATION);
					List<String> vals = new ArrayList<>();
					node.get("enum").forEach(v -> vals.add(v.asText()));
					p.setEnumValues(vals);
				} else
					p.setType(Type.STRING);
				break;
			case "integer":
				p.setType(Type.INTEGER);
				break;
			case "number":
				p.setType(Type.DOUBLE);
				break;
			case "boolean":
				p.setType(Type.BOOLEAN);
				break;

			case "array":
				p.setType(Type.ARRAY);
				p.setObjectType(parseParam(node.get("items"), defs, om));
				break;

			case "object":
			default:
				p.setType(Type.OBJECT);
				fillObject(node, p, defs, om);
			}

			if (node.has("description"))
				p.setDescription(node.get("description").asText());

			return p;
		}
	}

	/**
	 * Serializer.
	 * 
	 * @author Luna
	 */
	public static class JsonSchemaSerializer extends StdSerializer<JsonSchema> {

		private static final long serialVersionUID = 1L;

		public JsonSchemaSerializer() {
			super(JsonSchema.class);
		}

		@Override
		public void serialize(JsonSchema schema, JsonGenerator gen, SerializerProvider prov) throws IOException {

			// Collect all referenced objects
			Map<String, ToolParameter> defs = new HashMap<>();
			collectDefinitions(schema.getRoot(), defs);

			// Schema header
			gen.writeStartObject();
			gen.writeStringField("$schema", schema.getSchema());
			if (schema.getTitle() != null)
				gen.writeStringField("title", schema.getTitle());

			// Schema body
			writeObject(gen, schema.getRoot(), true);

			// Definitions, if any
			if (!defs.isEmpty()) {
				gen.writeObjectFieldStart("definitions");
				for (Map.Entry<String, ToolParameter> e : defs.entrySet()) {
					gen.writeFieldName(e.getKey());
					gen.writeStartObject();
					writeObject(gen, e.getValue(), false);
					gen.writeEndObject();
				}
				gen.writeEndObject(); // definitions
			}

			gen.writeEndObject(); // schema root
		}

		private void collectDefinitions(ToolParameter p, Map<String, ToolParameter> defs) {

			if (p.getType() == Type.OBJECT) {
				// $ref ?
				if (p.getObjectType() != null && p.getObjectFields().isEmpty()) {
					ToolParameter ref = p.getObjectType();
					defs.putIfAbsent(ref.getName(), ref);
				} else { // normal object
					p.getObjectFields().forEach(c -> collectDefinitions(c, defs));
				}
			} else if (p.getType() == Type.ARRAY && p.getObjectType() != null) {
				collectDefinitions(p.getObjectType(), defs);
			}
		}

		private void writeObject(JsonGenerator gen, ToolParameter p, boolean isRoot) throws IOException {

			switch (p.getType()) {
			case STRING:
				gen.writeStringField("type", "string");
				break;
			case INTEGER:
				gen.writeStringField("type", "integer");
				break;
			case DOUBLE:
				gen.writeStringField("type", "number");
				break;
			case BOOLEAN:
				gen.writeStringField("type", "boolean");
				break;

			case ENUMERATION:
				gen.writeStringField("type", "string");
				gen.writeArrayFieldStart("enum");
				for (String v : p.getEnumValues())
					gen.writeString(v);
				gen.writeEndArray();
				break;

			case ARRAY:
				gen.writeStringField("type", "array");
				gen.writeFieldName("items");
				gen.writeStartObject();
				writeObject(gen, p.getObjectType(), false);
				gen.writeEndObject();
				break;

			case OBJECT:
				// if it's a reference, use $ref
				if (p.getObjectType() != null && p.getObjectFields().isEmpty()) {
					gen.writeStringField("$ref", "#/definitions/" + p.getObjectType().getName());
					break;
				}

				gen.writeStringField("type", "object");
				gen.writeBooleanField("additionalProperties", false);
				if (isRoot && p.getDescription() != null)
					// trick to have the schema look like that SCHEMA_GENERATOR creates (easier
					// testing)
					gen.writeStringField("description", p.getDescription());
				// properties
				gen.writeObjectFieldStart("properties");
				for (ToolParameter c : p.getObjectFields()) {
					gen.writeFieldName(c.getName());
					gen.writeStartObject();
					writeObject(gen, c, false);
					gen.writeEndObject();
				}
				gen.writeEndObject(); // properties
				// required
				List<String> req = p.getObjectFields().stream().filter(ToolParameter::isRequired)
						.map(ToolParameter::getName).collect(Collectors.toList());
				if (!req.isEmpty()) {
					gen.writeArrayFieldStart("required");
					for (String r : req)
						gen.writeString(r);
					gen.writeEndArray();
				}
				break;
			}

			if (!isRoot && p.getDescription() != null)
				// trick to have the schema look like that SCHEMA_GENERATOR creates (easier
				// testing)
				gen.writeStringField("description", p.getDescription());
		}
	}

	/** Used for creating JSON schema out of classes */
	private final static JsonSchemaGenerator SCHEMA_GENERATOR = new JsonSchemaGenerator(new ObjectMapper());

	/**
	 * Mapper provided for JSON serialisation via Jackson, if needed.
	 * Use this to deserialized objects that have been created through some schema.
	 */
	public static final ObjectMapper JSON_MAPPER;
	static {
		JSON_MAPPER = new ObjectMapper();
		JSON_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		JSON_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
//		JSON_MAPPER.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
	}

	private String schema = "http://json-schema.org/draft-04/schema#";
	private String title = null;

	/** The first-level object of the schema (always type OBJECT). */
	@NonNull
	private ToolParameter root;

	/**
	 * Constructs an instance using the JSON schema for given class.
	 * 
	 * See also {@link #getParameters(Class)}.
	 */
	public static JsonSchema fromSchema(Class<?> c) {
		try {
			return fromSchema(JSON_MAPPER.writeValueAsString(SCHEMA_GENERATOR.generateJsonSchema(c)));
		} catch (JsonProcessingException e) {
			// This should never happen as serialization is no problem and we are
			// de-serializing
			// TODO see if we need more guards in de-serialization
			LOG.error("Error de-serializing JSON schema", e);
			return null;
		}
	}

	/**
	 * Constructs an instance from a JSON schema.
	 * 
	 * See also {@link #getParameters(Class)}.
	 * 
	 * @throws JsonProcessingException
	 */
	public static JsonSchema fromSchema(String jsonSchema) throws JsonProcessingException {
		return JSON_MAPPER.readValue(jsonSchema, JsonSchema.class);
	}

	/**
	 * Constructs JSON schema describing a list of {@link ToolParameter}s.
	 */
	public JsonSchema(List<? extends ToolParameter> parameters) {
		this(null, null, parameters);
	}

	/**
	 * Constructs JSON schema describing a list of {@link ToolParameter}s.
	 * 
	 * @param title Title to use for schema.
	 */
	public JsonSchema(String title, @NonNull List<? extends ToolParameter> parameters) {
		this(title, null, parameters);
	}

	/**
	 * Constructs JSON schema describing a list of {@link ToolParameter}s.
	 * 
	 * @param title       Title to use for schema.
	 * @param description Description for this schema.
	 */
	public JsonSchema(String title, String description, @NonNull List<? extends ToolParameter> parameters) {
		this.title = title;
		this.root = buildRoot(description, parameters);
	}

	private static ToolParameter buildRoot(String description, List<? extends ToolParameter> parameters) {
		ToolParameter r = new ToolParameter();
		r.setType(ToolParameter.Type.OBJECT);
		r.setDescription(description);
		r.getObjectFields().addAll(parameters);
		return r;
	}

	/**
	 * @return This schema description.
	 */
	public String getDescription() {
		return root.getDescription();
	}

	/**
	 * @return This schema as a list of {@link ToolParameter}s.
	 */
	public List<ToolParameter> asParameters() {
		return root.getObjectFields();
	}

	/**
	 * @return This schema as pretty JSON string.
	 */
	public String asJsonSchema() {
		try {
			return JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this);
		} catch (JsonProcessingException e) {
			// Should never ever happen as we are doing the serialization
			return null;
		}
	}

	/**
	 * Converts the JSON Schema represented by this instance into a Map that maps
	 * each schema property into its value.
	 * 
	 * This is a helper to create model objects for other libraries (namely OpenAI
	 * SDK).
	 * 
	 * @param string If true uses strict mode (see below).
	 * 
	 * @see <a href=
	 *      "https://platform.openai.com/docs/guides/function-calling?api-mode=chat#additional-configurations">strict
	 *      mode</a>
	 */
	public Map<String, Object> asMap(boolean strict) {

		Map<String, Object> schema = new LinkedHashMap<>();
		if (getSchema() != null)
			schema.put("$schema", getSchema());
		if (getTitle() != null)
			schema.put("title", getTitle());

		// Convert root object which recursively will convert all other objects
		schema.putAll(toSchema(getRoot(), strict, true));

		// Collect all referenced objects
		Map<String, Object> definitions = new HashMap<>();
		Map<String, ToolParameter> refs = new HashMap<>();
		collectDefinitions(getRoot(), refs);
		refs.entrySet()
				.forEach(e -> definitions.put(e.getKey(), JsonValue.from(toSchema(e.getValue(), strict, false))));
		if (!definitions.isEmpty()) {
			schema.put("definitions", definitions);
		}

		return schema;
	}

	/** Converts a single ToolParameter into a JSON-schema fragment. */
	private static Map<String, Object> toSchema(ToolParameter p, boolean strict, boolean isRoot) {

		Map<String, Object> schema = new LinkedHashMap<>();

		if (p.getType() == ToolParameter.Type.OBJECT && p.getObjectType() != null && p.getObjectFields().isEmpty()) {
			schema.put("$ref", "#/definitions/" + p.getObjectType().getName());
		} else {

			switch (p.getType()) {
			case STRING:
				schema.put("type", "string");
				break;
			case INTEGER:
				schema.put("type", "integer");
				break;
			case DOUBLE:
				schema.put("type", "number");
				break;
			case BOOLEAN:
				schema.put("type", "boolean");
				break;
			case ENUMERATION:
				schema.put("type", "string");
				schema.put("enum", p.getEnumValues());
				break;
			case ARRAY:
				schema.put("type", "array");
				schema.put("items", toSchema(p.getObjectType(), strict, false));
				break;
			case OBJECT:
				Map<String, Object> props = new LinkedHashMap<>();
				List<String> req = new ArrayList<>();
				for (ToolParameter child : p.getObjectFields()) {
					props.put(child.getName(), toSchema(child, strict, false));
					if (strict || child.isRequired()) {
						req.add(child.getName());
					}
				}
				schema.put("type", "object");
				schema.put("properties", props);
				if (!req.isEmpty()) {
					schema.put("required", req);
				}
				schema.put("additionalProperties", false);
				break;
			default:
				throw new IllegalArgumentException("Unsupported type: " + p.getType());
			}

			if (p.getDescription() != null && !p.getDescription().isEmpty()) {
				schema.put("description", p.getDescription());
			}
		} // if not a $ref

		if (strict && !isRoot) // top object must be object, not [object, null]
			return makeOptional(schema);
		return schema;
	}

	/** Makes a field optional by adding "null" to its type list. */
	private static Map<String, Object> makeOptional(Map<String, Object> schema) {

		Map<String, Object> copy = new LinkedHashMap<>(schema);
		Object typeVal = copy.get("type");

		if (typeVal instanceof String) {
			copy.put("type", List.of(typeVal, "null"));
		} else if (typeVal instanceof List) {
			@SuppressWarnings("unchecked")
			List<Object> list = new ArrayList<>((List<Object>) typeVal);
			if (!list.contains("null"))
				list.add("null");
			copy.put("type", list);
		}
		return copy;
	}

	private static void collectDefinitions(ToolParameter p, Map<String, ToolParameter> defs) {

		if (p.getType() == Type.OBJECT) {
			// $ref ?
			if (p.getObjectType() != null && p.getObjectFields().isEmpty()) {
				ToolParameter ref = p.getObjectType();
				defs.put(ref.getName(), ref);
			} else { // normal object
				p.getObjectFields().forEach(c -> collectDefinitions(c, defs));
			}
		} else if (p.getType() == Type.ARRAY && p.getObjectType() != null) {
			collectDefinitions(p.getObjectType(), defs);
		}
	}

	/**
	 * 
	 * @param jsonSchema
	 * @return Parameters described by given JSON schema.
	 * @throws JsonProcessingException
	 */
	public static List<ToolParameter> getParameters(String jsonSchema) throws JsonProcessingException {
		return fromSchema(jsonSchema).asParameters();
	}

	/**
	 * Converts a class which schema describes parameters for a {@link Tool} into a
	 * list of {@link ToolParameters}.
	 * 
	 * Developers can use Jackson annotations in the class definition describing the
	 * parameters for the tool, then use this method to create corresponding list of
	 * {@link ToolParameter}s. Supported annotations: {@link JsonSchemaDescription},
	 * {@link JsonPropertyDescription}, {@link JsonProperty} (required = true) to
	 * mark mandatory parameters, and {@link JsonIgnore} to skip parameters. Use
	 * {@link JsonAutoDetect @JsonAutoDetect(fieldVisibility =
	 * JsonAutoDetect.Visibility.ANY)} to have private fields added to the schema as
	 * well.
	 * 
	 * See {@link io.github.mzattera.predictivepowers.examples.FunctionCallExample}
	 * for examples.
	 * 
	 * @see <a href=
	 *      "https://platform.openai.com/docs/guides/function-calling?api-mode=chat">function
	 *      calling guide</a>
	 */
	public static List<ToolParameter> getParameters(Class<?> c) {
		return fromSchema(c).asParameters();
	}

	/**
	 * @return A representation of given tool parameters as JSON schema.
	 */
	public static String getJsonSchema(@NonNull List<? extends ToolParameter> parameters) {
		return new JsonSchema(parameters).asJsonSchema();
	}

	/**
	 * This uses a Class as a template to define tool parameters.
	 * 
	 * See {@link #getParameters(Class)}.
	 * 
	 * @return A representation of parameters as JSON schema.
	 */
	public static String getJsonSchema(Class<?> c) {
		return fromSchema(c).asJsonSchema();
	}

}