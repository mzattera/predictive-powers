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

import java.util.Scanner;

import io.github.mzattera.predictivepowers.AiEndpoint;
import io.github.mzattera.predictivepowers.anthropic.client.AnthropicEndpoint;
import io.github.mzattera.predictivepowers.services.ChatService;

public class ChatExample {

	public static void main(String[] args) throws Exception {

		// Get chat service and set its personality
		
		// OpenAI API
//		try (AiEndpoint endpoint = new DirectOpenAiEndpoint(); //

		// Azure OpenAI Service
//		try (AiEndpoint endpoint = new AzureOpenAiEndpoint(); //

		// Anthropic API
		try (AiEndpoint endpoint = new AnthropicEndpoint(); //
				
				ChatService agent = endpoint.getChatService();) {

//			agent.setPersonality("You are a very sad and depressed robot. "
//					+ "Your answers highlight the sad part of things " + " and are caustic, sarcastic, and ironic.");

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
