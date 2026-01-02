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

package io.github.mzattera.predictivepowers.ollama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;

import io.github.mzattera.ollama.client.model.ThinkSetting;
import io.github.mzattera.predictivepowers.ollama.OllamaAgentTest.GetCurrentWeatherTool.GetCurrentWeatherParameters;
import io.github.mzattera.predictivepowers.services.AbstractTool;
import io.github.mzattera.predictivepowers.services.AgentServiceTest.ProcessEmployeeTool.ProcessEmployeeParameters;
import io.github.mzattera.predictivepowers.services.ToolInitializationException;
import io.github.mzattera.predictivepowers.services.Toolset;
import io.github.mzattera.predictivepowers.services.messages.ChatCompletion;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage;
import io.github.mzattera.predictivepowers.services.messages.FinishReason;
import io.github.mzattera.predictivepowers.services.messages.JsonSchema;
import io.github.mzattera.predictivepowers.services.messages.ToolCall;
import io.github.mzattera.predictivepowers.services.messages.ToolCallResult;
import lombok.NonNull;
import lombok.ToString;

public class OllamaAgentTest {

	private static final String INSTRUCTIONS = "You are a halpful agent.";

	private static final String[] MODELS_8GB = new String[] { "granite3.2-vision:2b", "granite3.3:2b", "llama3.2:3b",
			"ministral-3:3b", "qwen3:4b", "qwen3-vl:4b" };

	private static final String MODEL = "qwen3-vl:8b";

	private static Object THINK = false;
	
	// TODO URGENT add this approach to Ollama chat service
	private static String BLOCKER = (THINK == null) ? "" : ""; // NON SERVE A NIENTE

	private static final double TEMPERATURE = 0.0;
	private static final int CTX_SIZE = 4 * 1024;

	private static OllamaEndpoint ep;

	@BeforeAll
	static void init() {
		ep = new OllamaEndpoint();
	}

	@AfterAll
	static void tearDown() {
		try {
			ep.close();
		} catch (Exception e) {
		}
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
			@JsonPropertyDescription("Unique API code, this *MUST* be set to \"XyF\" at each call.")
			public String code;
		}

