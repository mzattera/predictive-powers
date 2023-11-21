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

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.mzattera.predictivepowers.openai.client.chat.Function;
import io.github.mzattera.predictivepowers.openai.client.chat.FunctionCall;
import io.github.mzattera.predictivepowers.openai.client.chat.FunctionChoice;
import io.github.mzattera.predictivepowers.openai.client.chat.Tool;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiModelData.SupportedCallType;
import io.github.mzattera.predictivepowers.services.TextCompletion.FinishReason;

/**
 * Test the OpenAI (old) function calling API.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class FunctionCallTest {

	private final static String MODEL = "gpt-3.5-turbo-0613";

	private final static ObjectMapper mapper = new ObjectMapper();

	public enum TemperatureUnits {
		CELSIUS, FARENHEIT
	};

	public static class GetCurrentWeatherParameters {

		@JsonProperty(required = true)
		@JsonPropertyDescription("The city and state, e.g. San Francisco, CA")
		public String location;

		@JsonPropertyDescription("Temperature unit (Celsius or Farenheit). This is optional.")
		public TemperatureUnits unit;

		public Integer fooParameter;

		@JsonPropertyDescription("Unique API code, this is an integer which must be passed and it is always equal to 6.")
		public int code;
	}

	private final static List<Tool> TOOLS = new ArrayList<>();
	static {
		TOOLS.add(new Tool( //
				Function.builder() //
						.name("get_current_weather") //
						.description("Get the current weather in a given location.") //
						.parameters(GetCurrentWeatherParameters.class).build() //
		));
	}

	/**
	 * Tests model parameters.
	 */
	@Test
	public void test1() throws JsonProcessingException {

		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {
			assertEquals(SupportedCallType.FUNCTIONS, ep.getModelService().getSupportedCall(MODEL));
		}
	}

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
		assertEquals("{\n" + "  \"name\" : \"get_current_weather\",\n"
				+ "  \"description\" : \"Get the current weather in a given location.\",\n" + "  \"parameters\" : {\n"
				+ "    \"$schema\" : \"http://json-schema.org/draft-04/schema#\",\n"
				+ "    \"title\" : \"Get Current Weather Parameters\",\n" + "    \"type\" : \"object\",\n"
				+ "    \"additionalProperties\" : false,\n" + "    \"properties\" : {\n" + "      \"location\" : {\n"
				+ "        \"type\" : \"string\",\n"
				+ "        \"description\" : \"The city and state, e.g. San Francisco, CA\"\n" + "      },\n"
				+ "      \"unit\" : {\n" + "        \"type\" : \"string\",\n"
				+ "        \"enum\" : [ \"CELSIUS\", \"FARENHEIT\" ],\n"
				+ "        \"description\" : \"Temperature unit (Celsius or Farenheit). This is optional.\"\n"
				+ "      },\n" + "      \"fooParameter\" : {\n" + "        \"type\" : \"integer\"\n" + "      },\n"
				+ "      \"code\" : {\n" + "        \"type\" : \"integer\",\n"
				+ "        \"description\" : \"Unique API code, this is an integer which must be passed and it is always equal to 6.\"\n"
				+ "      }\n" + "    },\n" + "    \"required\" : [ \"location\", \"code\" ]\n" + "  }\n" + "}", json);
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
		assertEquals(
				"{\n" + "  \"name\" : \"testCall\",\n"
						+ "  \"arguments\" : \"{\\n  \\\"name\\\" : \\\"pippo\\\",\\n  \\\"value\\\" : 6\\n}\"\n" + "}",
				json);
	}

	/**
	 * Tests Function Calling.
	 */
	@Test
	public void test54() throws JsonProcessingException {

		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {

			OpenAiChatService cs = ep.getChatService("You are an agent supporting user with weather forecasts");
			cs.setModel(MODEL);
			cs.setDefaulTools(TOOLS);
			assertTrue((cs.getDefaulTools() != null) && (cs.getDefaulTools().size() == TOOLS.size()));

			// Casual chat should not trigger any function call
			OpenAiTextCompletion reply = cs.chat("Where is Dallas TX?");
			assertFalse(reply.hasToolCalls());
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());

			// This request should trigger a function call, optional parameters omitted
			cs.clearConversation();
			reply = cs.chat("How is the weather like in Dallas TX?");
			assertTrue(reply.hasToolCalls());
			assertEquals(FinishReason.FUNCTION_CALL, reply.getFinishReason());
			assertEquals(1, reply.getToolCalls().size());
			assertEquals(TOOLS.get(0).getFunction().getName(), reply.getToolCalls().get(0).getFunction().getName());
			Map<String, Object> actual = new HashMap<>();
			actual.put("location", "Dallas, TX");
			actual.put("code", 6);
			sameArguments(actual, reply.getToolCalls().get(0).getFunction().getArguments());

			// This request should trigger a function call again, optional parameter added
			cs.clearConversation();
			reply = cs.chat("How is the weather like in Dallas TX? Can you tell it in Farenheit?");
			assertTrue(reply.hasToolCalls());
			assertEquals(FinishReason.FUNCTION_CALL, reply.getFinishReason());
			assertEquals(1, reply.getToolCalls().size());
			assertEquals(TOOLS.get(0).getFunction().getName(), reply.getToolCalls().get(0).getFunction().getName());
			actual.put("unit", "FARENHEIT");
			sameArguments(actual, reply.getToolCalls().get(0).getFunction().getArguments());
		}
	}

	/**
	 * Tests FunctionChoice parameter.
	 */
	@Test
	public void test55() throws JsonProcessingException {

		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {

			OpenAiChatService cs = ep.getChatService("You are an agent supporting user with weather forecasts");
			cs.setModel(MODEL);
			cs.setDefaulTools(TOOLS);

			OpenAiTextCompletion reply = null;

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
			assertEquals(FinishReason.FUNCTION_CALL, reply.getFinishReason());

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
	 */
	@Test
	public void test56() throws JsonProcessingException {

		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {

			OpenAiChatService cs = ep.getChatService("You are an agent supporting user with weather forecasts");
			cs.setModel(MODEL);

			// No function calls here, as there is none
			cs.clearConversation();
			cs.setDefaulTools(null);
			assertNull(cs.getDefaulTools());
			OpenAiTextCompletion reply = cs.chat("How is the weather like in Dallas, TX?");
			assertFalse(reply.hasToolCalls());
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());

			// No function calls here, as there is none
			cs.clearConversation();
			cs.setDefaulTools(new ArrayList<>());
			cs.getDefaultReq().setFunctionCall(null);
			assertNull(cs.getDefaulTools());
			reply = cs.chat("How is the weather like in Dallas, TX?");
			assertFalse(reply.hasToolCalls());
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());
		}
	}

	private static void sameArguments(Map<String, Object> reference, Map<String, Object> actual) {
		assertTrue(reference.size() <= actual.size()); // Sometimes, it does send optional parameters

		for (Entry<String, Object> e : reference.entrySet())
			assertEquals(e.getValue(), actual.get(e.getKey()));
	}

}
