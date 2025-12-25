/*
 * Copyright 2023-2025 Massimiliano "Maxi" Zattera
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
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;

import io.github.mzattera.predictivepowers.services.Agent;
import io.github.mzattera.predictivepowers.services.ChatService;
import io.github.mzattera.predictivepowers.services.messages.FilePart.ContentType;
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

	// TODO Eventually add parts to support citations/annotations or find a6 way to expose original history so to get access to messages...

	/**
	 * The author (originator) of the message.
	 */
	public enum Author {

		/** Marks messages coming from the user */
		USER("user"),

		/** Marks messages coming from the bot/agent/assistant */
		BOT("bot"),

		/**
		 * Marks messages coming from the developer. For some models, this is used to
		 * provide instructions to the model
		 */
		DEVELOPER("developer");

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

	/**
	 * In some cases, agents might refuse to generate an answer and provide a reason for the refusal.
	 * This is optional and can be null.
	 */
	private @Nullable String refusal;

	/**
	 * If agent provided some reasoning for how this message was generated, it is contained here.
	 */
	private @Nullable String reasoning;
	
	@NonNull
	private List<MessagePart> parts = new ArrayList<>();

	public void setParts(List<? extends MessagePart> parts) {
		this.parts.addAll(parts);
	}

	public ChatMessage(String content) {
		this(Author.USER, content);
	}

	public ChatMessage(@NonNull MessagePart part) {
		this(Author.USER, part);
	}

	public ChatMessage(@NonNull List<? extends MessagePart> parts) {
		this(Author.USER, parts);
	}

	public ChatMessage(@NonNull Author author, String content) {
		this.author = author;
		if (content != null)
			parts.add(new TextPart(content));
	}

	public ChatMessage(@NonNull Author author, @NonNull MessagePart part) {
		this.author = author;
		this.parts.add(part);
	}

	public ChatMessage(@NonNull Author author, @NonNull List<? extends MessagePart> parts) {
		this.author = author;
		this.parts.addAll(parts);
	}

	/**
	 * Adds given part to the end of this message.
	 * 
	 * @param part
	 */
	public void addPart(@NonNull MessagePart part) {
		parts.add(part);
	}

	/**
	 * 
	 * @return True if and only if this message is pure text.
	 */
	public boolean isText() {
		for (MessagePart part : parts)
			if (!(part instanceof TextPart))
				return false;
		return true;
	}

	/**
	 * 
	 * @return True if this message contains at least one part which is text.
	 */
	public boolean hasText() {
		for (MessagePart part : parts)
			if (part instanceof TextPart)
				return true;
		return false;
	}

	/**
	 * 
	 * @return A string representation of the content of this message. Notice the
	 *         message could contain parts which are not easily representable as
	 *         text (e.g. a file). See {@link #isText()}.
	 */
	public String getTextContent() {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < parts.size(); ++i) {
			if (i > 0)
				result.append("\n\n");
			result.append(parts.get(i).getContent());
		}
		return result.toString();
	}

	/**
	 * 
	 * @return The content of this message as an instance of given class. This
	 *         assumes {@link #getTextContent()} will return a properly formatted
	 *         JSON representation of the object.
	 * 
	 * @throws JsonProcessingException If an error occurs while parsing the message
	 *                                 content.
	 */
	public <T> T getObjectContent(Class<T> c) throws JsonProcessingException {
		return JsonSchema.JSON_MAPPER.readValue(getTextContent(), c);
	}

	/**
	 * 
	 * @return True if this message contains at least one part which is a file of
	 *         the given {@link FilePart.ContentType}.
	 */
	public boolean hasFileContent(ContentType type) {
		for (MessagePart part : parts)
			if ((part instanceof FilePart) && (((FilePart) part).getContentType() == type))
				return true;
		return false;
	}

	/**
	 * 
	 * @return All files of the given {@link FilePart.ContentType} contained in this
	 *         message.
	 */
	public List<FilePart> getFileContent(ContentType type) {
		return parts.stream() //
				.filter(FilePart.class::isInstance) //
				.map(FilePart.class::cast) //
				.filter(f -> f.getContentType() == type).collect(Collectors.toList());
	}

	/**
	 * 
	 * @return True if this message contains at least one invocation of a tool.
	 */
	public boolean hasToolCalls() {
		for (MessagePart part : parts)
			if (part instanceof ToolCall)
				return true;
		return false;
	}

	/**
	 * 
	 * @return All tool invocations contained in this message.
	 */
	public List<ToolCall> getToolCalls() {
		return parts.stream() //
				.filter(ToolCall.class::isInstance) //
				.map(ToolCall.class::cast) //
				.collect(Collectors.toList());
	}

	/**
	 * 
	 * @return True if this message contains at least one tool call response.
	 */
	public boolean hasToolCallResults() {
		for (MessagePart part : parts)
			if (part instanceof ToolCallResult)
				return true;
		return false;
	}

	/**
	 * 
	 * @return All tool invocations contained in this message.
	 */
	public List<ToolCallResult> getToolCallResults() {
		return parts.stream() //
				.filter(ToolCallResult.class::isInstance) //
				.map(ToolCallResult.class::cast) //
				.collect(Collectors.toList());
	}
}
