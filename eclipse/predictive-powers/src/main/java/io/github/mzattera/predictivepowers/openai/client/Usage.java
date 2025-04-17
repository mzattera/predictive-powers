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
package io.github.mzattera.predictivepowers.openai.client;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Usage data returned within a /completions response.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class Usage {

	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@AllArgsConstructor
	@Builder
	@Getter
	@Setter
	@ToString
	public static class CompletionTokensDetails {
		private Long acceptedPredictionTokens;
		private Long audioTokens;
		private Long reasoningTokens;
		private Long rejectedPredictionTokens;
	}

	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@AllArgsConstructor
	@Builder
	@Getter
	@Setter
	@ToString
	public static class PromptTokensDetails {
		private Long audioTokens;
		private Long cachedTokens;
	}

	private long promptTokens;
	private long completionTokens;
	private long totalTokens;
	private CompletionTokensDetails completionTokensDetails;
	private PromptTokensDetails promptTokensDetails;
}
