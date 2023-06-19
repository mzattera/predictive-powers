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

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Response from Hugging Face conversaation task.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@Getter
@Setter
@ToString
public class ConversationalResponse {

	/**
	 * A facility dictionary to send back for the next input (with the new user
	 * input addition).
	 */
	@Getter
	@Setter
	@ToString
	public static class Conversation {

		/**
		 * List of strings. The last inputs from the user in the conversation, <em>after
		 * the model has run.
		 */
		List<String> pastUserInputs;

		/**
		 * List of strings. The last outputs from the model in the conversation,
		 * <em>after the model has run.
		 */
		List<String> generatedResponses;
	}

	Conversation conversaton;

	/** The answer of the bot */
	String generatedText;
}
