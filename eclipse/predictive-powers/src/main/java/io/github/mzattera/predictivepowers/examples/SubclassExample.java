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

import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiQuestionAnsweringService;

public class SubclassExample {

	public static void main(String[] args) {

		try (OpenAiEndpoint endpoint = new OpenAiEndpoint()) {

			// Explicitly instantiates a subclass of QuestionAnsweringService
			OpenAiQuestionAnsweringService svc = endpoint.getQuestionAnsweringService();

			// This field is provided only in OpenAI service,
			// to allow for additional configurability
			svc.getCompletionService()
				.setPersonality("You are an helpful question ansering assistant");

			// ... use the service here

		} // close endpoint
	}
}
