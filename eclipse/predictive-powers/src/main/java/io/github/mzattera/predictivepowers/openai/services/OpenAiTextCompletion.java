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

import io.github.mzattera.predictivepowers.openai.client.chat.OpenAiToolCall;
import io.github.mzattera.predictivepowers.services.AgentCompletion;
import io.github.mzattera.predictivepowers.services.TextCompletion;
import io.github.mzattera.predictivepowers.services.TextCompletion.FinishReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * This extends {@link TextCompletion} allowing to return function/tool
 * calls.ote that the library Works only with ool calls, transparently
 * translating function calls in tool calls.
 * 
 * @author Massimiliano "Maxi" Zattera
 */
@Getter
@Setter
@SuperBuilder
//@NoArgsConstructor
//@RequiredArgsConstructor
@AllArgsConstructor
@ToString
public class OpenAiTextCompletion extends AgentCompletion {

	// TODO URGENT: Extend AgentCompletion instead and rename accordingly

	@Override
	public OpenAiChatMessage getMessage() {
		return (OpenAiChatMessage) super.getMessage();
	}

	public OpenAiTextCompletion(String text, String finishReason, List<OpenAiToolCall> toolCalls) {
		this(text, FinishReason.fromGptApi(finishReason), toolCalls);
	}

	public OpenAiTextCompletion(String text, FinishReason finishReason, List<OpenAiToolCall> toolCalls) {
		super(OpenAiChatMessage.builder().content(text).build(), finishReason, toolCalls);
	}
}
