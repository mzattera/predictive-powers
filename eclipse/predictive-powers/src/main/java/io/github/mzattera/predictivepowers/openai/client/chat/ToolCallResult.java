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

package io.github.mzattera.predictivepowers.openai.client.chat;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * This holds the results of calling a tool.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@RequiredArgsConstructor
//@AllArgsConstructor
@ToString
public class ToolCallResult {

	/** ID for the call generating this result. */
	@NonNull
	String toolCallId;

	/** Name of the function being called. */
	@NonNull
	String name;

	/** Result of calling the tool. */
	@NonNull
	String result;

	/**
	 * Builds the result for given call.
	 */
	public ToolCallResult(ToolCall call, String result) {
		this(call.getId(), call.getFunction().getName(), result);
	}
}
