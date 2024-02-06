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

package io.github.mzattera.predictivepowers.services.messages;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * This holds the results of a {@link ToolCall}. It is used to pass results from
 * tool execution back to the calling agent.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Getter
@Setter
@ToString
public class ToolCallResult implements MessagePart {

	/** Unique ID for corresponding tool invocation. */
	@NonNull
	private String toolCallId;

	/** Unique ID of the tool being called. */
	// TODO Needed?
	@NonNull
	String toolId;

	/** Result of calling the tool. */
	Object result;

	public ToolCallResult(@NonNull ToolCall call, String result) {
		toolCallId = call.getId();
		toolId = call.getTool().getId();
		this.result = result; 
	}

	@Override
	public String getContent() {
		return (result == null ? "" : result.toString());
	}
}
