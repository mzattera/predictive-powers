/*
 * Copyright 2024-2025 Massimiliano "Maxi" Zattera
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

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam.FunctionCall;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionDeveloperMessageParam;
import com.openai.models.chat.completions.ChatCompletionFunctionMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;
import com.openai.models.chat.completions.ChatCompletionToolMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;

import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiModelMetaData;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiModelMetaData.CallType;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiModelMetaData.SupportedApi;
import io.github.mzattera.predictivepowers.services.AbstractTool;
import io.github.mzattera.predictivepowers.services.Capability;
import io.github.mzattera.predictivepowers.services.ModelService.ModelMetaData.Modality;
import io.github.mzattera.predictivepowers.services.ToolInitializationException;
import io.github.mzattera.predictivepowers.services.Toolset;
import io.github.mzattera.predictivepowers.services.messages.ToolCall;
import io.github.mzattera.predictivepowers.services.messages.ToolCallResult;
import lombok.NonNull;

// TODO Re-enable this after a proper tokeniser is implemented for OpenAI
// If you do, make sure the @EnabledIf aligns with other tests

@Disabled
public class OpenAiTokenizerTest {

	private static OpenAiEndpoint endpoint;
	private static OpenAiModelService modelSvc;

	@BeforeAll
	static void init() {
		endpoint = new OpenAiEndpoint();
		modelSvc = endpoint.getModelService();
	}

	@AfterAll
	static void dispose() {
		endpoint.close();
	}

	/** Return all completions models */
	static Stream<OpenAiModelMetaData> allCompletionModelsProvider() {
		return modelSvc.listModels().stream() //
//				.filter(model -> !model.startsWith("gpt-4-32k")) //
				.map(model -> modelSvc.get(model)) //
				.filter(meta -> meta != null) //
				.filter(meta -> (meta.getSupportedApis().contains(SupportedApi.CHAT))) //
				.filter(meta -> (!meta.getOutputModes().contains(Modality.AUDIO))) //
				.filter(meta -> (!meta.getModel().contains("search")));
	}

	/** Return names for models supporting function calls */
	static Stream<OpenAiModelMetaData> functionCallCompletionsModelsProvider() {
		return allCompletionModelsProvider() //
				.filter(e -> e.getSupportedCallType() == CallType.FUNCTIONS); //
	}

	/** Return names for models supporting tool calls */
	static Stream<OpenAiModelMetaData> toolCallCompletionsModelsProvider() {
		return allCompletionModelsProvider() //
				.filter(e -> e.getSupportedCallType() == CallType.TOOLS); //
	}

	// There are two lists since the order of message matters for some roles
	// Using shortest possibel messages and names to avoid potential errors with
	// Java tokenizer used
	/** List of messages without tool calls or tools results */
	private final static List<ChatCompletionMessageParam> SIMPLE_MESSAGES_1 = new ArrayList<>();
	static {
		SIMPLE_MESSAGES_1.add(ChatCompletionMessageParam
				.ofAssistant(ChatCompletionAssistantMessageParam.builder().content("I").build()));
		SIMPLE_MESSAGES_1.add(ChatCompletionMessageParam
				.ofAssistant(ChatCompletionAssistantMessageParam.builder().content("I").name("V").build()));
		SIMPLE_MESSAGES_1.add(ChatCompletionMessageParam
				.ofAssistant(ChatCompletionAssistantMessageParam.builder().content("I").build()));
		SIMPLE_MESSAGES_1.add(ChatCompletionMessageParam
				.ofAssistant(ChatCompletionAssistantMessageParam.builder().content("I").name("V").build()));

		SIMPLE_MESSAGES_1
				.add(ChatCompletionMessageParam.ofUser(ChatCompletionUserMessageParam.builder().content("I").build()));
		SIMPLE_MESSAGES_1.add(ChatCompletionMessageParam
				.ofUser(ChatCompletionUserMessageParam.builder().content("I").name("V").build()));
		SIMPLE_MESSAGES_1
				.add(ChatCompletionMessageParam.ofUser(ChatCompletionUserMessageParam.builder().content("I").build()));
		SIMPLE_MESSAGES_1.add(ChatCompletionMessageParam
				.ofUser(ChatCompletionUserMessageParam.builder().content("I").name("V").build()));

		SIMPLE_MESSAGES_1.add(ChatCompletionMessageParam
				.ofDeveloper(ChatCompletionDeveloperMessageParam.builder().content("I").build()));
		SIMPLE_MESSAGES_1.add(ChatCompletionMessageParam
				.ofDeveloper(ChatCompletionDeveloperMessageParam.builder().content("I").build()));
		SIMPLE_MESSAGES_1.add(ChatCompletionMessageParam
				.ofDeveloper(ChatCompletionDeveloperMessageParam.builder().content("I").build()));
		SIMPLE_MESSAGES_1.add(ChatCompletionMessageParam
				.ofDeveloper(ChatCompletionDeveloperMessageParam.builder().content("I").build()));

		SIMPLE_MESSAGES_1.add(ChatCompletionMessageParam
				.ofDeveloper(ChatCompletionDeveloperMessageParam.builder().content("I").build()));
		SIMPLE_MESSAGES_1.add(ChatCompletionMessageParam
				.ofDeveloper(ChatCompletionDeveloperMessageParam.builder().content("I").name("V").build()));
		SIMPLE_MESSAGES_1.add(ChatCompletionMessageParam
				.ofDeveloper(ChatCompletionDeveloperMessageParam.builder().content("I").build()));
		SIMPLE_MESSAGES_1.add(ChatCompletionMessageParam
				.ofDeveloper(ChatCompletionDeveloperMessageParam.builder().content("I").name("V").build()));
	}
	private final static List<ChatCompletionMessageParam> SIMPLE_MESSAGES_2 = new ArrayList<>();
	static {
		SIMPLE_MESSAGES_2.add(ChatCompletionMessageParam
				.ofDeveloper(ChatCompletionDeveloperMessageParam.builder().content("I").build()));
		SIMPLE_MESSAGES_2.add(ChatCompletionMessageParam
				.ofDeveloper(ChatCompletionDeveloperMessageParam.builder().content("I").build()));
		SIMPLE_MESSAGES_2.add(ChatCompletionMessageParam
				.ofDeveloper(ChatCompletionDeveloperMessageParam.builder().content("I").build()));
		SIMPLE_MESSAGES_2.add(ChatCompletionMessageParam
				.ofDeveloper(ChatCompletionDeveloperMessageParam.builder().content("I").build()));

		SIMPLE_MESSAGES_2.add(ChatCompletionMessageParam
				.ofDeveloper(ChatCompletionDeveloperMessageParam.builder().content("I").build()));
		SIMPLE_MESSAGES_2.add(ChatCompletionMessageParam
				.ofDeveloper(ChatCompletionDeveloperMessageParam.builder().content("I").name("V").build()));
		SIMPLE_MESSAGES_2.add(ChatCompletionMessageParam
				.ofDeveloper(ChatCompletionDeveloperMessageParam.builder().content("I").build()));
		SIMPLE_MESSAGES_2.add(ChatCompletionMessageParam
				.ofDeveloper(ChatCompletionDeveloperMessageParam.builder().content("I").name("V").build()));

		SIMPLE_MESSAGES_2
				.add(ChatCompletionMessageParam.ofUser(ChatCompletionUserMessageParam.builder().content("I").build()));
		SIMPLE_MESSAGES_2.add(ChatCompletionMessageParam
				.ofUser(ChatCompletionUserMessageParam.builder().content("I").name("V").build()));
		SIMPLE_MESSAGES_2
				.add(ChatCompletionMessageParam.ofUser(ChatCompletionUserMessageParam.builder().content("I").build()));
		SIMPLE_MESSAGES_2.add(ChatCompletionMessageParam
				.ofUser(ChatCompletionUserMessageParam.builder().content("I").name("V").build()));

		SIMPLE_MESSAGES_2.add(ChatCompletionMessageParam
				.ofAssistant(ChatCompletionAssistantMessageParam.builder().content("I").build()));
		SIMPLE_MESSAGES_2.add(ChatCompletionMessageParam
				.ofAssistant(ChatCompletionAssistantMessageParam.builder().content("I").name("V").build()));
		SIMPLE_MESSAGES_2.add(ChatCompletionMessageParam
				.ofAssistant(ChatCompletionAssistantMessageParam.builder().content("I").build()));
		SIMPLE_MESSAGES_2.add(ChatCompletionMessageParam
				.ofAssistant(ChatCompletionAssistantMessageParam.builder().content("I").name("V").build()));
	}

