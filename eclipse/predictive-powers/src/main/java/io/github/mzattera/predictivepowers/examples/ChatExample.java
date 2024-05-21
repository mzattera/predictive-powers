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

package io.github.mzattera.predictivepowers.examples;

import java.util.Scanner;

import io.github.mzattera.predictivepowers.AiEndpoint;
import io.github.mzattera.predictivepowers.anthropic.client.AnthropicEndpoint;
import io.github.mzattera.predictivepowers.services.ChatService;

public class ChatExample {

	public static void main(String[] args) throws Exception {

		try (
				// Uncomment the below to use OpenAI API
				// AiEndpoint endpoint = new DirectOpenAiEndpoint();
				// ChatService agent = endpoint.getChatService();

				// Uncomment the below to Azure OpenAI Cognitive Service
				// AiEndpoint endpoint = new AzureOpenAiEndpoint();
				// ChatService agent = endpoint.getChatService("<YourDeployModelName>");

				// Uncomment the below to use Anthropic API
				AiEndpoint endpoint = new AnthropicEndpoint();
				ChatService agent = endpoint.getChatService();
				
				// Uncomment the below to use Hugging Face API
				// AiEndpoint endpoint = new HuggingFaceEndpoint();
				// ChatService agent = endpoint.getChatService();
				
			) {

			// Give instructions to agent
			agent.setPersonality("You are a very sad and depressed robot. "
					+ "Your answers highlight the sad part of things " + " and are caustic, sarcastic, and ironic.");

			// Conversation loop
			try (Scanner console = new Scanner(System.in)) {
				while (true) {
					System.out.print("User     > ");
					String s = console.nextLine();
					System.out.println("Assistant> " + agent.chat(s).getText());
				}
			}
		} // Close resources
	}
}
