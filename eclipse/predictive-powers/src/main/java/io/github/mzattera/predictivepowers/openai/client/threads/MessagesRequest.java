/*
 * Copyright 2024 Massimiliano "Maxi" Zattera
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

package io.github.mzattera.predictivepowers.openai.client.threads;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.github.mzattera.predictivepowers.openai.client.Metadata;
import io.github.mzattera.predictivepowers.openai.client.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.files.File;
import io.github.mzattera.predictivepowers.openai.client.threads.Message.Role;
import io.github.mzattera.predictivepowers.openai.services.OpenAiFilePart;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage.Author;
import io.github.mzattera.predictivepowers.services.messages.FilePart;
import io.github.mzattera.predictivepowers.services.messages.FilePart.ContentType;
import io.github.mzattera.predictivepowers.services.messages.MessagePart;
import io.github.mzattera.predictivepowers.services.messages.TextPart;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Message request for OpenAI API.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
@SuperBuilder
@Getter
@Setter
@ToString
public class MessagesRequest extends Metadata {

	/**
	 * The role of the entity that is creating the message.
	 */
	@NonNull
	private Role role;

	/**
	 * The content of the message.
	 */
	@NonNull
	private String content;

	/**
	 * A list of File IDs that the message should use.
	 */
	@NonNull
	@Builder.Default
	private List<String> fileIds = new ArrayList<>();

	/**
	 * Build a request to create given message.
	 * 
	 * @param msg A generic ChatMessage.
	 * @return A request that can be used to add given message to the conversation
	 *         thread.
	 * 
	 * @throws IOException If the message contains files that cannnot be uploaded.
	 */
	public static @NonNull MessagesRequest getInstance(ChatMessage msg, OpenAiEndpoint endpoint)
			throws IOException {

		if (msg.getAuthor() != Author.USER)
			throw new IllegalArgumentException("Only user messages are supported.");

		if (msg.hasToolCalls())
			throw new IllegalArgumentException("Only API can generate tool/function calls.");

		// Guard, this should be handled in code already.
		if (msg.hasToolCallResults())
			throw new IllegalArgumentException("Tool call results should be handled separatly'");

		// At the moment Message is multi-part, but only one string is supported
		List<String> txt = new ArrayList<>();
		List<String> fileIds = new ArrayList<>();
		for (MessagePart part : msg.getParts()) {
			if (part instanceof TextPart)
				txt.add(part.getContent());
			// This is currently in the Message definition but not supported by the API
//				else if (part instanceof ImagePart)
//					content.add(new Content(upload((ImagePart) part)));
			else if (part instanceof FilePart) {
				FilePart f = (FilePart) part;
				fileIds.add(upload(f.getContentType(), f, endpoint).getFileId());
			}
		}

		return MessagesRequest.builder() //
				.role(Role.USER) //
				.content(String.join("\n", txt)) //
				.fileIds(fileIds) //
				.build();
	}

	private static OpenAiFilePart upload(ContentType contentType, FilePart file, OpenAiEndpoint endpoint)
			throws IOException {
		if (file instanceof OpenAiFilePart)
			return (OpenAiFilePart) file;

		File oiaFile = endpoint.getClient().uploadFile(file.getInputStream(), file.getName(), "assistants");
		return new OpenAiFilePart(contentType, oiaFile, endpoint);
	}

}
