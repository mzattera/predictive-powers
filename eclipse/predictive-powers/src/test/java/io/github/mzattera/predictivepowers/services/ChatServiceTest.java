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

package io.github.mzattera.predictivepowers.services;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;
import com.openai.errors.BadRequestException;

import io.github.mzattera.predictivepowers.AiEndpoint;
import io.github.mzattera.predictivepowers.TestConfiguration;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatService;
import io.github.mzattera.predictivepowers.services.ChatServiceTest.GetCurrentWeatherTool.GetCurrentWeatherParameters;
import io.github.mzattera.predictivepowers.services.ChatServiceTest.ProcessEmployeeTool.ProcessEmployeeParameters;
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
public class ChatServiceTest {

	private static List<Pair<AiEndpoint, String>> svcs;

	@BeforeAll
	static void init() {
		svcs = TestConfiguration.getChatServices();
	}

	@AfterAll
	static void tearDown() {
		TestConfiguration.close(svcs.stream().map(p -> p.getLeft()).collect(Collectors.toList()));
	}

	// All services planned to be tested
	static Stream<Pair<AiEndpoint, String>> services() {
		return svcs.stream();
	}

	// Some chat services are agents, test their agent capabilities
	static Stream<Pair<AiEndpoint, String>> agents() {
		List<Pair<AiEndpoint, String>> l = new ArrayList<>();
		for (Pair<AiEndpoint, String> p : svcs) {
			try (ChatService cs = p.getLeft().getChatService(p.getRight())) {
				if (cs instanceof Agent)
					l.add(p);
			} catch (Exception e) {
				// Catch exception on closing
			}
		}
		return l.stream();
	}

	static Stream<Pair<AiEndpoint, String>> openAiServices() {
		return svcs.stream();
	}

	@DisplayName("Basic completion and chat.")
	@ParameterizedTest
	@MethodSource("services")
	public void testBasis(Pair<AiEndpoint, String> p) throws Exception {
		try (ChatService s = p.getLeft().getChatService(p.getRight())) {
			ChatCompletion resp = s.chat("Hi, my name is Maxi.");
			assertTrue(resp.getFinishReason() == FinishReason.COMPLETED);
			resp = s.chat("Can you please repeat my name?");
			assertTrue(resp.getFinishReason() == FinishReason.COMPLETED);
			assertTrue(resp.getText().contains("Maxi"));

			resp = s.complete("Hi, my name is Maxi.");
			assertTrue(resp.getFinishReason() == FinishReason.COMPLETED);
			resp = s.complete("Can you please repeat my name?");
			assertTrue(resp.getFinishReason() == FinishReason.COMPLETED);
			assertFalse(resp.getText().contains("Maxi"));
		}
	}

	@DisplayName("Getters and setters.")
	@ParameterizedTest
	@MethodSource("services")
	public void testGetSet(Pair<AiEndpoint, String> p) throws Exception {
		try (ChatService s = p.getLeft().getChatService(p.getRight())) {
			String m = s.getModel();
			assertNotNull(m);
			s.setModel("pippo");
			assertEquals("pippo", s.getModel());
			s.setModel(m);

			assertTrue(p.getLeft() == s.getEndpoint());

			s.setMaxHistoryLength(7);
			assertEquals(7, s.getMaxHistoryLength());
			// TODO make it nullable
//			s.setMaxHistoryLength(null);
//			assertNull(s.getMaxHistoryLength());

			s.setMaxConversationSteps(5);
			assertEquals(5, s.getMaxConversationSteps());
			// TODO make it nullable
//			s.setMaxConversationSteps(null);
//			assertNull(s.getMaxConversationSteps());

			if (s instanceof OpenAiChatService) {
				assertThrows(UnsupportedOperationException.class, () -> s.setTopK(1));
			} else {
				s.setTopK(1);
				assertEquals(1, s.getTopK());
				s.setTopK(null);
				assertNull(s.getTopK());
			}

			s.setTopP(2.0);
			assertEquals(2.0, s.getTopP());
			s.setTopP(null);
			assertNull(s.getTopP());

			s.setTemperature(3.0);
			assertEquals(3.0, s.getTemperature());
			s.setTemperature(null);
			assertNull(s.getTemperature());
			s.setTemperature(1.0);

			s.setMaxNewTokens(4);
			assertEquals(4, s.getMaxNewTokens());
			s.setMaxNewTokens(null);
			assertNull(s.getMaxNewTokens());

			if (s instanceof OpenAiChatService) {
				@SuppressWarnings("resource")
				OpenAiChatService svc = (OpenAiChatService) s;
				svc.setDefaultRequest(svc.getDefaultRequest().toBuilder().maxCompletionTokens(99).build());
				assertEquals(99, s.getMaxNewTokens());
			}
		}
	}

