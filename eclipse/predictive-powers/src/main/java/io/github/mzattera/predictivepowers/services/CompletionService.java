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

/**
 * A completion service provides methods to complete/generate text (prompt
 * execution).
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public interface CompletionService extends Service {

	/**
	 * Number of top tokens considered within the sample operation to create new
	 * text.
	 */
	Integer getTopK();

	/**
	 * Number of top tokens considered within the sample operation to create new
	 * text.
	 */
	void setTopK(Integer topK);

	/**
	 * Add tokens in the sample for more probable to least probable until the sum of
	 * the probabilities is greater than this.
	 */
	Double getTopP();

	/**
	 * Add tokens in the sample for more probable to least probable until the sum of
	 * the probabilities is greater than this.
	 */
	void setTopP(Double topP);

	/**
	 * The temperature (0-100) of the sampling operation. 1 means regular sampling,
	 * 0 means always take the highest score, 100.0 is getting closer to uniform
	 * probability.
	 */
	Double getTemperature();

	/**
	 * The temperature (0-100) of the sampling operation. 1 means regular sampling,
	 * 0 means always take the highest score, 100.0 is getting closer to uniform
	 * probability.
	 */
	void setTemperature(Double temperature);

	/**
	 * Maximum amount of tokens to produce (not including the prompt).
	 */
	Integer getMaxNewTokens();

	/**
	 * Maximum amount of tokens to produce (not including the prompt).
	 */
	void setMaxNewTokens(Integer maxNewTokens);

	/**
	 * If true, returns the prompt in addition to the generated text.
	 * Implementations should ensure this defaults to false.
	 */
	boolean getEcho();

	/**
	 * If true, returns the prompt in addition to the generated text.
	 * Implementations should ensure this defaults to false.
	 */
	void setEcho(boolean echo);

	// TODO remove these getters / setters as they should be always 1 for the service
	
	/**
	 * Number of completions to return for each prompt. Implementations should
	 * ensure this defaults to 1.
	 */
	int getN();

	/**
	 * Number of completions to return for each prompt. Implementations should
	 * ensure this defaults to 1.
	 */
	void setN(int n);

	// TODO add "slot filling" capabilities: fill a slot in the prompt based on
	// values from a Map

	/**
	 * Completes text (executes given prompt).
	 */
	TextResponse complete(String prompt);

	/**
	 * Inserts text between given prompt and the suffix.
	 */
	TextResponse insert(String prompt, String suffix);
}