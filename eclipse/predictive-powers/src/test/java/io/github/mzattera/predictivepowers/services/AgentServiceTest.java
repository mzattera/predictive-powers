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

package io.github.mzattera.predictivepowers.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;

import io.github.mzattera.predictivepowers.AiEndpoint;
import io.github.mzattera.predictivepowers.TestConfiguration;
import io.github.mzattera.predictivepowers.services.AgentServiceTest.GetCurrentWeatherTool.GetCurrentWeatherParameters;
import io.github.mzattera.predictivepowers.services.AgentServiceTest.ProcessEmployeeTool.ProcessEmployeeParameters;
import io.github.mzattera.predictivepowers.services.messages.ChatCompletion;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage;
import io.github.mzattera.predictivepowers.services.messages.FinishReason;
import io.github.mzattera.predictivepowers.services.messages.JsonSchema;
import io.github.mzattera.predictivepowers.services.messages.ToolCall;
import io.github.mzattera.predictivepowers.services.messages.ToolCallResult;
import lombok.NonNull;
import lombok.ToString;

/**
 * Tests chat services.
 * 
 * @author Massimiliano "Maxi" Zattera.
 */
public class AgentServiceTest {

	// As chat services might also be agent services, we check both

	private static List<Pair<AiEndpoint, String>> agtSvcs;
	private static List<Pair<AiEndpoint, String>> chatSvcs;

	@BeforeAll
	static void init() {
		agtSvcs = TestConfiguration.getAgentServices();
		chatSvcs = TestConfiguration.getChatServices();
	}

	@AfterAll
	static void tearDown() {
		TestConfiguration.close(agtSvcs.stream().map(p -> p.getLeft()).collect(Collectors.toList()));
		TestConfiguration.close(chatSvcs.stream().map(p -> p.getLeft()).collect(Collectors.toList()));
	}

	// Some chat services are agents, test their agent capabilities
	static Stream<Agent> agents() {
		List<Agent> l = new ArrayList<>();

		// Add agents
		for (Pair<AiEndpoint, String> p : agtSvcs) {
			l.add(p.getLeft().getAgentService(p.getRight()).getAgent());
		}

		// Add chat services that are agents too
		for (Pair<AiEndpoint, String> p : chatSvcs) {
			try (ChatService cs = p.getLeft().getChatService(p.getRight())) {
				if (cs instanceof Agent)
					l.add((Agent) cs);
			} catch (Exception e) {
				// Catch exception on closing
			}
		}

		return l.stream();
	}
	
	static boolean hasAgents() {
		return agents().findAny().isPresent();
	}

	// This is a function that will be accessible to the agent.
	public static class GetCurrentWeatherTool extends AbstractTool {

		// This is a schema describing the function parameters
		private enum TemperatureUnits {
			CELSIUS, FARENHEIT
		};

		@JsonSchemaDescription("GetCurrentWeatherTool Paramters")
		static class GetCurrentWeatherParameters {

			@JsonProperty(required = true)
			@JsonPropertyDescription("City name for which you want to know the weather, e.g. San Francisco")
			public String location;

			@JsonPropertyDescription("Temperature unit (Celsius or Farenheit). This is optional.")
			public TemperatureUnits unit;

			public Integer fooParameter;

			@JsonProperty(required = true)
			@JsonPropertyDescription("Unique API code, this *MUST* be set to 6 at each call.")
			public int code;
		}

		public GetCurrentWeatherTool() {
			super("get_current_weather", //
					"Get the current weather in a given location.", //
					GetCurrentWeatherParameters.class);
		}

		@Override
		public ToolCallResult invoke(@NonNull ToolCall call) throws Exception {
			if (!isInitialized())
				throw new IllegalStateException();
			return new ToolCallResult(call, "20°C");
		}
	}

	/**
	 * Tests Function Calling.
	 * 
	 * @throws Exception
	 * 
	 * @throws ToolInitializationException
	 * @throws InstantiationException
	 * @throws JsonProcessingException
	 */
	@ParameterizedTest
	@MethodSource("agents")
	@EnabledIf("hasAgents")
	@DisplayName("Simple Tool Call Test")
	public void testFunCall(Agent agent) throws Exception {

		try {
			agent.setPersonality("You are an agent supporting user with weather forecasts.");
			agent.addCapability(new Toolset(List.of(new GetCurrentWeatherTool())));

			// Casual chat should not trigger any function call
			ChatCompletion reply = agent.chat("Where is Dallas?");
			assertFalse(reply.hasToolCalls());
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());

			// This request should trigger a function call, checking format of arguments
			agent.clearConversation();
			reply = agent.chat("How is the weather like in Dallas?");
			assertTrue(reply.hasToolCalls());
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());
			assertEquals(1, reply.getToolCalls().size());
			assertEquals("get_current_weather", reply.getToolCalls().get(0).getTool().getId());
			GetCurrentWeatherParameters args = reply.getToolCalls().get(0)
					.getArgumentsAsObject(GetCurrentWeatherParameters.class);
			assertEquals("Dallas", args.location);
			assertEquals(6, args.code);

