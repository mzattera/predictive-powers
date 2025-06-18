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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;

import io.github.mzattera.predictivepowers.openai.client.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.AbstractTool;
import io.github.mzattera.predictivepowers.services.ToolInitializationException;
import io.github.mzattera.predictivepowers.services.messages.ToolCall;
import io.github.mzattera.predictivepowers.services.messages.ToolCallResult;
import lombok.NonNull;

public class PersonLocatorAgent extends ExecutorAgent {

	public static class LocatePersonTool extends AbstractTool {

		@JsonSchemaDescription("This is a class describing parameters for LocatePersonTool")
		public static class Parameters {

			@JsonProperty(required = true)
			@JsonPropertyDescription("The name of the person you want to locate.")
			public String person;

			@JsonProperty(required = true)
			@JsonPropertyDescription("Your reasoning about why and how accomplish this step.")
			public String thought;
		}

		public LocatePersonTool() throws JsonProcessingException {
			super("locatePersonTool", // Function name
					"Returns the city where a person is located.", // Function description
					LocatePersonTool.Parameters.class); // Function parameters
		}

		@Override
		public ToolCallResult invoke(@NonNull ToolCall call) throws Exception {

			// Tool implementation goes here.
			// In this example we simply return a random temperature.

			if (!isInitialized())
				throw new IllegalStateException("Tool must be initialized.");

			String person = getString("person", call.getArguments());
			return new ToolCallResult(call, person + " is currently located in Padua, Italy.");
		}
	} // GetCurrentWeatherTool class

	public PersonLocatorAgent(OpenAiEndpoint enpoint) throws ToolInitializationException, JsonProcessingException {
		super(PersonLocatorAgent.class.getSimpleName(), //
				"This is able to find temperature in a given town.", //
				enpoint, List.of(new LocatePersonTool()));
	}
}