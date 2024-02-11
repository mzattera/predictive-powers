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

import io.github.mzattera.predictivepowers.openai.client.chat.Function;
import io.github.mzattera.predictivepowers.openai.client.chat.FunctionCall;
import io.github.mzattera.predictivepowers.openai.client.chat.FunctionChoice;
import io.github.mzattera.predictivepowers.openai.client.chat.OpenAiTool;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiModelMetaData.SupportedCallType;
import io.github.mzattera.predictivepowers.services.AbstractTool;
import io.github.mzattera.predictivepowers.services.Agent;
import io.github.mzattera.predictivepowers.services.Capability;
import io.github.mzattera.predictivepowers.services.SimpleCapability;
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
public class FunctionCallTest {

	private final static String MODEL = "gpt-3.5-turbo-0613";

	@BeforeAll
	static void check() {

		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {
			assertEquals(SupportedCallType.FUNCTIONS, ep.getModelService().getSupportedCallType(MODEL));
		}
	}

	private final static ObjectMapper mapper = new ObjectMapper();

	// This is a function that will be accessible to the agent.
	static class GetCurrentWeatherTool extends AbstractTool {

		// This is a schema describing the function parameters
		public enum TemperatureUnits {
			CELSIUS, FARENHEIT
		};

		static class GetCurrentWeatherParameters {

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

		GetCurrentWeatherTool() {
			super("get_current_weather", //
					"Get the current weather in a given location.", //
					GetCurrentWeatherParameters.class);
		}

		@Override
		public void init(@NonNull Agent agent) {
			setInitialized(true);
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
	private final static Capability DEFAULT_CAPABILITY = new SimpleCapability(TOOLS);

	/**
	 * Tests FunctionChoice serialization.
	 */
	@Test
	public void test51() throws JsonProcessingException {

		String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(FunctionChoice.NONE);
		assertEquals("\"none\"", json);

		json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(FunctionChoice.AUTO);
		assertEquals("\"auto\"", json);

		json = mapper.writeValueAsString(new FunctionChoice("banane"));
		assertEquals("{\"name\":\"banane\"}", json);
	}

	/**
	 * Test Function serialization.
	 * 
	 * @throws JsonProcessingException
	 */
	@Test
	public void test52() throws JsonProcessingException {
		Function f = TOOLS.get(0).getFunction();

		String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(f);
		assertEquals("{\r\n" + "  \"name\" : \"get_current_weather\",\r\n"
				+ "  \"description\" : \"Get the current weather in a given location.\",\r\n"
				+ "  \"parameters\" : {\r\n" + "    \"$schema\" : \"http://json-schema.org/draft-04/schema#\",\r\n"
				+ "    \"title\" : \"Get Current Weather Parameters\",\r\n" + "    \"type\" : \"object\",\r\n"
				+ "    \"additionalProperties\" : false,\r\n" + "    \"properties\" : {\r\n"
				+ "      \"location\" : {\r\n" + "        \"type\" : \"string\",\r\n"
				+ "        \"description\" : \"The city and state, e.g. San Francisco, CA\"\r\n" + "      },\r\n"
				+ "      \"unit\" : {\r\n" + "        \"type\" : \"string\",\r\n"
				+ "        \"enum\" : [ \"CELSIUS\", \"FARENHEIT\" ],\r\n"
				+ "        \"description\" : \"Temperature unit (Celsius or Farenheit). This is optional.\"\r\n"
				+ "      },\r\n" + "      \"fooParameter\" : {\r\n" + "        \"type\" : \"integer\"\r\n"
				+ "      },\r\n" + "      \"code\" : {\r\n" + "        \"type\" : \"integer\",\r\n"
				+ "        \"description\" : \"Unique API code, this is an integer which must be passed and it is always equal to 6.\"\r\n"
				+ "      }\r\n" + "    },\r\n" + "    \"required\" : [ \"location\", \"code\" ]\r\n" + "  }\r\n" + "}",
				json);
	}

	/**
	 * Tests FunctionCall serialization.
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
		String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(fc);
		assertEquals("{\r\n" + "  \"name\" : \"testCall\",\r\n"
				+ "  \"arguments\" : \"{\\r\\n  \\\"name\\\" : \\\"pippo\\\",\\r\\n  \\\"value\\\" : 6\\r\\n}\"\r\n"
				+ "}", json);
	}

	/**
	 * Tests Function Calling.
	 * 
	 * @throws ToolInitializationException
	 */
	@Test
	public void test54() throws JsonProcessingException, ToolInitializationException {

		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {

			OpenAiChatService cs = ep.getChatService("You are an agent supporting user with weather forecasts");
			cs.setModel(MODEL);
			cs.addCapability(DEFAULT_CAPABILITY);

			// Casual chat should not trigger any function call
			ChatCompletion reply = cs.chat("Where is Dallas TX?");
			assertFalse(reply.hasToolCalls());
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());

			// This request should trigger a function call, optional parameters omitted
			cs.clearConversation();
			reply = cs.chat("How is the weather like in Dallas TX?");
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
			reply = cs.chat("How is the weather like in Dallas TX? Can you tell it in Farenheit?");
			assertTrue(reply.hasToolCalls());
			assertEquals(FinishReason.OTHER, reply.getFinishReason());
			assertEquals(1, reply.getToolCalls().size());
			assertEquals(TOOLS.get(0).getFunction().getName(), reply.getToolCalls().get(0).getTool().getId());
			actual.put("unit", "FARENHEIT");
			sameArguments(actual, reply.getToolCalls().get(0).getArguments());
		}
	}

	/**
	 * Tests FunctionChoice parameter.
	 * 
	 * @throws ToolInitializationException
	 */
	@Test
	public void test55() throws JsonProcessingException, ToolInitializationException {

		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {

			OpenAiChatService cs = ep.getChatService("You are an agent supporting user with weather forecasts");
			cs.setModel(MODEL);
			cs.addCapability(DEFAULT_CAPABILITY);

			ChatCompletion reply = null;

			// Casual chat should not trigger any function call
			cs.clearConversation();
			cs.getDefaultReq().setFunctionCall(FunctionChoice.AUTO);
			reply = cs.chat("Where is Dallas TX?");
			assertFalse(reply.hasToolCalls());
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());

			// This request should trigger a function call
			cs.clearConversation();
			cs.getDefaultReq().setFunctionCall(FunctionChoice.AUTO);
			reply = cs.chat("How is the weather like in Dallas, TX?");
			assertTrue(reply.hasToolCalls());
			assertEquals(FinishReason.OTHER, reply.getFinishReason());

			// This should inhibit function call
			cs.clearConversation();
			cs.getDefaultReq().setFunctionCall(FunctionChoice.NONE);
			reply = cs.chat("How is the weather like in Dallas, TX?");
			assertFalse(reply.hasToolCalls());
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());

			// This should force call
			cs.clearConversation();
			cs.getDefaultReq().setFunctionCall(new FunctionChoice(TOOLS.get(0).getFunction().getName()));
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
			cs.clearCapabilities();
			ChatCompletion reply = cs.chat("How is the weather like in Dallas, TX?");
			assertFalse(reply.hasToolCalls());
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());
		}
	}

	private static void sameArguments(Map<String, Object> reference, @NonNull Map<String, ? extends Object> map) {
		assertTrue(reference.size() <= map.size()); // Sometimes, it does send optional parameters

		for (Entry<String, Object> e : reference.entrySet())
			assertEquals(e.getValue(), map.get(e.getKey()));
	}

}
