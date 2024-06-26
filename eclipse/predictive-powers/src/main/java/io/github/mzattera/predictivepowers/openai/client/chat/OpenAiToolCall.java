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

package io.github.mzattera.predictivepowers.openai.client.chat;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Tool call that a model returns indicating it wants to call a tool. This is
 * used by parallel function calling capability and Assistants API.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class OpenAiToolCall {

	/**
	 * The ID of the tool call.
	 */
	@NonNull
	private String Id;

	/**
	 * The type of the tool. Currently, only function is supported.
	 */
	@NonNull
	private OpenAiTool.Type type;

	/**
	 * A function call corresponding to this tool call.
	 */
	@NonNull
	private FunctionCall function;

	public OpenAiToolCall(FunctionCall call) {
		this("fake_id", OpenAiTool.Type.FUNCTION, call);
	}
}