	@DisplayName("Call exercising all parameters.")
	@ParameterizedTest
	@MethodSource("services")
	public void testParams(Pair<AiEndpoint, String> p) throws Exception {

		try (ChatService s = p.getLeft().getChatService(p.getRight())) {

			ChatCompletion resp = null;

			if (s instanceof OpenAiChatService) {
				assertThrows(UnsupportedOperationException.class, () -> s.setTopK(5));
			} else {
				s.setTopK(5);
			}
			s.setTopP(null);
			s.setTemperature(null);
			s.setMaxNewTokens(40);
			try {
				resp = s.complete("Name a mammal.");
				assertTrue(resp.getFinishReason() == FinishReason.COMPLETED);
			} catch (BadRequestException e) {
				assertTrue(p.getRight().equals("o1") || p.getRight().equals("o3-mini"));
			}

			s.setTopK(null);
			s.setTopP(0.2);
			s.setTemperature(null);
			s.setMaxNewTokens(40);
			try {
				resp = s.chat("Name a mammal.");
				assertTrue(resp.getFinishReason() == FinishReason.COMPLETED);
			} catch (BadRequestException e) {
				assertTrue(p.getRight().equals("o1") || p.getRight().equals("o3-mini"));
			}

			s.setTopK(null);
			s.setTopP(null);
			s.setTemperature(20.0);
			s.setMaxNewTokens(40);
			try {
				resp = s.complete("Name a mammal.");
				assertTrue(resp.getFinishReason() == FinishReason.COMPLETED);
			} catch (BadRequestException e) {
				assertTrue(p.getRight().equals("o1") || p.getRight().equals("o3-mini"));
			}
		}
	}

	///////////////////////////////////////////////////////////////////////
	/// Tool Calling and Structured Outputs
	///////////////////////////////////////////////////////////////////////

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
			@JsonPropertyDescription("Unique API code, this is always equal to 6.")
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
	@DisplayName("Simple Tool Call Test")
	public void testFunCall(Pair<AiEndpoint, String> p) throws Exception {

		String model = p.getRight();
		try (Agent cs = (Agent) p.getLeft().getChatService(model);) {

			cs.setPersonality("You are an agent supporting user with weather forecasts.");
			cs.addCapability(new Toolset(List.of(new GetCurrentWeatherTool())));

			// Casual chat should not trigger any function call
			ChatCompletion reply = cs.chat("Where is Dallas?");
			assertFalse(reply.hasToolCalls());
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());

			// This request should trigger a function call, checking format of arguments
			cs.clearConversation();
			reply = cs.chat("How is the weather like in Dallas?");
			assertTrue(reply.hasToolCalls());
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());
			assertEquals(1, reply.getToolCalls().size());
			assertEquals("get_current_weather", reply.getToolCalls().get(0).getTool().getId());
			GetCurrentWeatherParameters args = reply.getToolCalls().get(0)
					.getArgumentsAsObject(GetCurrentWeatherParameters.class);
			assertEquals("Dallas", args.location);
			assertEquals(6, args.code);

