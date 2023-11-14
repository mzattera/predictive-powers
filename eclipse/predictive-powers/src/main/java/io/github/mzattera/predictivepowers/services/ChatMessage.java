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
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A single message in a chat with an agent.
 * 
 * @author Massmiliano "Maxi" Zattera.
 *
 */
@Getter
@Setter
@Builder
//@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
@ToString
public class ChatMessage {

	/**
	 * Set of fixed roles for chats, to allow easier interoperability (hopefully).
	 */
	public enum Role {
		/** Marks messages coming from the user */
		USER("user"),

		/** Marks messages coming from the bot (assistant) */
		BOT("assistant"),

		/**
		 * Marks text used for bot configuration (e.g. in OpenAI ChatGPT). It might not
		 * be supported by all services.
		 */
		SYSTEM("system"),

		/**
		 * Marks text returned by a function. OpenAI API.
		 */
		FUNCTION("function");

		private final String label;

		private Role(String label) {
			this.label = label;
		}

		@Override
		@JsonValue
		public String toString() { // Notice we rely on labels not to change
			return label;
		}
	}

	Role role;

	/**
	 * Message content, can be null if a function call is returned instead.
	 */
	@JsonInclude(JsonInclude.Include.ALWAYS) // Needed for OpenAI function call API or it will throw HTTP 400 for
												// function calls messages
	String content;
}
