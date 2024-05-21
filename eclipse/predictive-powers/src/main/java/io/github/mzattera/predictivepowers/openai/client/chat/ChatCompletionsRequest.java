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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

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

/**
 * Parameters for a request to /chat/completions API.
 * 
 * @author Massmiliano "Maxi" Zattera.
 *
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class ChatCompletionsRequest {

	/**
	 * Provides custom serialization.
	 */
	private final static class ResponseFormatSerializer extends StdSerializer<ResponseFormat> {

		private static final long serialVersionUID = -4506958348962250647L;

		@SuppressWarnings("unused")
		public ResponseFormatSerializer() {
			this(null);
		}

		public ResponseFormatSerializer(Class<ResponseFormat> t) {
			super(t);
		}

		@Override
		public void serialize(ResponseFormat value, JsonGenerator jgen, SerializerProvider provider)
				throws IOException, JsonProcessingException {

			jgen.writeStartObject();
			jgen.writeStringField("type", value.label);
			jgen.writeEndObject();
		}
	}

	/**
	 * Format of the returned answer. Currently supports JSON and plain text.
	 */
	@JsonSerialize(using = ResponseFormatSerializer.class)
	public enum ResponseFormat {
		TEXT("text"), JSON("json_object");

		private final String label;

		ResponseFormat(String label) {
			this.label = label;
		}
	}

	@NonNull
	@Builder.Default
	private List<OpenAiChatMessage> messages = new ArrayList<>();

	@NonNull
	private String model;

	Double frequencyPenalty;
	private Map<String, Integer> logitBias;
	
	private Boolean logprobs;
	private Integer topLogprobs;
	
	/**
	 * Higher-level functions in the library will try to calculate this
	 * automatically if it is null when submitting a request.
	 */
	private Integer maxTokens;

	private Integer n;
	private Double presencePenalty;
	private ResponseFormat responseFormat;
	private Integer seed;
	private List<String> stop;

	// TODO: Add support for streaming input at least in direct API calls, if so
	// make sure services do not stream
	private final boolean stream = false;

	private Double temperature;
	private Double topP;

	private List<OpenAiTool> tools;
	private ToolChoice toolChoice;

	private String user;

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
