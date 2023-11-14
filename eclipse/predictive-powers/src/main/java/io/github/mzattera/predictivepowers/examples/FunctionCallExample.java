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

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.github.mzattera.predictivepowers.openai.client.chat.Function;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatMessage;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiTextCompletion;
import io.github.mzattera.predictivepowers.services.ChatMessage.Role;

public class FunctionCallExample {

	// Name and description of function to call to get temperature for one town
	private final static String functionName = "getCurrentWeather";
	private final static String functionDescription = "Get the current weather in a given location.";

	// The function parameters
	private static class GetCurrentWeatherParameters {

		private enum TemperatureUnits {
			CELSIUS, FARENHEIT
		};

		@JsonProperty(required = true)
		@JsonPropertyDescription("The city and state, e.g. San Francisco, CA.")
		public String location;

		@JsonPropertyDescription("Temperature unit (CELSIUS or FARENHEIT), defaults to CELSIUS")
		public TemperatureUnits unit;
	}

	// List of functions available to the bot (for now it is only 1).
	private final static List<Function> functions = new ArrayList<>();
	static {
		functions.add(Function.builder().name(functionName).description(functionDescription)
				.parameters(GetCurrentWeatherParameters.class).build());
	}

	public static void main(String[] args) {

		try (OpenAiEndpoint endpoint = new OpenAiEndpoint()) {

			// Get chat service and set bot personality
			OpenAiChatService bot = endpoint.getChatService();
//			bot.setModel("gpt-3.5-turbo-0613"); // OK
			bot.setModel("gpt-3.5-turbo-1106"); // Error 400
			bot.setModel("gpt-4-1106-preview"); // Error 400
//			bot.setModel("gpt-4-0613"); // Error 400
			bot.setPersonality("You are an helpful assistant.");

			// Conversation loop
			try (Scanner console = new Scanner(System.in)) {
				while (true) {
					System.out.print("User     > ");
					String s = console.nextLine();

					OpenAiTextCompletion reply = bot.chat(s, functions);

					if (reply.isFunctionCall()) {
						// The bot generated a function call, show it
						System.out.println("CALL     > " + reply.getFunctionCall());

						// Your function call would go here..
						// We create a fake reply instead,
						// always returning 33° Celsius
						OpenAiChatMessage functionResult = new OpenAiChatMessage(Role.FUNCTION, "33°C", functionName);

						// Pass function result to the bot
						reply = bot.chat(functionResult);
					}

					System.out.println("Assistant> " + reply.getText());
				}
			}

		} // closes endpoint
	}
}