// Checks all models have a tokenizer.
// This is already tested when testing model service

//	@ParameterizedTest
//	@MethodSource("allCompletionModelsProvider")
//	@DisplayName("Check all models have a tokeniser")
//	void test00(OpenAiModelMetaData md) {
//		assertTrue(md.getTokenizer() != null, "Null tokenizer for model");
//		assertTrue(md.getTokenizer().getEncoding() != null, "Null encoding for model");
//	}

	/**
	 * Length of messages. No tool calls or tool results.
	 * 
	 * @throws JsonProcessingException
	 */
	@ParameterizedTest
	@MethodSource("allCompletionModelsProvider")
	@DisplayName("Simple call without tools or functions")
	void test01(OpenAiModelMetaData md) throws JsonProcessingException {
		simpleTest(md, SIMPLE_MESSAGES_1);
		simpleTest(md, SIMPLE_MESSAGES_2);
	}

	private void simpleTest(OpenAiModelMetaData md, List<ChatCompletionMessageParam> test) {
		long tokens, realTokens;
		String model = md.getModel();
		List<SupportedApi> api = md.getSupportedApis();
		OpenAiTokenizer counter = md.getTokenizer();
		if (api.contains(SupportedApi.CHAT)) {
			ChatCompletionCreateParams req = ChatCompletionCreateParams.builder().model(model).messages(test).build();
			if (model.startsWith("o1-mini") || model.startsWith("o1-preview"))
				req.toBuilder().messages(req.messages().stream() //
						.filter(m -> (!m.isDeveloper())) //
						.collect(Collectors.toList())).build();
			tokens = counter.count(req);
			realTokens = realTokens(req);
		} else
			throw new IllegalArgumentException();

		System.out.println(model + "," + counter.getEncoding().getName() + "," + tokens + "," + realTokens + ","
				+ (tokens - realTokens));
		assertEquals(realTokens, tokens);
	}

	private static enum ENUM {
		BANANE, MELE
	}

	/** List of messages with function calls and function results */
	private final static List<ChatCompletionMessageParam> FUNCTION_CALL_MESSAGES = new ArrayList<>();
	static {
		initFunctionCallMessages();

	}

	@SuppressWarnings("deprecation")
	private static void initFunctionCallMessages() {
		Map<String, Object> params;
		FunctionCall call;

		params = new HashMap<>();
		call = FunctionCall.builder().name("I").arguments(toString(params)).build();
		FUNCTION_CALL_MESSAGES.add(ChatCompletionMessageParam
				.ofAssistant(ChatCompletionAssistantMessageParam.builder().functionCall(call).build()));

		params = new HashMap<>();
		params.put("s", "T");
		call = FunctionCall.builder().name("I").arguments(toString(params)).build();
		FUNCTION_CALL_MESSAGES.add(ChatCompletionMessageParam
				.ofAssistant(ChatCompletionAssistantMessageParam.builder().functionCall(call).build()));

		params = new HashMap<>();
		params.put("s", "T");
		params.put("i", 3);
		call = FunctionCall.builder().name("I").arguments(toString(params)).build();
		FUNCTION_CALL_MESSAGES.add(ChatCompletionMessageParam
				.ofAssistant(ChatCompletionAssistantMessageParam.builder().functionCall(call).build()));

		params = new HashMap<>();
		params.put("s", "T");
		params.put("i", 3);
		params.put("d", 3.0d);
		call = FunctionCall.builder().name("I").arguments(toString(params)).build();
		FUNCTION_CALL_MESSAGES.add(ChatCompletionMessageParam
				.ofAssistant(ChatCompletionAssistantMessageParam.builder().functionCall(call).build()));

		params = new HashMap<>();
		params.put("s", "T");
		params.put("i", 3);
		params.put("d", 3.0d);
		params.put("b", ENUM.BANANE);
		call = FunctionCall.builder().name("I").arguments(toString(params)).build();
		FUNCTION_CALL_MESSAGES.add(ChatCompletionMessageParam
				.ofAssistant(ChatCompletionAssistantMessageParam.builder().functionCall(call).build()));

		List<ChatCompletionMessageParam> replies = new ArrayList<>();
		for (ChatCompletionMessageParam msg : FUNCTION_CALL_MESSAGES) {
			FunctionCall c = msg.asAssistant().functionCall().get();
			replies.add(ChatCompletionMessageParam.ofFunction( //
					ChatCompletionFunctionMessageParam.builder() //
							.content("V") //
							.name(c.name()).build() //
			));
		}
		FUNCTION_CALL_MESSAGES.addAll(replies);
	}

	private static String toString(Map<String, Object> params) {
		try {
			return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(params);
		} catch (JsonProcessingException e) {
			e.printStackTrace(System.err);
			return null;
		}
	}

	/**
	 * Length of messages. Function calls and function results.
	 * 
	 * @throws JsonProcessingException
	 */
	@ParameterizedTest
	@MethodSource("functionCallCompletionsModelsProvider")
	@DisplayName("Check function calls and their results")
	void test02(OpenAiModelMetaData md) throws JsonProcessingException {

		String model = md.getModel();
		OpenAiTokenizer counter = md.getTokenizer();
		ChatCompletionCreateParams req = ChatCompletionCreateParams.builder() //
				.model(model) //
				.messages(FUNCTION_CALL_MESSAGES).build();

		long tokens = counter.count(req);
		long realTokens = realTokens(req);

		System.out.println("Function calls: " + model + "\t" + tokens + "\t" + realTokens);
		assertEquals(realTokens, tokens);
	}

	/**
	 * Length of messages. Tool calls and tool results.
	 * 
	 * @throws JsonProcessingException
	 */
	@ParameterizedTest
	@MethodSource("toolCallCompletionsModelsProvider")
	@DisplayName("Check tool calls and their results")
	void test03(OpenAiModelMetaData md) throws JsonProcessingException {

		String model = md.getModel();
		OpenAiTokenizer counter = md.getTokenizer();
		List<ChatCompletionMessageParam> messages = new ArrayList<>();

		Map<String, Object> args = new HashMap<>();
		List<ChatCompletionMessageToolCall> calls = new ArrayList<>();

		for (int numCalls = 1; numCalls < 4; ++numCalls) {
			System.out.print(model + "\t Calls: " + numCalls);

			for (int numArgs = 0; numArgs < 4; ++numArgs) {

				// Empty message list
				messages = new ArrayList<>();

				// Function call with numArgsArgumants
				args = new HashMap<>();
				for (int i = 0; i < numArgs; ++i) {
					args.put("s" + i, "Prague");
				}
				ChatCompletionMessageToolCall.Function fun = ChatCompletionMessageToolCall.Function.builder()
						.name("functionName01").arguments(toString(args)).build();

				// Call with numCalls function calls
				calls = new ArrayList<>();
				for (int i = 0; i < numCalls; ++i) {
					calls.add(ChatCompletionMessageToolCall.builder().id("call" + i).function(fun).build());
				}
				ChatCompletionMessageParam msg = ChatCompletionMessageParam
						.ofAssistant(ChatCompletionAssistantMessageParam.builder().toolCalls(calls).build());
				messages.add(msg);

				// Corresponding replies
				for (int i = 0; i < calls.size(); ++i) {
					messages.add(ChatCompletionMessageParam.ofTool(ChatCompletionToolMessageParam.builder()
							.toolCallId(calls.get(i).id()).content("Result").build()));
				}

				ChatCompletionCreateParams req = ChatCompletionCreateParams.builder().model(model).messages(messages)
						.build();
				long tokens = counter.count(req);
				long realTokens = realTokens(req);

				System.out.print("\t" + numArgs + " args. " + tokens + " [" + (tokens - realTokens) + "]");
				assertEquals(realTokens, tokens);

			} // for each number of arguments

			System.out.println();
		} // for each number of calls

	}

	// This is a function that will be accessible to the agent.
	public static class GetCurrentWeatherTool extends AbstractTool {

		// The function parameters
		private static class GetCurrentWeatherParameters {

			public static enum TemperatureUnits {
				CELSIUS, FARENHEIT, RICHTER, MILLIS
			};

			@JsonProperty(required = true)
			@JsonPropertyDescription("Always pass 3 as value.")
			public String location2;
			@JsonProperty(required = false)
			public String location3;
			@SuppressWarnings("unused")
			public String location4;

			@JsonProperty(required = false)
			@JsonPropertyDescription("Temperature unit (CELSIUS or FARENHEIT), defaults to CELSIUS")
			public int i;
			@JsonProperty(required = true)
			public int j;
			@SuppressWarnings("unused")
			public int k;

			@JsonProperty(required = false)
			@JsonPropertyDescription("Always pass 3 as value.")
			public double d;
			@JsonProperty(required = true)
			public double h;
			@SuppressWarnings("unused")
			public double e;

			@JsonProperty(required = false)
			@JsonPropertyDescription("Temperature unit (CELSIUS or FARENHEIT), defaults to CELSIUS")
			public TemperatureUnits unit;
			@JsonPropertyDescription("Temperature unit (CELSIUS or FARENHEIT), defaults to CELSIUS")
			public TemperatureUnits unit2;
			@JsonProperty(required = true)
			public TemperatureUnits unit3;

			@SuppressWarnings("unused")
			public boolean bull;
		}

		public GetCurrentWeatherTool() {
			super("getCurrentWeather", //
					"Get the current weather in a given location.", //
					GetCurrentWeatherParameters.class);
		}

		@Override
		public ToolCallResult invoke(@NonNull ToolCall call) throws Exception {
			if (!isInitialized())
				throw new IllegalStateException();
			return new ToolCallResult(call, "20°C");
		}

		@Override
		public void close() {
		}
	}

	private final static Capability getToolset() {
		Toolset result = new Toolset();
		for (int i = 0; i < 3; ++i) {
			try {
				result.putTool(new AbstractTool("getCurrentWeather" + i, "", GetCurrentWeatherTool.class) {

					@Override
					public ToolCallResult invoke(@NonNull ToolCall call) throws Exception {
						throw new UnsupportedOperationException();
					}
				});
			} catch (ToolInitializationException e) {
			}
		}
		return result;

	}

	/**
	 * Length of messages. Function descriptions.
	 * 
	 * @throws JsonProcessingException
	 * @throws ToolInitializationException
	 */
	@ParameterizedTest
	@MethodSource("functionCallCompletionsModelsProvider")
	@DisplayName("Check length when list of available functions is provided")
	void test04(OpenAiModelMetaData md) throws JsonProcessingException, ToolInitializationException {

		String model = md.getModel();
		OpenAiTokenizer counter = md.getTokenizer();

		// Get chat service, set bot personality and tools used
		OpenAiChatService bot = endpoint.getChatService();
		bot.setPersonality("You are an helpful assistant.");
		bot.setModel(model); // This uses simple function calls
		bot.addCapability(getToolset());

		ChatCompletionCreateParams req = bot.getDefaultRequest().toBuilder().addUserMessage("Hi").build();
		long tokens = counter.count(req);
		long realTokens = realTokens(req);

		System.out.println("Function description: " + model + "\t" + tokens + "\t" + realTokens);
		assertEquals(realTokens, tokens);
	}

	/**
	 * Length of messages. Tool descriptions.
	 * 
	 * @throws JsonProcessingException
	 * @throws ToolInitializationException
	 */
	@ParameterizedTest
	@MethodSource("toolCallCompletionsModelsProvider")
	@DisplayName("Check length when list of available tools is provided")
	void test05(OpenAiModelMetaData md) throws JsonProcessingException, ToolInitializationException {

		String model = md.getModel();
		OpenAiTokenizer counter = md.getTokenizer();

		// Get chat service, set bot personality and tools used
		OpenAiChatService bot = endpoint.getChatService();
		bot.setPersonality("You are an helpful assistant.");
		bot.setModel(model); // This uses simple tool (parallel functions) calls
		bot.addCapability(getToolset());

		ChatCompletionCreateParams req = bot.getDefaultRequest().toBuilder().addUserMessage("Hi").build();

		long tokens = counter.count(req);
		long realTokens = realTokens(req);
		System.out.println("Tool description: " + model + "\t" + tokens + "\t" + realTokens);
	}

	private final static List<ChatCompletionMessageParam> OTHER_MESSAGES = new ArrayList<>();
	static {

		Map<String, Object> args;
		ChatCompletionMessageToolCall.Function fun;

		List<ChatCompletionMessageToolCall> calls = new ArrayList<>();
		for (int i = 0; i < 3; ++i) {
			args = new HashMap<>();
			fun = ChatCompletionMessageToolCall.Function.builder().name("functionName01").arguments(toString(args))
					.build();
			calls.add(ChatCompletionMessageToolCall.builder().id("call" + i).function(fun).build());
		}
		args = new HashMap<>();
		args.put("param", "hello");
		fun = ChatCompletionMessageToolCall.Function.builder().name("functionNameXX").arguments(toString(args)).build();
		calls.add(ChatCompletionMessageToolCall.builder().id("call" + calls.size()).function(fun).build());

		ChatCompletionMessageParam msg = ChatCompletionMessageParam
				.ofAssistant(ChatCompletionAssistantMessageParam.builder().toolCalls(calls).build());
		OTHER_MESSAGES.add(msg);

		for (int i = 0; i < calls.size(); ++i) {
			OTHER_MESSAGES.add(ChatCompletionMessageParam.ofTool(
					ChatCompletionToolMessageParam.builder().toolCallId(calls.get(i).id()).content("Result").build()));
		}
	}

	/**
	 * Special tests for tool calls having 0 parameters...this is a special case.
	 * 
	 * @param model
	 * @throws JsonProcessingException
	 */
	@ParameterizedTest
	@MethodSource("toolCallCompletionsModelsProvider")
	@DisplayName("Tool calls having 0 parameters")
	void test06(OpenAiModelMetaData md) throws JsonProcessingException {

		String model = md.getModel();
		OpenAiTokenizer counter = md.getTokenizer();
		ChatCompletionCreateParams req = ChatCompletionCreateParams.builder().model(model).messages(OTHER_MESSAGES)
				.build();

		long tokens = counter.count(req);
		long realTokens = realTokens(req);
		System.out.println("0 params tool calls: " + model + "\t" + tokens + "\t" + realTokens);

		assertEquals(realTokens, tokens);
	}

	/**
	 * Test images and image urls in a message
	 */
	@Test
	void testVisionTokens() throws MalformedURLException, URISyntaxException {
//
//		String model = "gpt-4-vision-preview";
//		OpenAiTokenizer counter = modelSvc.getTokenizer(model);
//
//		// Get chat service, set bot personality and tools used
//		OpenAiChatService bot = endpoint.getChatService();
//		bot.setPersonality("You are an helpful assistant.");
//		bot.setModel(model);
//
//		ChatCompletionMessageParam msg = new ChatCompletionMessageParam(Role.USER, "Is there any grass in this image?");
//		msg.getContentParts().add(new FilePart(
//				ResourceUtil.getResourceFile("Gfp-wisconsin-madison-the-nature-boardwalk-MED.png"), "image/png"));
//		msg.getContentParts().add(new FilePart(
//				ResourceUtil.getResourceFile("Gfp-wisconsin-madison-the-nature-boardwalk-LOW.png"), "image/png"));
//		ChatCompletionCreateParams req = bot.getDefaultReq();
//		req._messages().add(msg);
//
//		long tokens = counter.count(req);
//		long realTokens = realTokens(req);
//		assertEquals(realTokens, tokens);
	}

	/**
	 * Test single image in a message
	 */
	@Test
	void testVisionTokens3() throws MalformedURLException, URISyntaxException {
//
//		String model = "gpt-4-vision-preview";
//		OpenAiTokenizer counter = modelSvc.getTokenizer(model);
//
//		// Get chat service, set bot personality and tools used
//		OpenAiChatService bot = endpoint.getChatService();
//		bot.setPersonality("You are an helpful assistant.");
//		bot.setModel(model);
//
//		ChatCompletionMessageParam msg = new ChatCompletionMessageParam(Role.USER, "Is there any grass in this image?");
//		msg.getContentParts().add(new FilePart(
//				ResourceUtil.getResourceFile("Gfp-wisconsin-madison-the-nature-boardwalk-LOW.png"), "image/png"));
//		ChatCompletionCreateParams req = bot.getDefaultReq();
//		req._messages().add(msg);
//
//		long tokens = counter.count(req);
//		long realTokens = realTokens(req);
//		assertEquals(realTokens, tokens);
	}

	/**
	 * Checks a single image URL.
	 */
	@Test
	void testVisionTokens2() throws MalformedURLException, URISyntaxException {
//
//		String model = "gpt-4-vision-preview";
//		OpenAiTokenizer counter = modelSvc.getTokenizer(model);
//
//		// Get chat service, set bot personality and tools used
//		OpenAiChatService bot = endpoint.getChatService();
//		bot.setPersonality("You are an helpful assistant.");
//		bot.setModel(model);
//
//		ChatCompletionMessageParam msg = new ChatCompletionMessageParam(Role.USER, "Is there any grass in this image?");
//		msg.getContentParts().add(FilePart.fromUrl(
//				"https://upload.wikimedia.org/wikipedia/commons/thumb/d/dd/Gfp-wisconsin-madison-the-nature-boardwalk.jpg/2560px-Gfp-wisconsin-madison-the-nature-boardwalk.jpg",
//				"image/jpeg"));
//		ChatCompletionCreateParams req = bot.getDefaultReq();
//		req._messages().add(msg);
//
//		long tokens = counter.count(req);
//
//		// *** IMPORTANT*** with remote images we do not do exact token calculation for
//		// performance reasons.
////		long realTokens = realTokens(req);
//		assertEquals(440, tokens);
	}

	/**
	 * Counts the actual token by calling OpenAI API.
	 * 
	 * @param req
	 * @return
	 */
	private static long realTokens(ChatCompletionCreateParams req) {

		try (OpenAiEndpoint endpoint = new OpenAiEndpoint()) {
			ChatCompletion resp = endpoint.getClient().chat().completions().create(req);
			return resp.usage().get().promptTokens();
		}
	}

	public static void main(String[] arg) throws JsonProcessingException {
//		long tokens, realTokens;
//		String[] models = new String[] { "o1-preview"
//				// , "o3-mini",
//		};

		// Below code is to run test01 step by step
//	public static void TestTest001(String[] arg) throws JsonProcessingException {
//		long tokens, realTokens;
//		String[] models = new String[] { "o1-preview"
//				// , "o3-mini",
//		};
//
//		try (OpenAiEndpoint ep = new OpenAiEndpoint(); OpenAiModelService modelSvc = ep.getModelService();) {
//			for (String model : models) {
//				OpenAiModelMetaData md = modelSvc.get(model);
//				List<SupportedApi> api = md.getSupportedApis();
//				OpenAiTokenizer counter = md.getTokenizer();
//
//				List<ChatCompletionMessageParam> testList = new ArrayList<>(SIMPLE_MESSAGES_2);
//				if (model.startsWith("o1-mini") || model.startsWith("o1-preview"))
//					testList = testList.stream() //
//							.filter(m -> (!m.isDeveloper())) //
//							.collect(Collectors.toList());
//
//				if (api.contains(SupportedApi.CHAT)) {
//					ChatCompletionCreateParams req = ChatCompletionCreateParams.builder().model(model).build();
//					for (int m = 0; m < testList.size(); ++m) {
//
//						req.getMessages().clear();
//						for (int i = 0; i < 3; ++i) {
//							for (int j = 0; j <= m; ++j) {
//								req.getMessages().add(testList.get(j));
//							}
//							tokens = counter.count(req);
//							realTokens = realTokens(req);
//
//							System.out.println(model + "\tfirst " + (m + 1) + " messages " + (i + 1)
//									+ " times\tTotal Messages: " + req.getMessages().size() + "\tCounted: " + tokens
//									+ "\tActual: " + realTokens + "\tDelta: " + (tokens - realTokens));
//						}
//						System.out.println();
//					}
//
//					req.getMessages().clear();
//					req.getMessages().addAll(testList);
//					tokens = counter.count(req);
//					realTokens = realTokens(req);
//
//					System.out.println(model + "\tSIMPLE_MESSAGES\tCounted: " + tokens + "\tActual: " + realTokens
//							+ "\tDelta: " + (tokens - realTokens));
//
//				} else if (api.contains(SupportedApi.COMPLETIONS)) {
//					ChatCompletionCreateParams creq = ChatCompletionCreateParams.builder() //
//							.model(model) //
//							.addUserMessage("This is a prompt, quite short, but it's OK").build();
//					tokens = counter.count(creq.getPrompt());
//					realTokens = realTokens(creq);
//
//					System.out.println(model + "\tCounted: " + tokens + "\tActual: " + realTokens);
//				} else
//					throw new IllegalArgumentException();
//			} // For each model
//		}
	}
}