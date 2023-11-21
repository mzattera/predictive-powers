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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * This class encapsulates a text completion response from the language model.
 * 
 * In addition to providing the returned text, this also contains a reason why
 * the response terminated, which allows the developer to take corrective
 * measures, eventually.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
@ToString
public class TextCompletion {

	/** Reason why the language model finished responding. */
	public enum FinishReason {

		/**
		 * API returned complete model output.
		 */
		COMPLETED,

		/** Incomplete model output due to token length limit */
		TRUNCATED,

		/** Omitted content due to content filters */
		INAPPROPRIATE,

		/**
		 * The API generated a function call (e.g. OpenAI function or tool call).
		 * 
		 * Notice that OpenAI API returns {@link #COMPLETED} if a function call was
		 * forced when setting a function name with function_call request parameter.
		 */
		FUNCTION_CALL,

		/** All finish reasons that do not fit in any other value */
		UNKNOWN;

		public static FinishReason fromGptApi(String reason) {
			switch (reason) {
			case "stop":
				return FinishReason.COMPLETED;
			case "length":
				return FinishReason.TRUNCATED;
			case "content_filter":
				return FinishReason.INAPPROPRIATE;
			case "function_call":
			case "tool_calls":
				return FinishReason.FUNCTION_CALL;
			default:
				return FinishReason.UNKNOWN;
			}
		}
	}

	@Getter
	private String text;

	@Getter
	@NonNull
	private FinishReason finishReason;
}
