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

import java.util.List;

import io.github.mzattera.predictivepowers.services.TextCompletion.FinishReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * This class encapsulates a response from an {#link AgentService}.
 * 
 * This add support for {@Tool} invocations.
 * 
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
@SuperBuilder
@NoArgsConstructor
//@RequiredArgsConstructor
@AllArgsConstructor
@ToString
public class AgentCompletion extends ChatCompletion {

	/**
	 * List of tool calls, if the call generated any.
	 */
	@Getter
	private List<? extends ToolCall> toolCalls;

	/**
	 * Check whether last call to the agent resulted in the agent invoking for any
	 * tool to be executed. Notice that if this return true, it might be
	 * {@link #getMessage()} is null.
	 * 
	 * @return True if last call generated any tool invocation.
	 */
	public boolean hasToolCalls() {
		return ((toolCalls != null) && (toolCalls.size() > 0));
	}

	public AgentCompletion(ChatMessage chatMessage, FinishReason finishReason, List<? extends ToolCall> toolCalls) {
		super(chatMessage, finishReason);
		this.toolCalls = toolCalls;
	}
}