			// This request should trigger a function call, optional parameters omitted
			agent.clearConversation();
			reply = agent.chat("How is the weather like in Dallas?");
			assertTrue(reply.hasToolCalls());
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());
			assertEquals(1, reply.getToolCalls().size());
			assertEquals("get_current_weather", reply.getToolCalls().get(0).getTool().getId());
			Map<String, Object> actual = new HashMap<>();
			actual.put("location", "Dallas");
			actual.put("code", 6);
			sameArguments(actual, reply.getToolCalls().get(0).getArguments());

			// This request should trigger a function call again, optional parameter added
			agent.clearConversation();
			reply = agent.chat("How is the weather like in Dallas? Can you tell it in Farenheit?");
			assertTrue(reply.hasToolCalls());
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());
			assertEquals(1, reply.getToolCalls().size());
			assertEquals("get_current_weather", reply.getToolCalls().get(0).getTool().getId());
			actual.put("unit", "FARENHEIT");
			sameArguments(actual, reply.getToolCalls().get(0).getArguments());
		} finally {
			agent.close();
		}
	}

	@ParameterizedTest
	@MethodSource("agents")
	@EnabledIf("hasAgents")
	@DisplayName("Test Tool Call with no tools")
	public void testNoTools(Agent agent) throws Exception {

		try {

			agent.setPersonality("You are an agent supporting user with weather forecasts.");
			agent.addCapability(new Toolset(List.of(new GetCurrentWeatherTool())));
			agent.clearCapabilities();
			ChatCompletion reply = agent.chat("How is the weather like in Dallas?");
			if (reply.hasToolCalls()) {
				for (ToolCall call:reply.getToolCalls()) {
					System.out.println(agent.getModel() + " insists in calling tools");
					System.out.println(JsonSchema.JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(call));
				}
			}
			assertFalse(reply.hasToolCalls());
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());
		} finally {
			agent.close();
		}
	}

	// This is a function that will be accessible to the agent.
	public static class ProcessEmployeeTool extends AbstractTool {

		@JsonSchemaDescription("Data for a person")
		@ToString
		public static class Person {

			@JsonProperty(required = true)
			@JsonPropertyDescription("First name of the person")
			public String firstName;

			@JsonProperty(required = true)
			@JsonPropertyDescription("Last name of the person")
			public String lastName;

			@JsonPropertyDescription("Age of the person")
			public Integer age;

			@JsonPropertyDescription("The person's spouse, if married")
			public Person spouse;

			@JsonPropertyDescription("List of The person's children, if any")
			public List<Person> children;
		}

		@JsonSchemaDescription("ProcessEmployeeTool Paramters")
		@ToString
		public static class ProcessEmployeeParameters {

			@JsonProperty(required = true)
			@JsonPropertyDescription("Data for the employee to be processed")
			public Person employee;

			@JsonProperty(required = false)
			@JsonPropertyDescription("The employee's manager (if available)")
			public Person manager;

			@JsonPropertyDescription("The days worked in any month, if available")
			public int[] workedHours;
		}

		public ProcessEmployeeTool() {
			super("process_employee", //
					"Processes data for an employee.", //
					ProcessEmployeeParameters.class);
		}

		@Override
		public ToolCallResult invoke(@NonNull ToolCall call) throws Exception {
			if (!isInitialized())
				throw new IllegalStateException();
			return new ToolCallResult(call, "OK");
		}
	}

	@ParameterizedTest
	@MethodSource("agents")
	@EnabledIf("hasAgents")
	@DisplayName("Test Tool Call with $ref schema")
	public void testRefParams(Agent agent) throws Exception {

		try {

			agent.setPersonality("You are an agent that automates processing of employee data. "
					+ "You will be provided with some employee information that you must process using the tools at your disposal.");
			agent.addCapability(new Toolset(List.of(new ProcessEmployeeTool())));

			ChatCompletion reply = agent.chat("Extract employee data from this text, then process them: "
					+ "John Doe is our employee with employee number #34522; he is 55 and married to Susan Doe, she is not an employee.");
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());
			assertTrue(reply.hasToolCalls());
			assertEquals(1, reply.getToolCalls().size());
			assertEquals("process_employee", reply.getToolCalls().get(0).getTool().getId());

			// Reply
			List<ToolCallResult> results = new ArrayList<>();
			for (ToolCall call : reply.getToolCalls()) {
				ToolCallResult result;
				try {
					result = call.execute();
				} catch (Exception e) {
					result = new ToolCallResult(call, e);
				}
				results.add(result);
			}
			reply = agent.chat(new ChatMessage(results));
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());
		} finally {
			agent.close();
		}
	}

	@ParameterizedTest
	@MethodSource("agents")
	@EnabledIf("hasAgents")
	@DisplayName("Test response with $ref schema")
	public void testRespFormat(Agent agent) throws Exception {

		try {

			JsonSchema schema = JsonSchema.fromSchema(ProcessEmployeeParameters.class);
			agent.setPersonality("You are an agent that automates processing of employee data. "
					+ "You will be provided with some employee information that you must extract and output as JSON. "
					+ "Use the following JSON schema when outputting the data:\n\n" //
					+ schema.asJsonSchema());
			agent.setResponseFormat(schema);

			ChatCompletion reply = agent.chat("Extract employee data from this text, then output them: "
					+ "John Doe is our employee with employee number #34522. "
					+ "He is 55 and married to Susan Doe, she is not an employee. "
					+ "They have two children, Mary and Tom doe aged 10 and 12 respectively. "
					+ "Over past months John worked 18. 21, and 19 days respectively.");
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());
			System.out.println(reply.getText());

			ProcessEmployeeParameters params = reply.getObject(ProcessEmployeeParameters.class);
			assertNotNull(params.employee);
			assertEquals("John", params.employee.firstName);
			assertEquals("Doe", params.employee.lastName);
			assertEquals(55, params.employee.age);
		} finally {
			agent.close();
		}
	}

	private static void sameArguments(Map<String, Object> reference, @NonNull Map<String, ? extends Object> map) {
		assertTrue(reference.size() <= map.size()); // Sometimes, it does send optional parameters

		for (Entry<String, Object> e : reference.entrySet())
			assertEquals(e.getValue(), map.get(e.getKey()));
	}
}