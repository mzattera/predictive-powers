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

import io.github.mzattera.predictivepowers.services.messages.ChatCompletion;
import io.github.mzattera.predictivepowers.services.messages.FinishReason;
import io.github.mzattera.predictivepowers.services.messages.TextCompletion;
import io.github.mzattera.predictivepowers.services.messages.ToolCall;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * This extends {@link TextCompletion} allowing to return function/tool
 * calls.ote that the library Works only with ool calls, transparently
 * translating function calls in tool calls.
 * 
 * @author Massimiliano "Maxi" Zattera
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
//@RequiredArgsConstructor
@ToString(callSuper = true)
public class OpenAiChatCompletion extends ChatCompletion {

	// TODO URGENT: Tool calls should be part of teh returned message

	/**
	 * List of tool calls, if the call generated any.
	 */
	@Getter
	private List<? extends ToolCall> toolCalls;

	@Override
	public boolean hasToolCalls() {
		return ((toolCalls != null) && (toolCalls.size() > 0));
	}

	@Override
	public OpenAiChatMessage getMessage() {
		return (OpenAiChatMessage) super.getMessage();
	}

	public OpenAiChatCompletion(FinishReason finishReason, OpenAiChatMessage message,
			List<? extends ToolCall> toolCalls) {
		super(finishReason, message);
		this.toolCalls = toolCalls;
	}
}
