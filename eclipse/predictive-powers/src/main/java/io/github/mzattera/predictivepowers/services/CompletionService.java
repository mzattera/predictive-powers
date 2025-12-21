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

package io.github.mzattera.predictivepowers.services;

import java.util.Map;
import java.util.Map.Entry;

import io.github.mzattera.predictivepowers.EndpointException;
import io.github.mzattera.predictivepowers.services.messages.TextCompletion;

/**
 * A completion service provides methods to complete text (prompt execution).
 * This is more basic than a {@link ChatService} in that is meant to provide
 * completions, but not to hold conversations.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public interface CompletionService extends AiService {

	// TODO URGENT: decide whether responseFormat should also go here... maybe not;
	// if yes, then a method is needed to deserialize the responses

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
	TextCompletion complete(String prompt) throws EndpointException;

	/**
	 * Inserts text between given prompt and the suffix.
	 */
	TextCompletion insert(String prompt, String suffix) throws EndpointException;

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
	// TODO URGENT Move this somewhare else (maybe in an utility class?) ->update
	// documentation online
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