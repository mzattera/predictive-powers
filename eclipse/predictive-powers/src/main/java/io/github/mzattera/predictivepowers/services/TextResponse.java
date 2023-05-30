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

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * This class encapsulates a text response from the language model.
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
@Builder
@NoArgsConstructor
@RequiredArgsConstructor
//@AllArgsConstructor
@ToString
public final class TextResponse {

	/** Reason why the language model finished responding. */
	public enum FinishReason {
		/**
		 * API returned complete model output (decided to properly stop before max
		 * length was reached)
		 */
		COMPLETED,

		/** Incomplete model output due to token length limit */
		LENGTH_LIMIT_REACHED,

		/**
		 * API returned without errors, but cannot distinguish between COMPLETED and
		 * LENGTH_LIMIT_REACHED
		 */
		OK,

		/**
		 * API response still in progress or incomplete (for asynchronous calls, not
		 * supported at the moment)
		 */
		INCOMPLETE,

		/** Omitted content due to content filters */
		INAPPROPRIATE,

		UNKNOWN
	}

	@Getter
	@NonNull
	private String text;

	@Getter
	@NonNull
	private FinishReason finishReason;

	public static TextResponse fromGptApi(String text, String reason) {
		if (reason.equals("stop"))
			return new TextResponse(text, FinishReason.COMPLETED);
		if (reason.equals("length"))
			return new TextResponse(text, FinishReason.LENGTH_LIMIT_REACHED);
		if (reason.equals("content_filter"))
			return new TextResponse(text, FinishReason.INAPPROPRIATE);
		if (reason.equals("null"))
			return new TextResponse(text, FinishReason.INCOMPLETE);
		return new TextResponse(text, FinishReason.UNKNOWN);
	}
}
