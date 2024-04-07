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

import io.github.mzattera.predictivepowers.openai.client.DirectOpenAiClient;
import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsResponse;

public class OpenAiClientExample {

	public static void main(String[] args) throws Exception {

		// Get API key from OS environment variable "OPENAI_API_KEY"
		try (OpenAiClient client = new DirectOpenAiClient()) {
			
			// Complete a sentence
			// see https://platform.openai.com/docs/api-reference/completions
			CompletionsRequest req = CompletionsRequest.builder()
					.model("davinci-002")
					.maxTokens(50)
					.prompt("Alan Turing was")
					.build();
			CompletionsResponse resp = client.createCompletion(req);

			// Prints result
			System.out.println(resp.getChoices().get(0).getText());
			
		} // closes client
	}
}
