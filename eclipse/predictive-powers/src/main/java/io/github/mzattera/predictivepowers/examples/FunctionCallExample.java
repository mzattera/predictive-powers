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

import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.AbstractTool;
import io.github.mzattera.predictivepowers.services.Agent;
import io.github.mzattera.predictivepowers.services.Capability;
import io.github.mzattera.predictivepowers.services.Toolset;
import io.github.mzattera.predictivepowers.services.messages.ChatCompletion;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage;
import io.github.mzattera.predictivepowers.services.messages.ToolCall;
import io.github.mzattera.predictivepowers.services.messages.ToolCallResult;
import lombok.NonNull;

public class FunctionCallExample {

	static Random RND = new Random();

	// This is a function that will be accessible to the agent
	// Notice it must be public.
	public static class GetCurrentWeatherTool extends AbstractTool {

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

		public GetCurrentWeatherTool() {
			super("getCurrentWeather", // Function name
					"Get the current weather in a given location.", // Function description
					GetCurrentWeatherParameters.class);
		}

		@Override
		public ToolCallResult invoke(@NonNull ToolCall call) throws Exception {
			
			// Function implementation goes here.
			// In this example we simply return a random temperature.
			
			if (!isInitialized())
				throw new IllegalStateException("Tool must be initialized.");
			
			String location = getString("location", call.getArguments());
			return new ToolCallResult(call, "Temperature in " + location + " is " + (RND.nextInt(10) + 20) + "°C");
		}
	}

	// List of functions available to the bot (for now it is only 1).
	private final static Collection<Class<?>> TOOLS = new ArrayList<>();
	static {
		TOOLS.add(GetCurrentWeatherTool.class);
	}

	// Capability providing the functions to the agent
	private final static Capability DEFAULT_CAPABILITY = new Toolset(TOOLS);

	public static void main(String[] args) throws Exception {

		try (OpenAiEndpoint endpoint = new OpenAiEndpoint();

		// Get chat service, set bot personality and tools used
//			Agent bot = endpoint.getChatService("gpt-4-1106-preview"); // This uses chat API with parallel function calls (tools)
//			Agent bot = endpoint.getChatService("gpt-3.5-turbo-0613"); // This uses chat API with single function calls (tools)
				Agent bot = endpoint.getAgentService().getAgent(); // This uses assistants API
		) {

			bot.setPersonality("You are an helpful assistant.");

			// Tells the model which tools it can use, by providing a capability
			bot.addCapability(DEFAULT_CAPABILITY);

			// Conversation loop
			try (Scanner console = new Scanner(System.in)) {
				while (true) {
					System.out.print("User     > ");
					String s = console.nextLine();

					ChatCompletion reply = bot.chat(s);

					// Check if bot generated a function call
					while (reply.hasToolCalls()) {

						List<ToolCallResult> results = new ArrayList<>();

						for (ToolCall call : reply.getToolCalls()) {
							// The bot generated one or more tool calls,
							// print them for illustrative purposes
							System.out.println("CALL " + " > " + call);

							// Execute call handling errors nicely
							ToolCallResult result;
							try {
								result = call.getTool().invoke(call);
							} catch (Exception e) {
								result = new ToolCallResult(call, "Error: " + e.getMessage());
							}
							results.add(result);
						}

						// Pass results back to the bot
						// Notice this might generate other tool calls, hence the loop
						reply = bot.chat(new ChatMessage(results));
					}

					System.out.println("Assistant> " + reply.getText());
				}
			}

		} // Closes resources
	}
}