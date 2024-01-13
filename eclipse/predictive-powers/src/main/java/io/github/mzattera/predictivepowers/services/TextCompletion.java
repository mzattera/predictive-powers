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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * This class encapsulates a response from a {@link CompletionService}.
 * 
 * In addition to providing the returned text, this also contains a reason why
 * the response terminated, which allows the developer to take corrective
 * measures, eventually.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
@Builder
@NoArgsConstructor
//@RequiredArgsConstructor
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

		/** All finish reasons that do not fit in any other value */
		OTHER;

		public static FinishReason fromGptApi(String reason) {
			switch (reason) {
			case "stop":
				return FinishReason.COMPLETED;
			case "length":
				return FinishReason.TRUNCATED;
			case "content_filter":
				return FinishReason.INAPPROPRIATE;
			default:
				return FinishReason.OTHER;
			}
		}
	}

	@Getter
	@NonNull
	private String text;

	@Getter
	@NonNull
	private FinishReason finishReason;
}
