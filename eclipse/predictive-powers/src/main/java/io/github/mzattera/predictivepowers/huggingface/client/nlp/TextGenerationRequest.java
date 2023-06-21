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

import io.github.mzattera.predictivepowers.huggingface.client.HuggingFaceRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * @author Massimiliano "Maxi" Zattera
 *
 */
@Getter
@Setter
@SuperBuilder
//@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
@ToString
public class TextGenerationRequest extends HuggingFaceRequest {

	// TODO check if it supports wait for model and if so use it in services

	@Getter
	@Setter
	@Builder
//	@NoArgsConstructor
	@RequiredArgsConstructor
	@AllArgsConstructor
	@ToString
	public static class Parameters {
		/**
		 * (Default: None). Integer to define the top tokens considered within the
		 * sample operation to create new text.
		 */
		Integer topK;

		/**
		 * (Default: None). Float to define the tokens that are within the sample
		 * operation of text generation. Add tokens in the sample for more probable to
		 * least probable until the sum of the probabilities is greater than top_p.
		 */
		Double topP;

		/**
		 * (Default: 1.0). Float (0.0-100.0). The temperature of the sampling operation.
		 * 1 means regular sampling, 0 means always take the highest score, 100.0 is
		 * getting closer to uniform probability.
		 */
		Double temperature;

		/**
		 * (Default: None). Float (0.0-100.0). The more a token is used within
		 * generation the more it is penalized to not be picked in successive generation
		 * passes.
		 */
		Double repetitionPenalty;

		/**
		 * (Default: None). Int (0-250). The amount of new tokens to be generated, this
		 * does not include the input length it is a estimate of the size of generated
		 * text you want. Each new tokens slows down the request, so look for balance
		 * between response times and length of text generated.
		 */
		Integer maxNewTokens;

		/**
		 * (Default: None). Float (0-120.0). The amount of time in seconds that the
		 * query should take maximum. Network can cause some overhead so it will be a
		 * soft limit. Use that in combination with max_new_tokens for best results.
		 */
		Double maxTime;

		/**
		 * (Default: True). Bool. If set to False, the return results will not contain
		 * the original query making it easier for prompting.
		 */
		Boolean returnFullText;

		/**
		 * (Default: 1). Integer. The number of proposition you want to be returned.
		 */
		Integer numReturnSequences;

		/**
		 * (Optional: True). Bool. Whether or not to use sampling, use greedy decoding
		 * otherwise.
		 */
		Boolean doSample;
	}

	@Builder.Default
	Parameters parameters = new Parameters();
}
