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
import io.github.mzattera.predictivepowers.huggingface.client.HuggingFaceEndpoint;
import io.github.mzattera.predictivepowers.services.CompletionService;

public class CompletionExample {

	public static void main(String[] args) throws Exception {

		// Uncomment the below to use OpenAI API
		// AiEndpoint endpoint = new DirectOpenAiEndpoint();

		// Uncomment the below to use Hugging Face API
		AiEndpoint endpoint = new HuggingFaceEndpoint();

		try (endpoint;
			CompletionService cs = endpoint.getCompletionService(); ) {			
			
			System.out.println(cs.complete("Alan Turing was").getText());
		}
	}
}
