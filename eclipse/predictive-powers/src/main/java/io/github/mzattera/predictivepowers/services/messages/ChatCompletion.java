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

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * This class encapsulates a {@link ChatMessage} response from a service.
 * 
 * In addition to providing the returned message, this also contains a reason
 * why the response terminated, which allows the developer to take corrective
 * measures or handle asynchronous calls, eventually.
 * 
 * @author Massimiliano "Maxi" Zattera.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
@SuperBuilder
@Getter
@Setter
@ToString
public class ChatCompletion {

	@NonNull
	private FinishReason finishReason;

	@NonNull
	private ChatMessage message;

	/**
	 * 
	 * @return A string representation of the returned message. Notice the message
	 *         could contain parts which are not easily representable as text (e.g.
	 *         a file).
	 */
	public String getText() {
		return message.getTextContent();
	}

	/**
	 * 
	 * @return The content of this message as an instance of given class. This
	 *         assumes {@link #getText()} will return a properly formatted JSON
	 *         representation of the object.
	 * 
	 * @throws JsonProcessingException If an error occurs while parsing the message
	 *                                 content.
	 */
	public <T> T getObject(Class<T> c) throws JsonProcessingException {
		return JsonSchema.JSON_MAPPER.readValue(getText(), c);
	}

	/**
	 * Convenience method to get all tool invocations contained in the returned
	 * message.
	 * 
	 * @return List of tool calls in the contained message.
	 */
	public List<ToolCall> getToolCalls() {
		return message.getToolCalls();
	}

	/**
	 * Check whether last call to the agent resulted in the agent invoking for any
	 * tool to be executed.
	 * 
	 * @return True if last call generated any tool invocation.
	 */
	public boolean hasToolCalls() {
		return message.hasToolCalls();
	}
}
