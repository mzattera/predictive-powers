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

package io.github.mzattera.predictivepowers.services.messages;

import io.github.mzattera.predictivepowers.services.messages.MessagePart.Type;
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
 * This holds the results of a {@link ToolCall}. It is used to pass results from
 * tool execution back to the calling agent.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
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
	private String toolId;

	/** Result of calling the tool. */
	private Object result;

	/** True if the result is an error. */
	@Builder.Default
	private boolean isError = false;

	@Override
	public Type getType() {
		return Type.TOOL_CALL_RESULT;
	}

	public ToolCallResult(@NonNull ToolCall call, String result) {
		toolCallId = call.getId();
		toolId = call.getTool().getId();
		this.result = result;
	}

	public ToolCallResult(String toolCallId, String toolId, String result) {
		this.toolCallId = toolCallId;
		this.toolId = toolId;
		this.result = result;
	}
	
	public ToolCallResult(ToolCall call, Exception e) {
		this(call, "Error: " + e.getMessage());
		isError = true;
	}

	@Override
	public String getContent() {
		return ("ToolCallResult(" + (isError ? "*ERROR* " : "") + (result == null ? "" : result.toString()) + ")");
	}

}
