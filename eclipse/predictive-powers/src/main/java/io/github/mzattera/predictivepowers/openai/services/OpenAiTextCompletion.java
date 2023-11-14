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

import io.github.mzattera.predictivepowers.openai.services.OpenAiChatMessage.FunctionCall;
import io.github.mzattera.predictivepowers.services.TextCompletion;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * This extends {@link TextCompletion} allowing to return function calls.
 * 
 * @author mzatt
 */
@Getter
@Setter
@SuperBuilder
//@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
@ToString
public class OpenAiTextCompletion extends TextCompletion {

	private FunctionCall functionCall;

	public boolean isFunctionCall() {
		return (functionCall != null);
	}

	public OpenAiTextCompletion(String text, String finishReason, FunctionCall functionCall) {
		this(text, FinishReason.fromGptApi(finishReason), functionCall);
	}

	public OpenAiTextCompletion(String text, FinishReason finishReason, FunctionCall functionCall) {
		super(text, finishReason);
		this.functionCall = functionCall;
	}
}
