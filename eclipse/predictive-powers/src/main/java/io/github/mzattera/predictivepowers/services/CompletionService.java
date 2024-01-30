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

import java.util.Map;
import java.util.Map.Entry;

/**
 * A completion service provides methods to complete/generate text (prompt
 * execution). This is more basic than a {@link ChatService} in that is meant to
 * provide completions, but not to hold conversations. In addition, ChatService
 * can normally be used to execute prompts as well, hence providing more
 * functionalities.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public interface CompletionService extends AiService {

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
	 */
	boolean getEcho();

	/**
	 * If true, returns the prompt in addition to the generated text.
	 */
	void setEcho(boolean echo);

	/**
	 * Completes text (executes given prompt).
	 */
	TextCompletion complete(String prompt);

	/**
	 * Completes text (executes given prompt).
	 * 
	 * @param parameters Parameters used for slot filling. See
	 *                   {@link #fillSlots(String, Map)}.
	 */
	TextCompletion complete(String prompt, Map<String, Object> parameters);

	/**
	 * Inserts text between given prompt and the suffix.
	 */
	TextCompletion insert(String prompt, String suffix);

	/**
	 * Inserts text between given prompt and the suffix.
	 * 
	 * @param parameters Parameters used for slot filling. See
	 *                   {@link #fillSlots(String, Map)}. This will be use to fill
	 *                   slots both in the prompt and the suffix.
	 */
	TextCompletion insert(String prompt, String suffix, Map<String, Object> parameters);

	/**
	 * Replaces 'slots' in a prompt.
	 * 
	 * Slots are place holders inserted in the prompt using a syntax like {{name}}
	 * where 'name' is a key in the provided Map; these place holders will be
	 * replaced with corresponding map value (using {@link Object#toString()}).
	 * 
	 * Parameters with a null value will result in a deleted slot, slots without
	 * corresponding parameters in the map will be ignored (and not replaced).
	 * 
	 * @param prompt
	 * @param parameters
	 * @return
	 */
	public static String fillSlots(String prompt, Map<String, ? extends Object> parameters) {
		if ((prompt == null) || (parameters == null))
			return prompt;

		for (Entry<String, ? extends Object> e : parameters.entrySet()) {
			String regex = "{{" + e.getKey() + "}}"; // No need to Pattern.quote()
			if (e.getValue() == null)
				prompt = prompt.replace(regex, "");
			else
				prompt = prompt.replace(regex, e.getValue().toString());
		}
		return prompt;
	}
}