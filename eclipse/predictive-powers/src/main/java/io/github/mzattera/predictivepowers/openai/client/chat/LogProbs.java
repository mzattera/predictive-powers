/*
 * Copyright 2024 Massimiliano "Maxi" Zattera
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

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * @author GPT-4
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class LogProbs {

	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@AllArgsConstructor
	@Builder
	@Getter
	@Setter
	@ToString
	public static class ContentToken {

		/**
		 * The token.
		 */
		@NonNull
		private String token;

		/**
		 * The log probability of this token.
		 */
		private double logprob;

		/**
		 * A list of integers representing the UTF-8 bytes representation of the token.
		 * Can be null if there is no bytes representation for the token.
		 */
		@Builder.Default
		private List<Integer> bytes = new ArrayList<>();

		/**
		 * List of the most likely tokens and their log probability, at this token
		 * position. This is non null only for LogProbs.content, ContentTokens inside
		 * ContentToken.topLogprobs do not have this field
		 */
		private List<ContentToken> topLogprobs;
	}

	/**
	 * A list of message content tokens with log probability information. Can be
	 * null.
	 */
	@Builder.Default
	private List<ContentToken> content = new ArrayList<>();

	@Builder.Default
	private List<ContentToken> refusal = new ArrayList<>();
}
