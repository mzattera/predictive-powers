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
package io.github.mzattera.predictivepowers.openai.services;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import io.github.mzattera.predictivepowers.services.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * This extends {@link ChatMessage} with fields to support OpenAI chat
 * completion, in particular function calling.
 * 
 * @author Massmiliano "Maxi" Zattera.
 *
 */
@Getter
@Setter
@NoArgsConstructor
//@RequiredArgsConstructor
//@AllArgsConstructor
@ToString
public class OpenAiChatMessage extends ChatMessage {

	/**
	 * Function call that a completion model might return.
	 * 
	 * @author Massimiliano "Maxi" Zattera
	 *
	 */
	@Getter
	@Setter
	@Builder
	@NoArgsConstructor
	@RequiredArgsConstructor
	@AllArgsConstructor
	@ToString
	public static class FunctionCall {

		/**
		 * Custom serializer as Arguments are returned as String, we'd like to have them
		 * as a Map....
		 */
		private static final class ArgumentsSerializer extends StdSerializer<Map<String, Object>> {

			private static final long serialVersionUID = 2127829900119652867L;

			@SuppressWarnings("unused")
			public ArgumentsSerializer() {
				this(null);
			}

			public ArgumentsSerializer(Class<Map<String, Object>> t) {
				super(t);
			}

			@Override
			public void serialize(Map<String, Object> value, JsonGenerator jgen, SerializerProvider provider)
					throws IOException, JsonProcessingException {

				ObjectMapper mapper = (ObjectMapper) jgen.getCodec();
				jgen.writeString(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value));
			}
		}

		/**
		 * Custom de-serializer as arguments are returned as String, we'd like to have
		 * them as a Map....
		 */
		private static final class ArgumentsDeserializer extends JsonDeserializer<Map<String, Object>> {

			@Override
			public Map<String, Object> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
					throws IOException, JsonProcessingException {

				String json = jsonParser.getText();
				ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
				return mapper.readValue(json, new TypeReference<Map<String, Object>>() {
				});
			}
		}

		/**
		 * Name of the function to call.
		 */
		@NonNull
		String name;

		/**
		 * Parameters to use in the call, as name/value pairs.
		 */
		@JsonSerialize(using = ArgumentsSerializer.class)
		@JsonDeserialize(using = ArgumentsDeserializer.class)
		Map<String, Object> arguments;
	}

	/**
	 * The name of the author of this message.
	 * 
	 * For OpenAI API, name is required if role is FUNCTION, and it should be the
	 * name of the function whose response is in the content. May contain a-z, A-Z,
	 * 0-9, and underscores, with a maximum length of 64 characters.
	 */
	String name;

	/**
	 * This will contain generated function call, if any. For the time being, this
	 * is supported only by OpenAI API.
	 */
	FunctionCall functionCall;

	public boolean isFunctionCall() {
		return (functionCall != null);
	}

	public OpenAiChatMessage(Role role, String content) {
		this(role, content, null, null);
	}

	public OpenAiChatMessage(Role role, String content, String name) {
		this(role, content, name, null);
	}

	@Builder(builderMethodName = "myBuilder")
	public OpenAiChatMessage(Role role, String content, String name, FunctionCall functionCall) {
		super(role, content);
		this.name = name;
		this.functionCall = functionCall;
	}
}
