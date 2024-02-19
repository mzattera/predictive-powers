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

package io.github.mzattera.predictivepowers.huggingface.client.nlp;

import java.util.ArrayList;
import java.util.List;

import io.github.mzattera.predictivepowers.huggingface.client.Options;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class ConversationalRequest {

	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@RequiredArgsConstructor
	@Builder
	@Getter
	@Setter
	@ToString
	public static class Inputs {

		/**
		 * (required) The last input from the user in the conversation.
		 * 
		 */
		@NonNull
		private String text;

		/**
		 * A list of strings corresponding to the earlier replies from the model.
		 */
		@NonNull
		@Builder.Default
		private List<String> generatedResponses = new ArrayList<>();

		/**
		 * A list of strings corresponding to the earlier replies from the user. Should
		 * be of the same length of generated_responses.
		 */
		@NonNull
		@Builder.Default
		private List<String> pastUserInputs = new ArrayList<>();
	}

	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	@Getter
	@Setter
	@ToString
	public static class Parameters {

		/**
		 * (Default: None). Integer to define the minimum length in tokens of the output
		 * summary.
		 */
		private Integer minLength;

		/**
		 * (Default: None). Integer to define the maximum length in tokens of the output
		 * summary.
		 */
		private Integer maxLength;

		/**
		 * (Default: None). Integer to define the top tokens considered within the
		 * sample operation to create new text.
		 */
		private Integer topK;

		/**
		 * (Default: None). Float to define the tokens that are within the sample
		 * operation of text generation. Add tokens in the sample for more probable to
		 * least probable until the sum of the probabilities is greater than top_p.
		 */
		private Double topP;

		/**
		 * (Default: 1.0). Float (0.0-100.0). The temperature of the sampling operation.
		 * 1 means regular sampling, 0 means always take the highest score, 100.0 is
		 * getting closer to uniform probability.
		 */
		private Double temperature;

		/**
		 * (Default: None). Float (0.0-100.0). The more a token is used within
		 * generation the more it is penalized to not be picked in successive generation
		 * passes.
		 */
		private Double repetitionPenalty;

		/**
		 * (Default: None). Float (0-120.0). The amount of time in seconds that the
		 * query should take maximum. Network can cause some overhead so it will be a
		 * soft limit.
		 */
		private Double maxTime;
	}

	@Builder.Default
	private Parameters parameters = new Parameters();

	@NonNull
	@Builder.Default
	private Inputs inputs = new Inputs();

	@Builder.Default
	private Options options = new Options();
}
