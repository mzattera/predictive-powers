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
package io.github.mzattera.predictivepowers.services.messages;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonValue;

import io.github.mzattera.predictivepowers.services.Agent;
import io.github.mzattera.predictivepowers.services.ChatService;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * This is a message exchanged with a {@link ChatService} or an {@link Agent}.
 * 
 * Messages are formed by a list of {@link MessagePart}s; each part can provide
 * different contents (e.g. some text, a file for an image, a tool invocation
 * and its results, etc.).
 * 
 * @author Massmiliano "Maxi" Zattera.
 *
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
@Getter
@Setter
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

	@NonNull
	private Author author;

	@NonNull
	private List<MessagePart> parts = new ArrayList<>();

	public ChatMessage(@NonNull Author author, String content) {
		this(author);
		if (content != null)
			parts.add(new TextPart(content));
	}

	public ChatMessage(@NonNull Author author, @NonNull List<? extends MessagePart> parts) {
		this(author);
		this.parts.addAll(parts);
	}

	/**
	 * 
	 * @return A string representation of the content of this message. Notice the
	 *         message could contain parts which are not easily representable as
	 *         text (e.g. a file).
	 */
	public String getContent() {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < parts.size(); ++i) {
			if (i > 0)
				result.append("\n\n");
			result.append(parts.get(i).getContent());
		}
		return result.toString(); // TODO lazy caching
	}
}
