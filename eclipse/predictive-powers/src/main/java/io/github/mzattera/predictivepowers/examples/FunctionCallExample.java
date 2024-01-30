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
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.github.mzattera.predictivepowers.openai.client.chat.OpenAiToolCallResult;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiTextCompletion;
import io.github.mzattera.predictivepowers.services.AgentService;
import io.github.mzattera.predictivepowers.services.Tool;
import io.github.mzattera.predictivepowers.services.ToolCall;
import io.github.mzattera.predictivepowers.services.ToolCallResult;
import io.github.mzattera.predictivepowers.services.ToolInitializationException;
import lombok.NonNull;

public class FunctionCallExample {

	static Random RND = new Random();

	// This is a function that will be accessible to the agent.
	private static class GetCurrentWeatherTool implements Tool {

		// This is a schema describing the function parameters
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

		@Override
		public String getId() {
			return "getCurrentWeather";
		}

		@Override
		public String getDescription() {
			return "Get the current weather in a given location.";
		}

		@Override
		public Class<?> getParameterSchema() {
			return GetCurrentWeatherParameters.class;
		}

		@Override
		public void init(@NonNull AgentService agent) {
			// Initialization goes here...
		}

		@Override
		public OpenAiToolCallResult invoke(@NonNull ToolCall call) throws Exception {
			// Function implementation goes here.
			// In this example we simply return a random temperature.
			return new OpenAiToolCallResult(call, (RND.nextInt(10) + 20) + "°C");
		}
	}

	// List of functions available to the bot (for now it is only 1).
	private final static Collection<Tool> TOOLS = new ArrayList<>();
	static {
		TOOLS.add(new GetCurrentWeatherTool());
	}

	public static void main(String[] args) throws ToolInitializationException {

		try (OpenAiEndpoint endpoint = new OpenAiEndpoint()) {

			// Get chat service, set bot personality and tools used
			OpenAiChatService bot = endpoint.getChatService();
			bot.setPersonality("You are an helpful assistant.");

			// Tells the model which tools are available,
			// Notice that the service works with both function
			// and tool calls in the same way.
//			bot.setModel("gpt-3.5-turbo-0613"); // This uses simple function calls
			bot.setModel("gpt-4-1106-preview"); // This uses parallel function calls (tools)
			bot.setTools(TOOLS);

			// Conversation loop
			try (Scanner console = new Scanner(System.in)) {
				while (true) {
					System.out.print("User     > ");
					String s = console.nextLine();

					OpenAiTextCompletion reply = bot.chat(s);

					// Check if bot generated a function call
					while (reply.hasToolCalls()) {

						List<ToolCallResult> results = new ArrayList<>();

						for (ToolCall call : reply.getToolCalls()) {
							// The bot generated tool calls, print them
							System.out.println("CALL " + " > " + call);

							// Execute calls and handle errors nicely
							ToolCallResult result;
							try {
								result = call.getTool().invoke(call);
							} catch (Exception e) {
								result = new OpenAiToolCallResult(call, "Error: " + e.getMessage());
							}
							results.add(result);
						}

						// Pass results back to the bot
						// Notice this can generate other tool calls, hence the loop
						reply = bot.chat(results);
					}

					System.out.println("Assistant> " + reply.getText());
				}
			}

		} // closes endpoint
	}
}