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

package io.github.mzattera.predictivepowers.openai.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.mzattera.predictivepowers.openai.client.chat.FunctionCall;
import io.github.mzattera.predictivepowers.openai.client.chat.OpenAiTool;
import io.github.mzattera.predictivepowers.openai.client.chat.OpenAiToolCall;
import io.github.mzattera.predictivepowers.openai.client.chat.ToolChoice;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiModelMetaData.SupportedCallType;
import io.github.mzattera.predictivepowers.services.Agent;
import io.github.mzattera.predictivepowers.services.Tool;
import io.github.mzattera.predictivepowers.services.ToolInitializationException;
import io.github.mzattera.predictivepowers.services.messages.ChatCompletion;
import io.github.mzattera.predictivepowers.services.messages.FinishReason;
import io.github.mzattera.predictivepowers.services.messages.ToolCall;
import io.github.mzattera.predictivepowers.services.messages.ToolCallResult;
import lombok.NonNull;

/**
 * Test the OpenAI (old) function calling API.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class ToolCallTest {

	private final static String MODEL = "gpt-4-turbo-preview";

	@BeforeAll
	public static void check() {

		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {
			assertEquals(SupportedCallType.TOOLS, ep.getModelService().getSupportedCallType(MODEL));
		}
	}

	private final static ObjectMapper mapper = new ObjectMapper();

	// This is a function that will be accessible to the agent.
	private static class GetCurrentWeatherTool implements Tool {

		// This is a schema describing the function parameters
		public enum TemperatureUnits {
			CELSIUS, FARENHEIT
		};

		public static class GetCurrentWeatherParameters {

			@JsonProperty(required = true)
			@JsonPropertyDescription("The city and state, e.g. San Francisco, CA")
			public String location;

			@JsonPropertyDescription("Temperature unit (Celsius or Farenheit). This is optional.")
			public TemperatureUnits unit;

			@SuppressWarnings("unused")
			public Integer fooParameter;

			@JsonPropertyDescription("Unique API code, this is an integer which must be passed and it is always equal to 6.")
			public int code;
		}

		@Override
		public String getId() {
			return "get_current_weather";
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
		public void init(@NonNull Agent agent) {
			// Initialization goes here...
		}

		@Override
		public ToolCallResult invoke(@NonNull ToolCall call) throws Exception {
			// Function implementation goes here.
			// In this example we simply return a random temperature.
			return new ToolCallResult(call, "20°C");
		}
	}

	private final static List<OpenAiTool> TOOLS = new ArrayList<>();
	static {
		TOOLS.add(new OpenAiTool(new GetCurrentWeatherTool()));
	}

	/**
	 * Tests ToolChoice serialization.
	 */
	@Test
	public void test51() throws JsonProcessingException {

		String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(ToolChoice.NONE);
		assertEquals("\"none\"", json);

		json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(ToolChoice.AUTO);
		assertEquals("\"auto\"", json);

		json = mapper.writeValueAsString(new ToolChoice(TOOLS.get(0).getFunction()));
		assertEquals("{\"type\":\"function\",\"function\":{\"name\":\"get_current_weather\"}}", json);
	}

	/**
	 * Test Tool serialization.
	 * 
	 * @throws JsonProcessingException
	 */
	@Test
	public void test52() throws JsonProcessingException {
		OpenAiTool t = TOOLS.get(0);

		String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(t);
		assertEquals("{\r\n" + "  \"type\" : \"function\",\r\n" + "  \"function\" : {\r\n"
				+ "    \"name\" : \"get_current_weather\",\r\n"
				+ "    \"description\" : \"Get the current weather in a given location.\",\r\n"
				+ "    \"parameters\" : {\r\n" + "      \"$schema\" : \"http://json-schema.org/draft-04/schema#\",\r\n"
				+ "      \"title\" : \"Get Current Weather Parameters\",\r\n" + "      \"type\" : \"object\",\r\n"
				+ "      \"additionalProperties\" : false,\r\n" + "      \"properties\" : {\r\n"
				+ "        \"location\" : {\r\n" + "          \"type\" : \"string\",\r\n"
				+ "          \"description\" : \"The city and state, e.g. San Francisco, CA\"\r\n" + "        },\r\n"
				+ "        \"unit\" : {\r\n" + "          \"type\" : \"string\",\r\n"
				+ "          \"enum\" : [ \"CELSIUS\", \"FARENHEIT\" ],\r\n"
				+ "          \"description\" : \"Temperature unit (Celsius or Farenheit). This is optional.\"\r\n"
				+ "        },\r\n" + "        \"fooParameter\" : {\r\n" + "          \"type\" : \"integer\"\r\n"
				+ "        },\r\n" + "        \"code\" : {\r\n" + "          \"type\" : \"integer\",\r\n"
				+ "          \"description\" : \"Unique API code, this is an integer which must be passed and it is always equal to 6.\"\r\n"
				+ "        }\r\n" + "      },\r\n" + "      \"required\" : [ \"location\", \"code\" ]\r\n" + "    }\r\n"
				+ "  }\r\n" + "}", json);
	}

	/**
	 * Tests ToolCall serialization.
	 * 
	 * @throws JsonProcessingException
	 */
	@Test
	public void test53() throws JsonProcessingException {

		FunctionCall fc = FunctionCall.builder().name("testCall").build();
		Map<String, Object> ar = new HashMap<>();
		ar.put("name", "pippo");
		ar.put("value", 6);
		fc.setArguments(ar);
		OpenAiToolCall tc = OpenAiToolCall.builder() //
				.function(fc) //
				.Id("test_id") //
				.type(OpenAiTool.Type.FUNCTION).build();

		String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tc);
		assertEquals(
				"{\r\n  \"type\" : \"function\",\r\n  \"function\" : {\r\n    \"name\" : \"testCall\",\r\n    \"arguments\" : \"{\\r\\n  \\\"name\\\" : \\\"pippo\\\",\\r\\n  \\\"value\\\" : 6\\r\\n}\"\r\n  },\r\n  \"id\" : \"test_id\"\r\n}",
				json);
	}

	/**
	 * Tests Tool Calling.
	 * 
	 * @throws ToolInitializationException
	 */
	@Test
	public void test54() throws JsonProcessingException, ToolInitializationException {

		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {

			OpenAiChatService cs = ep.getChatService("You are an agent supporting user with weather forecasts");
			cs.setModel(MODEL);
			cs.setTools(TOOLS);
			assertTrue((cs.getTools() != null) && (cs.getTools().size() == TOOLS.size()));

			// Casual chat should not trigger any function call
			ChatCompletion reply = cs.chat("Where is Dallas, TX?");
			assertFalse(reply.hasToolCalls());
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());

			// This request should trigger a function call, optional parameters omitted
			cs.clearConversation();
			reply = cs.chat("How is the weather like in Dallas, TX?");
			assertTrue(reply.hasToolCalls());
			assertEquals(FinishReason.OTHER, reply.getFinishReason());
			assertEquals(1, reply.getToolCalls().size());
			assertEquals(TOOLS.get(0).getFunction().getName(), reply.getToolCalls().get(0).getTool().getId());
			Map<String, Object> actual = new HashMap<>();
			actual.put("location", "Dallas, TX");
			actual.put("code", 6);
			sameArguments(actual, reply.getToolCalls().get(0).getArguments());

			// This request should trigger a function call again, optional parameter added
			cs.clearConversation();
			reply = cs.chat("How is the weather like in Dallas, TX? Can you tell it in Farenheit?");
			assertTrue(reply.hasToolCalls());
			assertEquals(FinishReason.OTHER, reply.getFinishReason());
			assertEquals(1, reply.getToolCalls().size());
			assertEquals(TOOLS.get(0).getFunction().getName(), reply.getToolCalls().get(0).getTool().getId());
			actual.put("unit", "FARENHEIT");
			sameArguments(actual, reply.getToolCalls().get(0).getArguments());
		}
	}

	/**
	 * Tests ToolChoice parameter.
	 * 
	 * @throws ToolInitializationException
	 */
	@Test
	public void test55() throws JsonProcessingException, ToolInitializationException {

		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {

			OpenAiChatService cs = ep.getChatService("You are an agent supporting user with weather forecasts");
			cs.setModel(MODEL);
			cs.setTools(TOOLS);

			ChatCompletion reply = null;

			// Casual chat should not trigger any function call
			cs.clearConversation();
			cs.getDefaultReq().setToolChoice(ToolChoice.AUTO);
			reply = cs.chat("Where is Dallas, TX?");
			assertFalse(reply.hasToolCalls());
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());

			// This request should trigger a function call
			cs.clearConversation();
			cs.getDefaultReq().setToolChoice(ToolChoice.AUTO);
			reply = cs.chat("How is the weather like in Dallas, TX?");
			assertTrue(reply.hasToolCalls());
			assertEquals(FinishReason.OTHER, reply.getFinishReason());

			// This should inhibit function call
			cs.clearConversation();
			cs.getDefaultReq().setToolChoice(ToolChoice.NONE);
			reply = cs.chat("How is the weather like in Dallas, TX?");
			assertFalse(reply.hasToolCalls());
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());

			// This should force call
			cs.clearConversation();
			cs.getDefaultReq().setToolChoice(new ToolChoice(TOOLS.get(0).getFunction()));
			reply = cs.chat("Where is Dallas, TX?");
			assertTrue(reply.hasToolCalls());
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());
		}
	}

	/**
	 * Tests get/setDefaultTools().
	 * 
	 * @throws ToolInitializationException
	 */
	@Test
	public void test56() throws JsonProcessingException, ToolInitializationException {

		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {

			OpenAiChatService cs = ep.getChatService("You are an agent supporting user with weather forecasts");
			cs.setModel(MODEL);

			// No function calls here, as there is none
			cs.clearConversation();
			cs.setTools(null);
			assertEquals(0, cs.getTools().size());
			ChatCompletion reply = cs.chat("How is the weather like in Dallas, TX?");
			assertFalse(reply.hasToolCalls());
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());

			// No function calls here, as there is none
			cs.clearConversation();
			cs.setTools(new ArrayList<>());
			cs.getDefaultReq().setFunctionCall(null);
			assertEquals(0, cs.getTools().size());
			reply = cs.chat("How is the weather like in Dallas, TX?");
			assertFalse(reply.hasToolCalls());
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());
		}
	}

	/**
	 * Tests parallel function calls.
	 * 
	 * @param args
	 * @throws ToolInitializationException
	 */
	@Test
	public void test57() throws ToolInitializationException {

		try (OpenAiEndpoint endpoint = new OpenAiEndpoint()) {

			OpenAiChatService cs = endpoint.getChatService("You are an helpful assistant.");
			cs.setModel(MODEL);
			cs.setTools(TOOLS);

			// This should generate a single call for 2 tools
			ChatCompletion reply = cs.chat("What is the temperature in London and Zurich?");
			assertTrue(reply.hasToolCalls());
			assertEquals(FinishReason.OTHER, reply.getFinishReason());
			assertEquals(2, reply.getToolCalls().size());

			// Test responding to both
			List<ToolCallResult> results = new ArrayList<>();
			results.add(new ToolCallResult(reply.getToolCalls().get(0), "10°C"));
			results.add(new ToolCallResult(reply.getToolCalls().get(1), "20°C"));
			reply = cs.chat(results);
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());
			assertTrue(reply.getText().contains("10"));
			assertTrue(reply.getText().contains("20"));
			assertEquals(5, cs.getHistory().size());
		}
	}

	private static void sameArguments(Map<String, Object> reference, Map<String, Object> actual) {
		assertTrue(reference.size() <= actual.size()); // Sometimes, it does send optional parameters

		for (Entry<String, Object> e : reference.entrySet())
			assertEquals(e.getValue(), actual.get(e.getKey()));
	}

}