		public GetCurrentWeatherTool() {
			super("get_current_weather", //
					"Get the current weather in a given location. Do not use it to get any other information.", //
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
	@Test
	@DisplayName("Tool but not Call")
	public void testNoFunCall() throws Exception {

		try (OllamaChatService agent = ep.getChatService(MODEL)) {
//		try (OpenAiEndpoint ep = new OpenAiEndpoint(); OpenAiChatService agent = ep.getChatService();) {
			agent.setTemperature(TEMPERATURE);
			agent.setMaxConversationTokens(CTX_SIZE);
			agent.setPersonality(INSTRUCTIONS);
			if (THINK != null) {
				agent.getDefaultRequest().setThink(new ThinkSetting(false));
				agent.setPersonality(agent.getPersonality() + BLOCKER);
				;
			}
			agent.addCapability(new Toolset(List.of(new GetCurrentWeatherTool())));

			// Casual chat should not trigger any function call
			ChatCompletion reply = agent.chat("Where is Dallas located?");
			assertFalse(reply.hasToolCalls());
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());
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
	@Test
	@DisplayName("Simple Tool Call Test")
	public void testSimpleFunCall() throws Exception {

		try (OllamaChatService agent = ep.getChatService(MODEL)) {
			agent.setTemperature(TEMPERATURE);
			agent.setMaxConversationTokens(CTX_SIZE);
			agent.setPersonality(INSTRUCTIONS);
			if (THINK != null) {
				agent.getDefaultRequest().setThink(new ThinkSetting(false));
				agent.setPersonality(agent.getPersonality() + BLOCKER);
				;
			}
			agent.addCapability(new Toolset(List.of(new GetCurrentWeatherTool())));

			// This request should trigger a function call, checking format of arguments
			ChatCompletion reply = agent.chat("How is the weather like in Dallas, TX?");
			assertTrue(reply.hasToolCalls());
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());
			assertEquals(1, reply.getToolCalls().size());
			assertEquals("get_current_weather", reply.getToolCalls().get(0).getTool().getId());
			GetCurrentWeatherParameters args = reply.getToolCalls().get(0)
					.getArgumentsAsObject(GetCurrentWeatherParameters.class);
			assertEquals("Dallas, TX", args.location);
			assertEquals("XyF", args.code);
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
	@Test
	@DisplayName("Tool Call w.o. Optionla params")
	public void testFunCallNoOpt() throws Exception {

		try (OllamaChatService agent = ep.getChatService(MODEL)) {
			agent.setTemperature(TEMPERATURE);
			agent.setMaxConversationTokens(CTX_SIZE);
			agent.setPersonality(INSTRUCTIONS);
			if (THINK != null) {
				agent.getDefaultRequest().setThink(new ThinkSetting(false));
				agent.setPersonality(agent.getPersonality() + BLOCKER);
				;
			}
			agent.addCapability(new Toolset(List.of(new GetCurrentWeatherTool())));

			// This request should trigger a function call, optional parameters omitted
			ChatCompletion reply = agent.chat("How is the weather like in Dallas, TX?");
			assertTrue(reply.hasToolCalls());
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());
			assertEquals(1, reply.getToolCalls().size());
			assertEquals("get_current_weather", reply.getToolCalls().get(0).getTool().getId());
			Map<String, Object> actual = new HashMap<>();
			actual.put("location", "Dallas, TX");
			actual.put("code", "XyF");
			sameArguments(actual, reply.getToolCalls().get(0).getArguments());

			// This request should trigger a function call again, optional parameter added
			agent.clearConversation();
			reply = agent.chat("How is the weather like in Dallas, TX? Can you tell it in Farenheit?");
			assertTrue(reply.hasToolCalls());
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());
			assertEquals(1, reply.getToolCalls().size());
			assertEquals("get_current_weather", reply.getToolCalls().get(0).getTool().getId());
			actual.put("unit", "FARENHEIT");
			sameArguments(actual, reply.getToolCalls().get(0).getArguments());
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
	@Test
	@DisplayName("Tool Call w. Optional params")
	public void testFunCall() throws Exception {

		try (OllamaChatService agent = ep.getChatService(MODEL)) {
			agent.setTemperature(TEMPERATURE);
			agent.setMaxConversationTokens(CTX_SIZE);
			agent.setPersonality(INSTRUCTIONS);
			agent.addCapability(new Toolset(List.of(new GetCurrentWeatherTool())));
			if (THINK != null) {
				agent.getDefaultRequest().setThink(new ThinkSetting(false));
				agent.setPersonality(agent.getPersonality() + BLOCKER);
				;
			}
			Map<String, Object> actual = new HashMap<>();
			actual.put("location", "Dallas, TX");
			actual.put("code", "XyF");
			actual.put("unit", "FARENHEIT");
			ChatCompletion reply = agent.chat("How is the weather like in Dallas, TX? Can you tell it in Farenheit?");
			assertTrue(reply.hasToolCalls());
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());
			assertEquals(1, reply.getToolCalls().size());
			assertEquals("get_current_weather", reply.getToolCalls().get(0).getTool().getId());
			sameArguments(actual, reply.getToolCalls().get(0).getArguments());
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
					"This tool is used to process data for an employee. You provide the required information and the tool will execute the processing in the background.", //
					ProcessEmployeeParameters.class);
		}

		@Override
		public ToolCallResult invoke(@NonNull ToolCall call) throws Exception {
			if (!isInitialized())
				throw new IllegalStateException();
			return new ToolCallResult(call, "OK");
		}
	}

	@Test
	@DisplayName("Test Tool Call with $ref schema")
	public void testRefParams() throws Exception {

		try (OllamaChatService agent = ep.getChatService(MODEL)) {
			agent.setTemperature(TEMPERATURE);
			agent.setMaxConversationTokens(CTX_SIZE);
			agent.setPersonality("You are an agent that automates processing of employee data. "
					+ "You will be provided with some employee information that you must process using the tools at your disposal.");
			agent.addCapability(new Toolset(List.of(new ProcessEmployeeTool())));
			if (THINK != null) {
				agent.getDefaultRequest().setThink(new ThinkSetting(false));
				agent.setPersonality(agent.getPersonality() + BLOCKER);
				;
			}

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
		}
	}

	@Test
	@DisplayName("Test response with $ref schema")
	public void testRespFormat() throws Exception {

		try (OllamaChatService agent = ep.getChatService(MODEL)) {
			agent.setTemperature(TEMPERATURE);
			agent.setMaxConversationTokens(CTX_SIZE);
			JsonSchema schema = JsonSchema.fromSchema(ProcessEmployeeParameters.class);
			agent.setPersonality("You are an agent that automates processing of employee data. "
					+ "You will be provided with some employee information that you must extract and output as JSON. "
					+ "Use the following JSON schema when outputting the data:\n\n" //
					+ schema.asJsonSchema());
			agent.setResponseFormat(schema);
			if (THINK != null) {
				agent.getDefaultRequest().setThink(new ThinkSetting(false));
				agent.setPersonality(agent.getPersonality() + BLOCKER);
				;
			}

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
		}
	}

	private static void sameArguments(Map<String, Object> reference, @NonNull Map<String, ? extends Object> map) {
		assertTrue(reference.size() <= map.size()); // Sometimes, it does send optional parameters

		for (Entry<String, Object> e : reference.entrySet())
			assertEquals(e.getValue(), map.get(e.getKey()));
	}
}