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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import io.github.mzattera.predictivepowers.services.TextCompletion;
import io.github.mzattera.predictivepowers.services.TextCompletion.FinishReason;

/**
 * Test the OpenAI chat API & Service
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class OpenAiChatServiceTest {

	/**
	 * Check completions not affecting history.
	 */
	@Test
	public void test01() {
		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {
			OpenAiChatService cs = ep.getChatService();

			// Personality
			String personality = "You are a smart and nice agent.";
			cs.setPersonality(personality);
			assertEquals(cs.getPersonality(), personality);

			// In completion, we do not consider history, but we consider personality.
			cs.getHistory().add(new ChatMessage(ChatMessage.Role.USER, "test"));
			assertEquals(1, cs.getHistory().size());
			String question = "How high is Mt.Everest?";
			TextCompletion resp = cs.complete(question);
			assertEquals(resp.getFinishReason(), TextCompletion.FinishReason.COMPLETED);
			assertEquals(1, cs.getHistory().size());
			assertEquals(cs.getHistory().get(0).getContent(), "test");
			assertEquals(cs.getDefaultReq().getMessages().size(), 2);
			assertEquals(cs.getDefaultReq().getMessages().get(0).getRole(), ChatMessage.Role.SYSTEM);
			assertEquals(cs.getDefaultReq().getMessages().get(0).getContent(), personality);
			assertEquals(cs.getDefaultReq().getMessages().get(1).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getDefaultReq().getMessages().get(1).getContent(), question);
			assertEquals(cs.getDefaultReq().getMaxTokens(), null);

			cs.clearConversation();
			assertEquals(cs.getHistory().size(), 0);
		} // Close endpoint
	}

	/**
	 * Check chat and history management.
	 */
	@Test
	public void test02() {
		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {
			OpenAiChatService cs = ep.getChatService();

			// Personality, history length and conversation steps limits ////////////

			// Personality
			String personality = "You are a smart and nice agent.";
			cs.setPersonality(personality);
			assertEquals(cs.getPersonality(), personality);

			// Fake history
			for (int i = 0; i < 10; ++i) {
				cs.getHistory().add(new ChatMessage(ChatMessage.Role.USER, "" + i));
			}

			cs.setMaxHistoryLength(3);
			cs.setMaxConversationSteps(2);
			assertEquals(cs.getMaxConversationTokens(),
					Math.max(ep.getModelService().getContextSize(cs.getDefaultReq().getModel()), 2046) * 3 / 4);

			String question = "How high is Mt.Everest?";
			TextCompletion resp = cs.chat(question);
			assertEquals(resp.getFinishReason(), TextCompletion.FinishReason.COMPLETED);
			assertEquals(cs.getHistory().size(), 3);
			assertEquals(cs.getHistory().get(0).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getHistory().get(0).getContent(), "" + 9);
			assertEquals(cs.getHistory().get(1).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getHistory().get(1).getContent(), question);
			assertEquals(cs.getHistory().get(2).getRole(), ChatMessage.Role.BOT);
			assertEquals(cs.getHistory().get(2).getContent(), resp.getText());
			assertEquals(4, cs.getDefaultReq().getMessages().size());
			assertEquals(cs.getDefaultReq().getMessages().get(0).getRole(), ChatMessage.Role.SYSTEM);
			assertEquals(cs.getDefaultReq().getMessages().get(0).getContent(), personality);
			assertEquals(cs.getDefaultReq().getMessages().get(1).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getDefaultReq().getMessages().get(1).getContent(), "" + 8);
			assertEquals(cs.getDefaultReq().getMessages().get(2).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getDefaultReq().getMessages().get(2).getContent(), "" + 9);
			assertEquals(cs.getDefaultReq().getMessages().get(3).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getDefaultReq().getMessages().get(3).getContent(), question);
			assertEquals(cs.getDefaultReq().getMaxTokens(), null);

			// NO personality, history length and conversation steps limits ////////////
			// Also testing maxTokens

			// Fake history
			cs.clearConversation();
			for (int i = 0; i < 10; ++i) {
				cs.getHistory().add(new ChatMessage(ChatMessage.Role.USER, "" + i));
			}
			cs.setPersonality(null);
			cs.setMaxNewTokens(100);

			resp = cs.chat(question);
			assertEquals(resp.getFinishReason(), TextCompletion.FinishReason.COMPLETED);
			assertEquals(cs.getHistory().size(), 3);
			assertEquals(cs.getHistory().get(0).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getHistory().get(0).getContent(), "" + 9);
			assertEquals(cs.getHistory().get(1).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getHistory().get(1).getContent(), question);
			assertEquals(cs.getHistory().get(2).getRole(), ChatMessage.Role.BOT);
			assertEquals(cs.getHistory().get(2).getContent(), resp.getText());
			assertEquals(3, cs.getDefaultReq().getMessages().size());
			assertEquals(cs.getDefaultReq().getMessages().get(0).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getDefaultReq().getMessages().get(0).getContent(), "" + 8);
			assertEquals(cs.getDefaultReq().getMessages().get(1).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getDefaultReq().getMessages().get(1).getContent(), "" + 9);
			assertEquals(cs.getDefaultReq().getMessages().get(2).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getDefaultReq().getMessages().get(2).getContent(), question);
			assertEquals(cs.getDefaultReq().getMaxTokens(), 100);

			// Personality, history length and conversation tokens limits ////////////

			// Fake history
			cs.clearConversation();
			for (int i = 0; i < 10; ++i) {
				cs.getHistory().add(new ChatMessage(ChatMessage.Role.USER, "" + i));
			}
			cs.setPersonality(personality);
			cs.setMaxNewTokens(null);
			cs.setMaxHistoryLength(3);
			cs.setMaxConversationSteps(9999);
			cs.setMaxConversationTokens(1);

			resp = cs.chat(question);
			assertEquals(resp.getFinishReason(), TextCompletion.FinishReason.COMPLETED);
			assertEquals(cs.getHistory().size(), 3);
			assertEquals(cs.getHistory().get(0).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getHistory().get(0).getContent(), "" + 9);
			assertEquals(cs.getHistory().get(1).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getHistory().get(1).getContent(), question);
			assertEquals(cs.getHistory().get(2).getRole(), ChatMessage.Role.BOT);
			assertEquals(cs.getHistory().get(2).getContent(), resp.getText());
			assertEquals(2, cs.getDefaultReq().getMessages().size());
			assertEquals(cs.getDefaultReq().getMessages().get(0).getRole(), ChatMessage.Role.SYSTEM);
			assertEquals(cs.getDefaultReq().getMessages().get(0).getContent(), personality);
			assertEquals(cs.getDefaultReq().getMessages().get(1).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getDefaultReq().getMessages().get(1).getContent(), question);
			assertEquals(cs.getDefaultReq().getMaxTokens(), null);

			// NO personality, history length and conversation tokens limits ////////////

			// Fake history
			cs.clearConversation();
			for (int i = 0; i < 10; ++i) {
				cs.getHistory().add(new ChatMessage(ChatMessage.Role.USER, "" + i));
			}
			cs.setPersonality(null);
			cs.setMaxNewTokens(null);
			cs.setMaxHistoryLength(3);
			cs.setMaxConversationSteps(9999);
			cs.setMaxConversationTokens(1);

			resp = cs.chat(question);
			assertEquals(resp.getFinishReason(), TextCompletion.FinishReason.COMPLETED);
			assertEquals(cs.getHistory().size(), 3);
			assertEquals(cs.getHistory().get(0).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getHistory().get(0).getContent(), "" + 9);
			assertEquals(cs.getHistory().get(1).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getHistory().get(1).getContent(), question);
			assertEquals(cs.getHistory().get(2).getRole(), ChatMessage.Role.BOT);
			assertEquals(cs.getHistory().get(2).getContent(), resp.getText());
			assertEquals(1, cs.getDefaultReq().getMessages().size());
			assertEquals(cs.getDefaultReq().getMessages().get(0).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getDefaultReq().getMessages().get(0).getContent(), question);
			assertEquals(cs.getDefaultReq().getMaxTokens(), null);

			// Completion with no personality
			cs.setPersonality(null);
			cs.setMaxNewTokens(null);
			resp = cs.complete(question);
			assertEquals(resp.getFinishReason(), TextCompletion.FinishReason.COMPLETED);
			assertEquals(cs.getHistory().size(), 3);
			assertEquals(1, cs.getDefaultReq().getMessages().size());
			assertEquals(cs.getDefaultReq().getMessages().get(0).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getDefaultReq().getMessages().get(0).getContent(), question);
			assertEquals(cs.getDefaultReq().getMaxTokens(), null);

		} // Close endpoint
	}

	/**
	 * Check chat and history management with exception.
	 */
	@Test
	public void test03() {
		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {
			OpenAiChatService cs = ep.getChatService();

			// Personality, history length and conversation steps limits ////////////

			// Personality
			cs.setPersonality(null);

			// Fake history
			for (int i = 0; i < 10; ++i) {
				cs.getHistory().add(new ChatMessage(ChatMessage.Role.USER, "" + i));
			}

			cs.setMaxHistoryLength(1);
			cs.setModel("gpt-bananas");

			String question = "How high is Mt.Everest?";

			try {
				cs.chat(question);
			} catch (Exception e) {
				// Should fail because context wrong model name
			}

			// If chat fails, history is not changed
			assertEquals(10, cs.getHistory().size());
		} // Close endpoint
	}

	/**
	 * Getters and setters
	 */
	@Test
	public void test04() {
		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {
			OpenAiChatService s = ep.getChatService();
			String m = s.getModel();
			assertNotNull(m);
			s.setModel("pippo");
			assertEquals("pippo", s.getModel());
			s.setModel(m);

			assertNull(s.getTopK());
			s.setTopK(null);
			assertNull(s.getTopK());
			assertThrows(UnsupportedOperationException.class, () -> s.setTopK(1));

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
			s.setMaxNewTokens(15);
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// FUNCTION CALLING TESTS
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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
	public void test51() throws JsonProcessingException {

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
	public void test52() throws JsonProcessingException {
		Function f = Function.builder().name("getCurrentWeather").description("A test.")
				.parameters(GetCurrentWeatherParameters.class).build();

		String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(f);
		assertEquals("{\r\n" + "  \"name\" : \"getCurrentWeather\",\r\n" + "  \"description\" : \"A test.\",\r\n"
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

		OpenAiChatMessage.FunctionCall fc = OpenAiChatMessage.FunctionCall.builder().name("testCall").build();
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
	 */
	@Test
	public void test54() throws JsonProcessingException {

		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {

			OpenAiChatService cs = ep.getChatService("You are an agent supporting user with weather forecasts");

			// Casual chat should not trigger any function call
			OpenAiTextCompletion reply = cs.chat("Where is Dallas TX?");
			assertFalse(reply.isFunctionCall());
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());

			// This request should trigger a function call, optional parameters omitted
			reply = cs.chat("How is the weather like there?", functions);
			assertTrue(reply.isFunctionCall());
			assertEquals(FinishReason.FUNCTION_CALL, reply.getFinishReason());
			assertEquals(functions.get(0).getName(), reply.getFunctionCall().getName());
			Map<String, Object> actual = new HashMap<>();
			actual.put("location", "Dallas, TX");
			actual.put("code", 6);
			sameArguments(actual, reply.getFunctionCall().getArguments());

			// This request should trigger a function call again, optional parameter added
			reply = cs.chat("Can you tell that in celsius degrees?", functions);
			assertTrue(reply.isFunctionCall());
			assertEquals(FinishReason.FUNCTION_CALL, reply.getFinishReason());
			assertEquals(functions.get(0).getName(), reply.getFunctionCall().getName());
			actual.put("unit", "CELSIUS");
			sameArguments(actual, reply.getFunctionCall().getArguments());
		}
	}

	/**
	 * Tests Function Calling.
	 */
	@Test
	public void test55() throws JsonProcessingException {

		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {

			OpenAiChatService cs = ep.getChatService("You are an agent supporting user with weather forecasts");
			OpenAiTextCompletion reply = null;

			// Casual chat should not trigger any function call
			cs.clearConversation();
			cs.getDefaultReq().setFunctionCall(FunctionCallSetting.AUTO);
			reply = cs.chat("Where is Dallas TX?", functions);
			assertFalse(reply.isFunctionCall());
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());

			// This request should trigger a function call
			cs.clearConversation();
			cs.getDefaultReq().setFunctionCall(FunctionCallSetting.AUTO);
			reply = cs.chat("How is the weather like in Dallas, TX?", functions);
			assertTrue(reply.isFunctionCall());
			assertEquals(FinishReason.FUNCTION_CALL, reply.getFinishReason());

			// This should inhibit function call
			cs.clearConversation();
			cs.getDefaultReq().setFunctionCall(FunctionCallSetting.NONE);
			reply = cs.chat("How is the weather like in Dallas, TX?", functions);
			assertFalse(reply.isFunctionCall());
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());

			// This should force call
			cs.clearConversation();
			cs.getDefaultReq().setFunctionCall(new FunctionCallSetting(functions.get(0).getName()));
			reply = cs.chat("Where is Dallas, TX?");
			assertTrue(reply.isFunctionCall());
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());
		}
	}

	private static void sameArguments(Map<String, Object> reference, Map<String, Object> actual) {
		assertTrue(reference.size() <= actual.size()); // Sometimes, it does send optional parameters

		for (Entry<String, Object> e : reference.entrySet())
			assertEquals(e.getValue(), actual.get(e.getKey()));
	}

}
