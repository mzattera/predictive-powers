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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * A single message in a chat with an agent.
 * 
 * @author Massmiliano "Maxi" Zattera.
 *
 */
@Getter
@SuperBuilder
//@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
@ToString
public class ChatMessage {

	/**
	 * The author (originator) of the message.
	 */
	public enum Author {
		/** Marks messages coming from the user */
		USER("user"),

		/** Marks messages coming from the bot/agent/assistant */
		BOT("bot");
		
		private final String label;

		private Author(String label) {
			this.label = label;
		}

		@Override
		@JsonValue
		public String toString() { // Notice we rely on labels not to change
			return label;
		}
	}

	Author author;

	/**
	 * Message content, can be null if a function call is returned instead.
	 */
	// TODO Move to OpenAITextMessage
	@JsonInclude(JsonInclude.Include.ALWAYS) // Needed for OpenAI function call API or it will throw HTTP 400 for
												// function calls messages
	String content;
}
