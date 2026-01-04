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
import java.util.Map;
import java.util.Set;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

/**
 * This holds the results of a {@link ToolCall}. It is used to pass results from
 * tool execution back to the calling agent.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
@Getter
@ToString
public final class ToolCallResult implements MessagePart {

	/** Unique ID for corresponding tool invocation. */
	private final @NonNull String toolCallId;

	/** Unique ID of the tool being called. */
	private final @NonNull String toolId;

	/** Result of calling the tool. */
	private final Object result;

	/** True if the result is an error. */
	private final boolean isError;

	@Override
	public Type getType() {
		return Type.TOOL_CALL_RESULT;
	}

	public ToolCallResult(@NonNull ToolCall call, String result) {
		this(call.getId(), call.getToolId(), result, false);
	}

	public ToolCallResult(@NonNull String toolCallId, @NonNull String toolId, String result) {
		this(toolCallId, toolId, result, false);
	}

	public ToolCallResult(ToolCall call, Exception e) {
		this(call.getId(), call.getToolId(), "ERROR: " + e.getMessage(), true);
	}

	@Builder
	public ToolCallResult(@NonNull String toolCallId, @NonNull String toolId, Object result, boolean isError) {
		this.toolCallId = toolCallId;
		this.toolId = toolId;
		this.isError = isError;
		this.result = makeImmutable(result);
	}

	private static Object makeImmutable(Object obj) {
		if (obj instanceof List) {
			return List.copyOf((List<?>) obj);
		} else if (obj instanceof Map) {
			return Map.copyOf((Map<?, ?>) obj);
		} else if (obj instanceof Set) {
			return Set.copyOf((Set<?>) obj);
		}
		return obj;
	}

	@Override
	public String getContent() {
		return ("ToolCallResult(" + (result == null ? "" : result.toString()) + ")");
	}
}
