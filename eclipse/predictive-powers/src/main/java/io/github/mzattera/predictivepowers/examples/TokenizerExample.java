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

package io.github.mzattera.predictivepowers.examples;

import io.github.mzattera.predictivepowers.AiEndpoint;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.ModelService.Tokenizer;

public class TokenizerExample {

	@SuppressWarnings("unused")
	public static void main(String args[]) throws Exception {
		
		// Notice same code will work using HuggingFaceEndpoint
		try (AiEndpoint endpoint = new OpenAiEndpoint()) {
			
			// Get a tokenizer for a model, GPT-4 in this example
			Tokenizer counter = endpoint.getModelService().getTokenizer("gpt-4");
			
			// Counts tokens in a string
			int tokens = counter.count("Hello World");
			
			// Get model context size
			int contextSize = endpoint.getModelService().getContextSize("gpt-4");

			// ....
			
		} // Close endpoint
	}
}
