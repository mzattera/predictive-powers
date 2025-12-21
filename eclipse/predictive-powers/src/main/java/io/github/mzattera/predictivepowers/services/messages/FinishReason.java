/*
 * Copyright 2024-2025 Massimiliano "Maxi" Zattera
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

/**
 * 
 */
package io.github.mzattera.predictivepowers.services.messages;

/**
 * This enumeration describes possible ways in which a language model completed
 * its output.
 * 
 * @author Massimiliano "Maxi" Zattera.
 */
public enum FinishReason {

	/**
	 * Text generation is not yet completed, model might be returning a partial
	 * result (e.g. to allow streaming).
	 */
	IN_PROGRESS,

	/**
	 * Text generation has successfully terminated and the text is complete.
	 */
	COMPLETED,

	/**
	 * Text generation is finished, but the text was truncated, probably for
	 * limitations in model output length.
	 */
	TRUNCATED,

	/**
	 * Text content was in part or completely omitted due to content filters (e.g.
	 * profanity filter)
	 */
	INAPPROPRIATE,

	/** All finish reasons that do not fit in any other value */
	OTHER;

	public static FinishReason fromAnthropicApi(String reason) {
		switch (reason) {
		case "end_turn":
		case "stop_sequence":
		case "tool_use":
			return FinishReason.COMPLETED;
		case "max_tokens":
			return FinishReason.TRUNCATED;
		default:
			throw new IllegalArgumentException("Unrecognized finish reason: " + reason);
		}
	}
}
