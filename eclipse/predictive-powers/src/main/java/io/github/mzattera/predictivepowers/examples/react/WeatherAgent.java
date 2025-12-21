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

package io.github.mzattera.predictivepowers.examples.react;

import java.util.List;
import java.util.Random;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;

import io.github.mzattera.predictivepowers.openai.services.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.AbstractTool;
import io.github.mzattera.predictivepowers.services.ToolInitializationException;
import io.github.mzattera.predictivepowers.services.messages.ToolCall;
import io.github.mzattera.predictivepowers.services.messages.ToolCallResult;
import lombok.NonNull;

public class WeatherAgent extends ToolableReactAgent {

	static Random RND = new Random();

	// This is a tool that will be accessible to the agent
	// Notice it must be public.
	public static class GetCurrentWeatherTool extends AbstractTool {

		@JsonSchemaDescription("This is a class describing parameters for GetCurrentWeatherTool")
		public static class Parameters {

			private enum TemperatureUnits {
				CELSIUS, FARENHEIT
			};

			@JsonProperty(required = true)
			@JsonPropertyDescription("The city and state, e.g. San Francisco, CA.")
			public String location;

			@JsonProperty(required = true)
			@JsonPropertyDescription("Your reasoning about why and how accomplish this step.")
			public String thought;

			@JsonPropertyDescription("Temperature unit (CELSIUS or FARENHEIT), defaults to CELSIUS")
			public TemperatureUnits unit;
		}

		public GetCurrentWeatherTool() throws JsonProcessingException {
			super("getCurrentWeather", // Function name
					"Get the current weather in a given city.", // Function description
					Parameters.class); // Function parameters
		}

		@Override
		public ToolCallResult invoke(@NonNull ToolCall call) throws Exception {

			// Tool implementation goes here.
			// In this example we simply return a random temperature.

			if (!isInitialized())
				throw new IllegalStateException("Tool must be initialized.");

			String location = getString("location", call.getArguments());
			return new ToolCallResult(call, "Temperature in " + location + " is " + (RND.nextInt(10) + 20) + "°C");
		}
	} // GetCurrentWeatherTool class

	public WeatherAgent(OpenAiEndpoint enpoint) throws ToolInitializationException, JsonProcessingException {
		super(WeatherAgent.class.getSimpleName(), //
				"This is able to find temperature in a given town.", //
				enpoint, List.of(new GetCurrentWeatherTool()), false);
	}
}