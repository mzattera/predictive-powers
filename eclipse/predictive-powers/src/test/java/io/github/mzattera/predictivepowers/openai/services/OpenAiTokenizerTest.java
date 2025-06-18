/*
 * Copyright 2024 Massimiliano "Maxi" Zattera
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
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;

import io.github.mzattera.predictivepowers.openai.client.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsResponse;
import io.github.mzattera.predictivepowers.openai.client.chat.FunctionCall;
import io.github.mzattera.predictivepowers.openai.client.chat.OpenAiTool.Type;
import io.github.mzattera.predictivepowers.openai.client.chat.OpenAiToolCall;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsResponse;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatMessage.Role;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiModelMetaData;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiModelMetaData.CallType;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiModelMetaData.SupportedApi;
import io.github.mzattera.predictivepowers.services.AbstractTool;
import io.github.mzattera.predictivepowers.services.Capability;
import io.github.mzattera.predictivepowers.services.ModelService.ModelMetaData.Mode;
import io.github.mzattera.predictivepowers.services.ToolInitializationException;
import io.github.mzattera.predictivepowers.services.Toolset;
import io.github.mzattera.predictivepowers.services.messages.FilePart;
import io.github.mzattera.predictivepowers.services.messages.ToolCall;
import io.github.mzattera.predictivepowers.services.messages.ToolCallResult;
import io.github.mzattera.util.ResourceUtil;
import lombok.NonNull;

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

	/** Return names for all chat / completions models */
	static Stream<OpenAiModelMetaData> allCompletionModelsProvider() {
		return modelSvc.listModels().stream() //
//				.filter(model -> !model.startsWith("gpt-4-32k")) //
				.map(model -> modelSvc.get(model)) //
				.filter(meta -> meta != null) //
				.filter(meta -> (meta.getSupportedApis().contains(SupportedApi.CHAT)
						|| meta.getSupportedApis().contains(SupportedApi.COMPLETIONS))) //
				.filter(meta -> (!meta.getOutputModes().contains(Mode.AUDIO))) //
				.filter(meta -> (!meta.getModel().contains("search")));
	}

	/** Return names for models supporting function calls */
	static Stream<OpenAiModelMetaData> functionCallCompletionsModelsProvider() {
		return allCompletionModelsProvider() //
				.filter(e -> e.getSupportedCallType() == CallType.FUNCTIONS) //
				.filter(meta -> meta.getSupportedApis().contains(SupportedApi.CHAT)); //
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
	private final static List<OpenAiChatMessage> SIMPLE_MESSAGES_1 = new ArrayList<>();
	static {
		SIMPLE_MESSAGES_1.add(new OpenAiChatMessage(Role.ASSISTANT, "I"));
		SIMPLE_MESSAGES_1.add(new OpenAiChatMessage(Role.ASSISTANT, "I", "V"));
		SIMPLE_MESSAGES_1.add(new OpenAiChatMessage(Role.ASSISTANT, "I"));
		SIMPLE_MESSAGES_1.add(new OpenAiChatMessage(Role.ASSISTANT, "I", "V"));

		SIMPLE_MESSAGES_1.add(new OpenAiChatMessage(Role.USER, "I"));
		SIMPLE_MESSAGES_1.add(new OpenAiChatMessage(Role.USER, "I", "V"));
		SIMPLE_MESSAGES_1.add(new OpenAiChatMessage(Role.USER, "I"));
		SIMPLE_MESSAGES_1.add(new OpenAiChatMessage(Role.USER, "I", "V"));

		SIMPLE_MESSAGES_1.add(new OpenAiChatMessage(Role.DEVELOPER, "I"));
		SIMPLE_MESSAGES_1.add(new OpenAiChatMessage(Role.DEVELOPER, "I", "V"));
		SIMPLE_MESSAGES_1.add(new OpenAiChatMessage(Role.DEVELOPER, "I"));
		SIMPLE_MESSAGES_1.add(new OpenAiChatMessage(Role.DEVELOPER, "I", "V"));

		SIMPLE_MESSAGES_1.add(new OpenAiChatMessage(Role.DEVELOPER, "I"));
		SIMPLE_MESSAGES_1.add(new OpenAiChatMessage(Role.DEVELOPER, "I", "V"));
		SIMPLE_MESSAGES_1.add(new OpenAiChatMessage(Role.DEVELOPER, "I"));
		SIMPLE_MESSAGES_1.add(new OpenAiChatMessage(Role.DEVELOPER, "I", "V"));
	}
	private final static List<OpenAiChatMessage> SIMPLE_MESSAGES_2 = new ArrayList<>();
	static {
		SIMPLE_MESSAGES_2.add(new OpenAiChatMessage(Role.DEVELOPER, "I", "V"));
		SIMPLE_MESSAGES_2.add(new OpenAiChatMessage(Role.DEVELOPER, "I"));
		SIMPLE_MESSAGES_2.add(new OpenAiChatMessage(Role.DEVELOPER, "I", "V"));
		SIMPLE_MESSAGES_2.add(new OpenAiChatMessage(Role.DEVELOPER, "I"));

		SIMPLE_MESSAGES_2.add(new OpenAiChatMessage(Role.DEVELOPER, "I", "V"));
		SIMPLE_MESSAGES_2.add(new OpenAiChatMessage(Role.DEVELOPER, "I"));
		SIMPLE_MESSAGES_2.add(new OpenAiChatMessage(Role.DEVELOPER, "I", "V"));
		SIMPLE_MESSAGES_2.add(new OpenAiChatMessage(Role.DEVELOPER, "I"));

		SIMPLE_MESSAGES_2.add(new OpenAiChatMessage(Role.USER, "I", "V"));
		SIMPLE_MESSAGES_2.add(new OpenAiChatMessage(Role.USER, "I"));
		SIMPLE_MESSAGES_2.add(new OpenAiChatMessage(Role.USER, "I", "V"));
		SIMPLE_MESSAGES_2.add(new OpenAiChatMessage(Role.USER, "I"));

		SIMPLE_MESSAGES_2.add(new OpenAiChatMessage(Role.ASSISTANT, "I", "V"));
		SIMPLE_MESSAGES_2.add(new OpenAiChatMessage(Role.ASSISTANT, "I"));
		SIMPLE_MESSAGES_2.add(new OpenAiChatMessage(Role.ASSISTANT, "I", "V"));
		SIMPLE_MESSAGES_2.add(new OpenAiChatMessage(Role.ASSISTANT, "I"));
	}

	/**
	 * Checks all models have a tokenizer.
	 */
	@ParameterizedTest
	@MethodSource("allCompletionModelsProvider")
	void test00(OpenAiModelMetaData md) {
		assertTrue(md.getTokenizer() != null, "Null tokenizer for model");
		assertTrue(md.getTokenizer().getEncoding() != null, "Null encoding for model");
	}

	/**
	 * Length of messages. No tool calls or tool results.
	 * 
	 * @throws JsonProcessingException
	 */
	@ParameterizedTest
	@MethodSource("allCompletionModelsProvider")
	void test01(OpenAiModelMetaData md) throws JsonProcessingException {
		simpleTest(md, SIMPLE_MESSAGES_1);
		simpleTest(md, SIMPLE_MESSAGES_2);
	}

	private void simpleTest(OpenAiModelMetaData md, List<OpenAiChatMessage> test) {
		long tokens, realTokens;
		String model = md.getModel();
		List<SupportedApi> api = md.getSupportedApis();
		OpenAiTokenizer counter = md.getTokenizer();
		if (api.contains(SupportedApi.CHAT)) {
			ChatCompletionsRequest req = ChatCompletionsRequest.builder().model(model).messages(test).build();
			if (model.startsWith("o1-mini") || model.startsWith("o1-preview"))
				req.setMessages(req.getMessages().stream() //
						.filter(m -> ((m.getRole() != Role.DEVELOPER) && (m.getRole() != Role.DEVELOPER))) //
						.collect(Collectors.toList()));
			tokens = counter.count(req);
			realTokens = realTokens(req);
		} else if (api.contains(SupportedApi.COMPLETIONS)) {
			CompletionsRequest creq = CompletionsRequest.builder() //
					.model(model) //
					.prompt("This is a prompt, quite short, but it's OK").build();
			tokens = counter.count(creq.getPrompt());
			realTokens = realTokens(creq);
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
	private final static List<OpenAiChatMessage> FUNCTION_CALL_MESSAGES = new ArrayList<>();
	static {

		Map<String, Object> params;
		FunctionCall call;

		params = new HashMap<>();
		call = FunctionCall.builder().name("I").arguments(params).build();
		FUNCTION_CALL_MESSAGES.add(new OpenAiChatMessage(call));

		params = new HashMap<>();
		params.put("s", "T");
		call = FunctionCall.builder().name("I").arguments(params).build();
		FUNCTION_CALL_MESSAGES.add(new OpenAiChatMessage(call));

		params = new HashMap<>();
		params.put("s", "T");
		params.put("i", 3);
		call = FunctionCall.builder().name("I").arguments(params).build();
		FUNCTION_CALL_MESSAGES.add(new OpenAiChatMessage(call));

		params = new HashMap<>();
		params.put("s", "T");
		params.put("i", 3);
		params.put("d", 3.0d);
		call = FunctionCall.builder().name("I").arguments(params).build();
		FUNCTION_CALL_MESSAGES.add(new OpenAiChatMessage(call));

		params = new HashMap<>();
		params.put("s", "T");
		params.put("i", 3);
		params.put("d", 3.0d);
		params.put("b", ENUM.BANANE);
		call = FunctionCall.builder().name("I").arguments(params).build();
		FUNCTION_CALL_MESSAGES.add(new OpenAiChatMessage(call));

		List<OpenAiChatMessage> replies = new ArrayList<>();
		for (OpenAiChatMessage msg : FUNCTION_CALL_MESSAGES) {
			FunctionCall c = msg.getFunctionCall();
			replies.add(new OpenAiChatMessage(Role.FUNCTION, "V", c.getName()));
		}
		FUNCTION_CALL_MESSAGES.addAll(replies);
	}

	/**
	 * Length of messages. Function calls and function results.
	 * 
	 * @throws JsonProcessingException
	 */
	@ParameterizedTest
	@MethodSource("functionCallCompletionsModelsProvider")
	void test02(OpenAiModelMetaData md) throws JsonProcessingException {

		String model = md.getModel();
		OpenAiTokenizer counter = md.getTokenizer();
		ChatCompletionsRequest req = ChatCompletionsRequest.builder() //
				.model(model) //
				.messages(FUNCTION_CALL_MESSAGES).build();

		long tokens = counter.count(req);
		long realTokens = realTokens(req);
		System.out.println(model + "\t" + tokens + "\t" + realTokens);

		assertEquals(realTokens, tokens);
	}

	/**
	 * Length of messages. Tool calls and tool results.
	 * 
	 * @throws JsonProcessingException
	 */
	@ParameterizedTest
	@MethodSource("toolCallCompletionsModelsProvider")
	void test03(OpenAiModelMetaData md) throws JsonProcessingException {

		String model = md.getModel();
		OpenAiTokenizer counter = md.getTokenizer();
		List<OpenAiChatMessage> messages = new ArrayList<>();

		Map<String, Object> args = new HashMap<>();
		List<OpenAiToolCall> calls = new ArrayList<>();

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
				FunctionCall fun = FunctionCall.builder().name("functionName01").arguments(args).build();

				// Call with numCalls function calls
				calls = new ArrayList<>();
				for (int i = 0; i < numCalls; ++i) {
					calls.add(OpenAiToolCall.builder().type(Type.FUNCTION).Id("call" + i).function(fun).build());
				}
				OpenAiChatMessage msg = new OpenAiChatMessage(Role.ASSISTANT, (String) null);
				msg.setToolCalls(calls);
				messages.add(msg);

				// Corresponding replies
				for (int i = 0; i < calls.size(); ++i) {
					messages.add(new OpenAiChatMessage(Role.TOOL,
							new ToolCallResult(calls.get(i).getId(), calls.get(i).getFunction().getName(), "Result")));
				}

				ChatCompletionsRequest req = ChatCompletionsRequest.builder().model(model).messages(messages).build();
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

			// TODO URGENT Add it back and redo tests
//			@SuppressWarnings("unused")
//			public boolean bull;
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
			result.putTool("getCurrentWeather" + i, GetCurrentWeatherTool.class);
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
	void test04(OpenAiModelMetaData md) throws JsonProcessingException, ToolInitializationException {

		String model = md.getModel();
		OpenAiTokenizer counter = md.getTokenizer();

		// Get chat service, set bot personality and tools used
		OpenAiChatService bot = endpoint.getChatService();
		bot.setPersonality("You are an helpful assistant.");
		bot.setModel(model); // This uses simple function calls
		bot.addCapability(getToolset());

		ChatCompletionsRequest req = bot.getDefaultReq();
		req.getMessages().add(new OpenAiChatMessage(Role.USER, "Hi"));

		long tokens = counter.count(req);
		long realTokens = realTokens(req);

		System.out.println(model + "\t" + tokens + "\t" + realTokens);
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
	void test05(OpenAiModelMetaData md) throws JsonProcessingException, ToolInitializationException {

		String model = md.getModel();
		OpenAiTokenizer counter = md.getTokenizer();

		// Get chat service, set bot personality and tools used
		OpenAiChatService bot = endpoint.getChatService();
		bot.setPersonality("You are an helpful assistant.");
		bot.setModel(model); // This uses simple tool (parallel functions) calls
		bot.addCapability(getToolset());

		ChatCompletionsRequest req = bot.getDefaultReq();
		req.getMessages().add(new OpenAiChatMessage(Role.USER, "Hi"));

		long tokens = counter.count(req);
		long realTokens = realTokens(req);
		System.out.println(model + "\t" + tokens + "\t" + realTokens);
	}

	private final static List<OpenAiChatMessage> OTHER_MESSAGES = new ArrayList<>();
	static {

		Map<String, Object> args;
		FunctionCall fun;

		List<OpenAiToolCall> calls = new ArrayList<>();
		for (int i = 0; i < 3; ++i) {
			args = new HashMap<>();
			fun = FunctionCall.builder().name("functionName01").arguments(args).build();

			OpenAiToolCall call = OpenAiToolCall.builder().type(Type.FUNCTION).Id("call" + i).function(fun).build();
			calls.add(call);
		}
		args = new HashMap<>();
		args.put("param", "hello");
		fun = FunctionCall.builder().name("functionNameXX").arguments(args).build();
		OpenAiToolCall call = OpenAiToolCall.builder().type(Type.FUNCTION).Id("call" + calls.size()).function(fun)
				.build();
		calls.add(call);

		OpenAiChatMessage msg = new OpenAiChatMessage(Role.ASSISTANT, (String) null);
		msg.setToolCalls(calls);
		OTHER_MESSAGES.add(msg);

		for (int i = 0; i < calls.size(); ++i) {
			OTHER_MESSAGES.add(new OpenAiChatMessage(Role.TOOL,
					new ToolCallResult(calls.get(i).getId(), calls.get(i).getFunction().getName(), "Result")));
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
	void test06(OpenAiModelMetaData md) throws JsonProcessingException {

		String model = md.getModel();
		OpenAiTokenizer counter = md.getTokenizer();
		ChatCompletionsRequest req = ChatCompletionsRequest.builder().model(model).messages(OTHER_MESSAGES).build();

		long tokens = counter.count(req);
		long realTokens = realTokens(req);
		System.out.println(model + "\t" + tokens + "\t" + realTokens);

		assertEquals(realTokens, tokens);
	}

	/**
	 * Test images and image urls in a message
	 */
	@Test
	void testVisionTokens() throws MalformedURLException, URISyntaxException {

		String model = "gpt-4-vision-preview";
		OpenAiTokenizer counter = modelSvc.getTokenizer(model);

		// Get chat service, set bot personality and tools used
		OpenAiChatService bot = endpoint.getChatService();
		bot.setPersonality("You are an helpful assistant.");
		bot.setModel(model);

		OpenAiChatMessage msg = new OpenAiChatMessage(Role.USER, "Is there any grass in this image?");
		msg.getContentParts().add(new FilePart(
				ResourceUtil.getResourceFile("Gfp-wisconsin-madison-the-nature-boardwalk-MED.png"), "image/png"));
		msg.getContentParts().add(new FilePart(
				ResourceUtil.getResourceFile("Gfp-wisconsin-madison-the-nature-boardwalk-LOW.png"), "image/png"));
		ChatCompletionsRequest req = bot.getDefaultReq();
		req.getMessages().add(msg);

		long tokens = counter.count(req);
		long realTokens = realTokens(req);
		assertEquals(realTokens, tokens);
	}

	/**
	 * Test single image in a message
	 */
	@Test
	void testVisionTokens3() throws MalformedURLException, URISyntaxException {

		String model = "gpt-4-vision-preview";
		OpenAiTokenizer counter = modelSvc.getTokenizer(model);

		// Get chat service, set bot personality and tools used
		OpenAiChatService bot = endpoint.getChatService();
		bot.setPersonality("You are an helpful assistant.");
		bot.setModel(model);

		OpenAiChatMessage msg = new OpenAiChatMessage(Role.USER, "Is there any grass in this image?");
		msg.getContentParts().add(new FilePart(
				ResourceUtil.getResourceFile("Gfp-wisconsin-madison-the-nature-boardwalk-LOW.png"), "image/png"));
		ChatCompletionsRequest req = bot.getDefaultReq();
		req.getMessages().add(msg);

		long tokens = counter.count(req);
		long realTokens = realTokens(req);
		assertEquals(realTokens, tokens);
	}

	/**
	 * Checks a single image URL.
	 */
	@Test
	void testVisionTokens2() throws MalformedURLException, URISyntaxException {

		String model = "gpt-4-vision-preview";
		OpenAiTokenizer counter = modelSvc.getTokenizer(model);

		// Get chat service, set bot personality and tools used
		OpenAiChatService bot = endpoint.getChatService();
		bot.setPersonality("You are an helpful assistant.");
		bot.setModel(model);

		OpenAiChatMessage msg = new OpenAiChatMessage(Role.USER, "Is there any grass in this image?");
		msg.getContentParts().add(FilePart.fromUrl(
				"https://upload.wikimedia.org/wikipedia/commons/thumb/d/dd/Gfp-wisconsin-madison-the-nature-boardwalk.jpg/2560px-Gfp-wisconsin-madison-the-nature-boardwalk.jpg",
				"image/jpeg"));
		ChatCompletionsRequest req = bot.getDefaultReq();
		req.getMessages().add(msg);

		long tokens = counter.count(req);

		// *** IMPORTANT*** with remote images we do not do exact token calculation for
		// performance reasons.
//		long realTokens = realTokens(req);
		assertEquals(440, tokens);
	}

	/**
	 * Counts the actual token by calling OpenAI API.
	 * 
	 * @param req
	 * @return
	 */
	private static long realTokens(ChatCompletionsRequest req) {

		try (OpenAiEndpoint endpoint = new OpenAiEndpoint()) {
			ChatCompletionsResponse resp = endpoint.getClient().createChatCompletion(req);
			return resp.getUsage().getPromptTokens();
		}
	}

	/**
	 * Counts the actual token by calling OpenAI API.
	 * 
	 * @param req
	 * @return
	 */
	private static long realTokens(CompletionsRequest req) {

		try (OpenAiEndpoint endpoint = new OpenAiEndpoint()) {
			req.setMaxTokens(1);
			CompletionsResponse resp = endpoint.getClient().createCompletion(req);
			return resp.getUsage().getPromptTokens();
		}
	}

	public static void main(String[] arg) throws JsonProcessingException {
		long tokens, realTokens;
		String[] models = new String[] { "o1-preview"
				// , "o3-mini",
		};


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
//				List<OpenAiChatMessage> testList = new ArrayList<>(SIMPLE_MESSAGES_2);
//				if (model.startsWith("o1-mini") || model.startsWith("o1-preview"))
//					testList = testList.stream() //
//							.filter(m -> ((m.getRole() != Role.DEVELOPER) && (m.getRole() != Role.DEVELOPER))) //
//							.collect(Collectors.toList());
//
//				if (api.contains(SupportedApi.CHAT)) {
//					ChatCompletionsRequest req = ChatCompletionsRequest.builder().model(model).build();
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
//					CompletionsRequest creq = CompletionsRequest.builder() //
//							.model(model) //
//							.prompt("This is a prompt, quite short, but it's OK").build();
//					tokens = counter.count(creq.getPrompt());
//					realTokens = realTokens(creq);
//
//					System.out.println(model + "\tCounted: " + tokens + "\tActual: " + realTokens);
//				} else
//					throw new IllegalArgumentException();
//			} // For each model
//		}
//	}
}