			// This request should trigger a function call, optional parameters omitted
			cs.clearConversation();
			reply = cs.chat("How is the weather like in Dallas?");
			assertTrue(reply.hasToolCalls());
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());
			assertEquals(1, reply.getToolCalls().size());
			assertEquals("get_current_weather", reply.getToolCalls().get(0).getTool().getId());
			Map<String, Object> actual = new HashMap<>();
			actual.put("location", "Dallas");
			actual.put("code", 6);
			sameArguments(actual, reply.getToolCalls().get(0).getArguments());

			// This request should trigger a function call again, optional parameter added
			cs.clearConversation();
			reply = cs.chat("How is the weather like in Dallas? Can you tell it in Farenheit?");
			assertTrue(reply.hasToolCalls());
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());
			assertEquals(1, reply.getToolCalls().size());
			assertEquals("get_current_weather", reply.getToolCalls().get(0).getTool().getId());
			actual.put("unit", "FARENHEIT");
			sameArguments(actual, reply.getToolCalls().get(0).getArguments());
		}
	}

	@ParameterizedTest
	@MethodSource("agents")
	@DisplayName("Test Tool Call with no tools")
	public void testNoTools(Pair<AiEndpoint, String> p) throws Exception {

		String model = p.getRight();
		try (Agent cs = (Agent) p.getLeft().getChatService(model);) {

			cs.setPersonality("You are an agent supporting user with weather forecasts.");
			cs.addCapability(new Toolset(List.of(new GetCurrentWeatherTool())));
			cs.clearCapabilities();
			ChatCompletion reply = cs.chat("How is the weather like in Dallas?");
			assertFalse(reply.hasToolCalls());
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());
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
	@DisplayName("Test Tool Call with $ref schema")
	public void testRefParams(Pair<AiEndpoint, String> p) throws Exception {

		String model = p.getRight();
		try (Agent cs = (Agent) p.getLeft().getChatService(model);) {

			cs.setPersonality("You are an agent that automates processing of employee data. "
					+ "You will be provided with some employee information that you must process using the tools at your disposal.");
			cs.addCapability(new Toolset(List.of(new ProcessEmployeeTool())));

			ChatCompletion reply = cs.chat("Extract employee data from this text, then process them: "
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
			reply = cs.chat(new ChatMessage(results));
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());
		}
	}

	@ParameterizedTest
	@MethodSource("agents")
	@DisplayName("Test response with $ref schema")
	public void testRespFormat(Pair<AiEndpoint, String> p) throws Exception {

		String model = p.getRight();
		try (Agent cs = (Agent) p.getLeft().getChatService(model);) {

			JsonSchema schema = JsonSchema.fromSchema(ProcessEmployeeParameters.class);
			cs.setPersonality("You are an agent that automates processing of employee data. "
					+ "You will be provided with some employee information that you must extract and output as JSON. "
					+ "Use the following JSON schema when outputting the data:\n\n" //
					+ schema.asJsonSchema());
			cs.setResponseFormat(schema);

			ChatCompletion reply = cs.chat("Extract employee data from this text, then output them: "
					+ "John Doe is our employee with employee number #34522. "
					+ "He is 55 and married to Susan Doe, she is not an employee. "
					+ "They have two children, Mary and Tom doe aged 10 and 12 respectively. "
					+ "Over past months John worked 18. 21, and 19 days respectively.");
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());
			System.out.println(reply.getText());

			ProcessEmployeeParameters params = reply.getObject(ProcessEmployeeParameters.class);
			assertNonNull(params.employee);
			assertEquals(params.employee.firstName);
			// TODO URGENT do some basic tests here
			System.out.println(params.toString());
		}
	}

	private static void sameArguments(Map<String, Object> reference, @NonNull Map<String, ? extends Object> map) {
		assertTrue(reference.size() <= map.size()); // Sometimes, it does send optional parameters

		for (Entry<String, Object> e : reference.entrySet())
			assertEquals(e.getValue(), map.get(e.getKey()));
	}
}