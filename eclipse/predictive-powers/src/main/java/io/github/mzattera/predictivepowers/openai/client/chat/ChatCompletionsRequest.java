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
package io.github.mzattera.predictivepowers.openai.client.chat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import io.github.mzattera.predictivepowers.openai.client.chat.StaticContent.ContentPart;
import io.github.mzattera.predictivepowers.openai.services.AudioFormat;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatMessage;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Parameters for a request to /chat/completions API.
 * 
 * @author Massmiliano "Maxi" Zattera.
 *
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Getter
@Setter
@ToString
public class ChatCompletionsRequest {

	public enum Modality {
		TEXT("text"), AUDIO("audio");

		private final String label;

		Modality(String label) {
			this.label = label;
		}

		@JsonValue
		@Override
		public String toString() {
			return label;
		}
	}

	public enum Effort {
		LOW("low"), MEDIUM("medium"), HIGH("high");

		private final String label;

		Effort(String label) {
			this.label = label;
		}

		@JsonValue
		@Override
		public String toString() {
			return label;
		}
	}

	public enum ServiceTier {
		AUTO("auto"), DEFAULT("default");

		private final String label;

		ServiceTier(String label) {
			this.label = label;
		}

		@JsonValue
		@Override
		public String toString() {
			return label;
		}
	}

	// To serialize static content
	private final static class StaticContentSerializer extends StdSerializer<StaticContent> {

		private static final long serialVersionUID = 1L;

		@SuppressWarnings("unused")
		public StaticContentSerializer() {
			this(null);
		}

		public StaticContentSerializer(Class<StaticContent> t) {
			super(t);
		}

		@Override
		public void serialize(StaticContent value, JsonGenerator gen, SerializerProvider serializers)
				throws IOException, JsonProcessingException {

			if (value.getContent() != null) { // Serialize as a single string
				gen.writeString(value.getContent());
			} else {
				gen.writeStartArray();
				if (value.getContents() != null) { // paranoid, better write an empty array if all is null
					for (ContentPart part : value.getContents()) {
						gen.writeStartObject();
						gen.writeStringField("text", part.getText());
						gen.writeStringField("type", part.getType());
						gen.writeEndObject();
					}
				}
				gen.writeEndArray();
			}
		}
	}

	// To de-serialize static content
	private final static class StaticContentDeserializer extends JsonDeserializer<StaticContent> {

		@Override
		public StaticContent deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

			StaticContent content = new StaticContent();

			JsonToken currentToken = p.currentToken();
			if (currentToken == JsonToken.VALUE_STRING) {
				content.setContent(p.getText());
			} else if (currentToken == JsonToken.START_ARRAY) {
				List<ContentPart> parts = p.readValueAs(new TypeReference<List<ContentPart>>() {
				});
				content.setContents(parts);
			} else {
				throw new JsonMappingException(p, "Unsupported content format");
			}

			return content;
		}

		@Override
		public StaticContent getNullValue(DeserializationContext ctxt) {
			return null;
		}
	}

	// To serialize static content
	private final static class ModalitiesSerializer extends StdSerializer<List<Modality>> {

		private static final long serialVersionUID = 1L;

		@SuppressWarnings("unused")
		public ModalitiesSerializer() {
			this(null);
		}

		public ModalitiesSerializer(Class<List<Modality>> t) {
			super(t);
		}

		@Override
		public void serialize(List<Modality> value, JsonGenerator gen, SerializerProvider serializers)
				throws IOException, JsonProcessingException {

			if ((value == null) || (value.size() == 0)) {
				// API will error if we serialize an empty list
				gen.writeNull();
			} else {
				gen.writeStartArray();
				for (Modality m : value) {
					// Serialize modality using the default serialization for it
					serializers.defaultSerializeValue(m, gen);
				}
				gen.writeEndArray();
			}
		}
	}

	@NonNull
	@Builder.Default
	private List<OpenAiChatMessage> messages = new ArrayList<>();

	@NonNull
	private String model;

	private AudioFormat audio;

	Double frequencyPenalty;
	private Map<String, Integer> logitBias;

	private Boolean logprobs;
	private Integer topLogprobs;

	/**
	 * Deprecated, replaced by {@link #maxCompletionTokens}.
	 */
	private Integer maxTokens;

	/**
	 * Maximum number of tokens to generate. If this is left to null, higher-level
	 * functions in the library will try to fix this to best value.
	 */
	private Integer maxCompletionTokens;

	// This must be null, unless "store" is specified
	private Map<String, String> metadata;

	@Builder.Default
	@JsonSerialize(using = ModalitiesSerializer.class)
	private List<Modality> modalities = new ArrayList<>();

	private Integer n;
	private Boolean parallelToolCalls;

	@JsonSerialize(using = StaticContentSerializer.class)
	@JsonDeserialize(using = StaticContentDeserializer.class)
	private StaticContent prediction;

	private Double presencePenalty;
	private Effort reasoningEffort;
	private ResponseFormat responseFormat;
	private Integer seed;
	private ServiceTier serviceTier;
	private List<String> stop;
	private Boolean store;

	// TODO: Add support for streaming input at least in direct API calls, if so
	// make sure services do not stream
	private final boolean stream = false;
	private final String streamOptions = null;

	private Double temperature;
	private ToolChoice toolChoice;
	private List<OpenAiTool> tools;
	private Double topP;
	private String user;
	private WebSearchOptions webSearchOptions;

	/**
	 * Notice: Setting this without providing {@link #functions} causes an HTTP 400
	 * error. See {@link FunctionChoice}.
	 */
	private FunctionChoice functionCall;

	/**
	 * Notice: HTTP 400 error is generated if this is an empty list.
	 */
	private List<Function> functions;
}
