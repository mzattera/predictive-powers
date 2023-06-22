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

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest.FunctionCallSetting;
import io.github.mzattera.predictivepowers.openai.client.chat.Function;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.ChatMessage;

/**
 * Tests function calling OpenAI API.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class FunctionCallTest {

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

	private final static List<Function> functions = new ArrayList<>();
	static {
			functions.add(Function.builder().name("get_current_weather")
					.description("Get the current weather in a given location.")
					.parameters(GetCurrentWeatherParameters.class).build());
	}

	/**
	 * Tests FunctionCallSetting serialization.
	 */
	@Test
	public void test01() throws JsonProcessingException {
		
		String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(FunctionCallSetting.NONE);
		assertEquals("\"none\"", json);
		
		json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(FunctionCallSetting.AUTO);
		assertEquals("\"auto\"", json);
	
		json = mapper.writeValueAsString(new FunctionCallSetting("banane"));
		assertEquals("{\"name\":\"banane\"}", json);		
	}

	/**
	 * Test Function serialization.
	 * 
	 * @throws JsonProcessingException
	 */
	@Test
	public void test02() throws JsonProcessingException {
		Function f = Function.builder().name("getCurrentWeather").description("A test.")
				.parameters(GetCurrentWeatherParameters.class).build();

		String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(f);
		
		System.out.println(json);
		
		assertEquals("{\r\n"
				+ "  \"name\" : \"getCurrentWeather\",\r\n"
				+ "  \"description\" : \"A test.\",\r\n"
				+ "  \"parameters\" : {\r\n"
				+ "    \"$schema\" : \"http://json-schema.org/draft-04/schema#\",\r\n"
				+ "    \"title\" : \"Get Current Weather Parameters\",\r\n"
				+ "    \"type\" : \"object\",\r\n"
				+ "    \"additionalProperties\" : false,\r\n"
				+ "    \"properties\" : {\r\n"
				+ "      \"location\" : {\r\n"
				+ "        \"type\" : \"string\",\r\n"
				+ "        \"description\" : \"The city and state, e.g. San Francisco, CA\"\r\n"
				+ "      },\r\n"
				+ "      \"unit\" : {\r\n"
				+ "        \"type\" : \"string\",\r\n"
				+ "        \"enum\" : [ \"CELSIUS\", \"FARENHEIT\" ],\r\n"
				+ "        \"description\" : \"Temperature unit (Celsius or Farenheit). This is optional.\"\r\n"
				+ "      },\r\n"
				+ "      \"fooParameter\" : {\r\n"
				+ "        \"type\" : \"integer\"\r\n"
				+ "      },\r\n"
				+ "      \"code\" : {\r\n"
				+ "        \"type\" : \"integer\",\r\n"
				+ "        \"description\" : \"Unique API code, this is an integer which must be passed and it is always equal to 6.\"\r\n"
				+ "      }\r\n"
				+ "    },\r\n"
				+ "    \"required\" : [ \"location\", \"code\" ]\r\n"
				+ "  }\r\n"
				+ "}",json);
	}

	/**
	 * Tests FunctionCall serialization.
	 * @throws JsonProcessingException 
	 */
	@Test
	public void test03() throws JsonProcessingException {
		
		ChatMessage.FunctionCall fc = ChatMessage.FunctionCall.builder().name("testCall").build();
		Map<String, Object> ar = new HashMap<>();
		ar.put("name", "pippo");
		ar.put("value", 6);
		fc.setArguments(ar);
		String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(fc);
		assertEquals("{\r\n"
				+ "  \"name\" : \"testCall\",\r\n"
				+ "  \"arguments\" : \"{\\r\\n  \\\"name\\\" : \\\"pippo\\\",\\r\\n  \\\"value\\\" : 6\\r\\n}\"\r\n"
				+ "}", json);
	}
	
	
	/**
	 * Tests Function Calling.
	 */
	@Test
	public void test04() throws JsonProcessingException {
		
		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {
			
			OpenAiChatService cs = ep.getChatService("You are an agent supporting user with weather forecasts");

			// Casual chat should not trigger any function call
			OpenAiTextCompletion reply = cs.chat("Where is Dallas TX?");
			assertFalse(reply.isFunctionCall());

			// This request should trigger a function call, optional parameters omitted
			reply = cs.chat("How is the weather like there?", functions);
			assertTrue(reply.isFunctionCall());
			assertEquals(functions.get(0).getName(), reply.getFunctionCall().getName());
			Map<String,Object> actual = new HashMap<>();
			actual.put("location", "Dallas, TX");
			actual.put("code", 6);
			sameArguments(actual,reply.getFunctionCall().getArguments());

			// This request should trigger a function call again, optional  parameter added
			reply = cs.chat("Can you tell that in celsius degrees?", functions);
			assertTrue(reply.isFunctionCall());
			assertEquals(functions.get(0).getName(), reply.getFunctionCall().getName());
			actual.put("unit", "CELSIUS");
			sameArguments(actual,reply.getFunctionCall().getArguments());
		}		
	}

	private static void sameArguments(Map<String, Object> reference, Map<String, Object> actual) {
		assertTrue(reference.size() <=  actual.size()); // Sometimes, it does send optional parameters

		for (Entry<String, Object> e : reference.entrySet())
			assertEquals(e.getValue(), actual.get(e.getKey()));
	}

}