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
package io.github.mzattera.predictivepowers.openai.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import io.github.mzattera.predictivepowers.openai.client.chat.FunctionCall;
import io.github.mzattera.predictivepowers.openai.client.chat.OpenAiToolCall;
import io.github.mzattera.predictivepowers.openai.client.chat.RefusalPart;
import io.github.mzattera.predictivepowers.services.messages.Base64FilePart;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage.Author;
import io.github.mzattera.predictivepowers.services.messages.FilePart;
import io.github.mzattera.predictivepowers.services.messages.MessagePart;
import io.github.mzattera.predictivepowers.services.messages.TextPart;
import io.github.mzattera.predictivepowers.services.messages.ToolCallResult;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * This extends {@link ChatMessage} with fields to support OpenAI chat API.
 * 
 * @author Massmiliano "Maxi" Zattera.
 *
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
@ToString
public class OpenAiChatMessage {

	// This serializes getContentParts() as a list of text messages and image URLS,
	// to
	// support vision models.
	private final static class MessagePartSerializer extends StdSerializer<List<MessagePart>> {

		private static final long serialVersionUID = 1L;

		@SuppressWarnings("unused")
		public MessagePartSerializer() {
			this(null);
		}

		public MessagePartSerializer(Class<List<MessagePart>> t) {
			super(t);
		}

		@Override
		public void serialize(List<MessagePart> value, JsonGenerator gen, SerializerProvider serializers)
				throws IOException, JsonProcessingException {

			if ((value == null) || (value.size() == 0)) {
				// We must always serialize the field or some models will error in case of
				// function calls
				gen.writeNull();
			} else if ((value.size() == 1) && (value.get(0) instanceof TextPart)) {
				// for all non-vision model, content is just a string
				gen.writeString(value.get(0).getContent());
			} else {
				gen.writeStartArray();
				for (MessagePart part : value) {
					gen.writeStartObject();

					if (part instanceof RefusalPart) { // This is a refusal message
						gen.writeStringField("type", "refusal");
						gen.writeStringField("refusal", ((RefusalPart) part).getContent());
					} else if (part instanceof TextPart) { // Simple text part
						gen.writeStringField("type", "text");
						gen.writeStringField("text", ((TextPart) part).getContent());
					} else if (part instanceof OpenAiFilePart) { // This part is a file uploaded with file API
						gen.writeStringField("type", "file");
						gen.writeObjectFieldStart("file");
						gen.writeStringField("file_id", ((OpenAiFilePart) part).getFileId());
						gen.writeEndObject();
					} else if (part instanceof FilePart) { // Handles all files

						FilePart file = (FilePart) part;

						switch (file.getContentType()) {
						case IMAGE: // Encode Image
							gen.writeStringField("type", "image_url");
							gen.writeObjectFieldStart("image_url");
							if (file.getUrl() != null)
								gen.writeStringField("url", file.getUrl().toString());
							else {// base64 encode
								if (file instanceof Base64FilePart)

									// TODO URGENT Must the image be encoded as JPEG?
									// https://platform.openai.com/docs/guides/images?api-mode=responses&format=base64-encoded

									gen.writeStringField("url",
											"data:image/jpeg;base64," + ((Base64FilePart) file).getEncodedContent());
								else
									gen.writeStringField("url", "data:image/jpeg;base64,"
											+ Base64.getEncoder().encodeToString(file.getInputStream().readAllBytes()));
							}
							gen.writeEndObject();
							break;
						case AUDIO: // Encode Audio
							gen.writeStringField("type", "input_audio");
							gen.writeObjectFieldStart("input_audio");
							if (file instanceof Base64FilePart)
								gen.writeStringField("data", ((Base64FilePart) file).getEncodedContent());
							else
								gen.writeStringField("data",
										Base64.getEncoder().encodeToString(file.getInputStream().readAllBytes()));
							gen.writeStringField("format", file.getFormat());
							gen.writeEndObject();
							break;
						default: // All other file types
							gen.writeStringField("type", "file");
							gen.writeObjectFieldStart("file");
							if (file instanceof Base64FilePart)
								gen.writeStringField("file_data", ((Base64FilePart) file).getEncodedContent());
							else
								gen.writeStringField("file_data",
										Base64.getEncoder().encodeToString(file.getInputStream().readAllBytes()));
							if (file.isLocalFile())
								gen.writeStringField("filename", file.getFile().getName());
							gen.writeEndObject();
							break;
						}
					} else { // Unsupported message part
						throw new IllegalArgumentException("Unsupported message part: " + part.getClass().getName());
					}

					gen.writeEndObject();
				} // for each message part
				gen.writeEndArray();
			} // Multi-part message
		}
	}

