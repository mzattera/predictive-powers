/*
 * Copyright 2023-2025 Massimiliano "Maxi" Zattera
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

package io.github.mzattera.predictivepowers.examples.react;

import java.util.List;

import io.github.mzattera.predictivepowers.openai.services.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.messages.JsonSchema;

public class Test {

	public static void main(String[] args) throws Exception {

		try (OpenAiEndpoint endpoint = new OpenAiEndpoint();
				ReactAgent agent = new ReactAgent("Orchestrator", endpoint, //
						List.of(new PersonLocatorAgent(endpoint), new WeatherAgent(endpoint)), false);) {

			agent.execute(
					"Determine whether the temperature in the town where Maxi is located the same as in Copenhagen.");

			System.out.println("//////////////////////////////////////////////////////////////////////");

			System.out.println(agent.getPersonality());
			System.out.println();
			System.out.println(
					JsonSchema.JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(agent.getSteps()));
		} // Closes resources
	}
}