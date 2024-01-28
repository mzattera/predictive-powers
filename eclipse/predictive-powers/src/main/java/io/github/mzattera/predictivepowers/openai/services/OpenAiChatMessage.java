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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

import io.github.mzattera.predictivepowers.openai.client.chat.FunctionCall;
import io.github.mzattera.predictivepowers.openai.client.chat.ToolCall;
import io.github.mzattera.predictivepowers.openai.client.chat.ToolCallResult;
import io.github.mzattera.predictivepowers.services.ChatMessage;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
//@Builder
@NoArgsConstructor
//@RequiredArgsConstructor
//@AllArgsConstructor
@ToString(callSuper = true)
public class OpenAiChatMessage extends ChatMessage {

	/**
	 * The originator of the message.
	 */
	public enum Role {

		/** Marks messages coming from the user */
		USER("user"),

		/** Marks messages coming from the bot/agent/assistant */
		ASSISTANT("assistant"),

		/**
		 * Marks text used for bot configuration (e.g. in OpenAI ChatGPT). It might not
		 * be supported by all services.
		 */
		SYSTEM("system"),

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

	@Override
	@JsonIgnore
	// Suppress serialization of this field
	public Author getAuthor() {
		return roleToAuthor(role);
	}

	private static Author roleToAuthor(Role role) {
		switch (role) {
		case USER:
		case SYSTEM:
			return Author.USER;
		case ASSISTANT:
		case FUNCTION:
		case TOOL:
			return Author.BOT;
		default:
			throw new IllegalArgumentException(); // Guard
		}
	}

	private static Role authorToRole(Author author) {
		switch (author) {
		case USER:
			return Role.USER;
		case BOT:
			return Role.ASSISTANT;
		default:
			throw new IllegalArgumentException(); // Guard
		}
	}

	/** The role of the messages author */
	Role role;

	/**
	 * The name of the author of this message.
	 * 
	 * For OpenAI API, name is required if role is FUNCTION or TOOL, and it should
	 * be the name of the function whose response is in the content. May contain
	 * a-z, A-Z, 0-9, and underscores, with a maximum length of 64 characters.
	 */
	String name;

	/**
	 * This will contain tool calls generated by the model.
	 */
	List<ToolCall> toolCalls;

	/**
	 * Required when returning tool call results to the API.
	 * 
	 * Notice in this case role should be "tool" and name the name of the function
	 * being called.
	 */
	String toolCallId;

	/**
	 * This will contain generated function call.
	 * 
	 * This is now deprecated and replaced by {@link #toolCalls} in newer models.
	 */
	FunctionCall functionCall;

	public OpenAiChatMessage(ChatMessage msg) {
		this(authorToRole(msg.getAuthor()), msg.getContent(), null, null);
	}

	public OpenAiChatMessage(Role role, String content) {
		this(role, content, null, null);
	}

	public OpenAiChatMessage(Role role, String content, String name) {
		this(role, content, name, null);
	}

	// TODO URGENT name and content are optional for a function call message
	public OpenAiChatMessage(Role role, String content, String name, FunctionCall functionCall) {
		super(roleToAuthor(role), content);
		this.role = role;
		this.name = name;
		this.functionCall = functionCall;
	}

	public OpenAiChatMessage(Role role, ToolCallResult result) {
		super(roleToAuthor(role), result.getResult());

		if (role == Role.FUNCTION) {
			this.role = Role.FUNCTION;
			this.name = result.getName();
		} else if (role == Role.TOOL) {
			this.role = Role.TOOL;
			this.name = result.getName();
			this.toolCallId = result.getToolCallId();
		} else {
			throw new IllegalArgumentException("Wrong role: " + role);
		}
	}
}