	// This de-serializes getParts(). We always do this as getContent() is backed up
	// by getParts()
	private final static class MessagePartDeserializer extends JsonDeserializer<List<MessagePart>> {

		@Override
		public List<MessagePart> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
			JsonNode node = p.getCodec().readTree(p);
			List<MessagePart> parts = new ArrayList<>();
			if (node.isTextual()) {
				parts.add(new TextPart(node.asText()));
			} else {
				// API should always return null or a single String
				throw new JsonMappingException(p, "Returned message is not a string");
			}
			return parts;
		}

		@Override
		public List<MessagePart> getNullValue(DeserializationContext ctxt) {
			return new ArrayList<>(); // Return empty list on null
		}
	}

	/**
	 * The originator of the message.
	 */
	public static enum Role {

		/** Marks messages coming from the user */
		USER("user"),

		/** Marks messages coming from the API */
		ASSISTANT("assistant"),

		/**
		 * Marks text used for bot configuration (instructions).
		 */
		SYSTEM("system"),

		/**
		 * Marks text used for bot configuration (instructions) for newer models. Not
		 * this is interchangeable with SYSTEM.
		 */
		DEVELOPER("developer"),

		/**
		 * The message was generated by a function that was called by OpenAI function
		 * call API.
		 */
		FUNCTION("function"),

		/**
		 * The message was generated by a tool that was called by OpenAI function call
		 * API.
		 */
		TOOL("tool");

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

	private static Role authorToRole(Author author) {
		switch (author) {
		case USER:
			return Role.USER;
		case BOT:
			return Role.ASSISTANT;
		default:
			throw new IllegalArgumentException("Unsupported author: " + author); // Guard
		}
	}

	/** The role of the messages author */
	@NonNull
	private Role role;

	/**
	 * The name of the author of this message.
	 * 
	 * For OpenAI API, name is required if role is FUNCTION or TOOL, and it should
	 * be the name of the function whose response is in the content. May contain
	 * a-z, A-Z, 0-9, and underscores, with a maximum length of 64 characters.
	 */
	private String name;

	/**
	 * Message content, can be null if a function call is returned instead.
	 * 
	 * Notice that, to support view models, this can be an array.
	 * 
	 * When a message is returned by the API, this is always a single string value
	 * that can be read with {@link getContent()}. When calling the API, you can use
	 * {@link setContent()} to set this field to a single string value, or use
	 * {@link getContentParts()} to provide an array of strings and images (as
	 * {@link FilePart}s). Notice that {@link getContent()} will throw an
	 * IllegalArgumentException if the message is not a single-part message that
	 * contains only text. On the other side, {@link getContentParts()} will always
	 * return a list of message parts, eventually empty.
	 */
	@JsonIgnore
	public String getContent() {
		if ((contentParts.size() == 1) && (contentParts.get(0) instanceof TextPart))
			return ((TextPart) contentParts.get(0)).getContent();
		throw new IllegalArgumentException("This is a multipart message");
	}

	/**
	 * Message content, can be null if a function call is returned instead.
	 * 
	 * Notice that, to support view models, this can be an array.
	 * 
	 * When a message is returned by the API, this is always a single string value
	 * that can be read with {@link getContent()}. When calling the API, you can use
	 * {@link setContent()} to set this field to a single string value, or use
	 * {@link getContentParts()} to provide an array of strings and images (as
	 * {@link FilePart}s). Notice that {@link getContent()} will return null if the
	 * message is not a single-part message that contains only text. On the other
	 * side, {@link getContentParts()} will always return a list of message parts,
	 * eventually empty.
	 */
	@JsonIgnore
	public void setContent(String content) {
		contentParts.clear();
		if (content != null)
			contentParts.add(new TextPart(content));
	}

	private List<MessagePart> contentParts = new ArrayList<>();

	/**
	 * Message content, eventually empty (e.g. if a function call is returned
	 * instead).
	 * 
	 * This is meant to support view models.
	 * 
	 * When a message is returned by the API, this is always a single string value
	 * that can be read with {@link getContent()}. When calling the API, you can use
	 * {@link setContent()} to set "content" field to a single string value, or use
	 * {@link getContentParts()} to provide an array of strings and images (as
	 * {@link FilePart}s). Notice that {@link getContent()} will return null if the
	 * message is not a single-part message that contains only text. On the other
	 * side, {@link getContentParts()} will always return a list of message parts,
	 * eventually empty.
	 */
	@JsonProperty("content")
	@JsonSerialize(using = MessagePartSerializer.class)
	@JsonDeserialize(using = MessagePartDeserializer.class)
	@JsonInclude(JsonInclude.Include.ALWAYS) // Needed to avoid errors with function calls
	public List<MessagePart> getContentParts() {
		return contentParts;
	}

	/**
	 * Gets refusal message generated by the model, if any.
	 * 
	 * Notice this needs a special handling since API responses return this as a
	 * separate string field, but when the message is serialized back in chat
	 * history for API call, it is provided as a message part. For this reason, this
	 * is always stored as a message part of type {@link RefusalPart}; we assume
	 * either a refusal or a content text is provided in the reply.
	 */
	@JsonIgnore
	public String getRefusal() {
		for (MessagePart p : contentParts)
			if (p instanceof RefusalPart)
				return ((RefusalPart) contentParts.get(0)).getContent();
		return null;
	}

	/**
	 * Sets refusal message generated by the model, if any.
	 * 
	 * Notice this needs a special handling since API responses return this as a
	 * separate string field, but when the message is serialized back in chat
	 * history for API call, it is provided as a message part. For this reason, this
	 * is always stored as a message part of type {@link RefusalPart}; we assume
	 * either a refusal or a content text is provided in the reply.
	 */
	public void setRefusal(String refusal) {
		contentParts.clear();
		if (refusal != null)
			contentParts.add(new RefusalPart(refusal));
	}

	/**
	 * 
	 * /** This will contain tool calls generated by the model.
	 */
	private List<OpenAiToolCall> toolCalls;

	@JsonIgnore
	// These are not sent back as part of history
	private List<Annotation> annotations = new ArrayList<>();

	private AudioOutput audio;

	/**
	 * Required when returning tool call results to the API.
	 * 
	 * Notice in this case role should be "tool" and name the name of the function
	 * being called.
	 */
	private String toolCallId;

	/**
	 * This will contain generated function call.
	 * 
	 * This is now deprecated and replaced by {@link #toolCalls} in newer models.
	 */
	private FunctionCall functionCall;

	public OpenAiChatMessage(@NonNull String content) {
		this(Role.USER, content, null);
	}

	// Some roles might use a null content
	public OpenAiChatMessage(@NonNull Role role, String content) {
		this(role, content, null);
	}

	public OpenAiChatMessage(@NonNull String content, String name) {
		this(Role.USER, content, name);
	}

	// Some roles might use a null content
	public OpenAiChatMessage(@NonNull Role role, String content, String name) {
		this.role = role;
		setContent(content);
		this.name = name;
	}

	public OpenAiChatMessage(FunctionCall functionCall) {
		this.role = Role.ASSISTANT;
		this.functionCall = functionCall;
	}

	public OpenAiChatMessage(Role role, ToolCallResult result) {

		setContent(result.getResult().toString());
		if (role == Role.FUNCTION) {
			this.role = Role.FUNCTION;
			this.name = result.getToolId();
		} else if (role == Role.TOOL) {
			this.role = Role.TOOL;
			this.name = result.getToolId();
			this.toolCallId = result.getToolCallId();
		} else {
			throw new IllegalArgumentException("Unsupported role: " + role);
		}
	}

	public OpenAiChatMessage(@NonNull Author author, @NonNull List<MessagePart> parts) {
		this.role = authorToRole(author);
		this.contentParts.addAll(parts);
	}
}
