package io.github.mzattera.predictivepowers.openai.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;

import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsResponse;
import io.github.mzattera.predictivepowers.openai.client.chat.FunctionCall;
import io.github.mzattera.predictivepowers.openai.client.chat.OpenAiTool;
import io.github.mzattera.predictivepowers.openai.client.chat.OpenAiTool.Type;
import io.github.mzattera.predictivepowers.openai.client.chat.OpenAiToolCall;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsResponse;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatMessage.Role;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiModelMetaData;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiModelMetaData.SupportedApi;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiModelMetaData.SupportedCallType;
import io.github.mzattera.predictivepowers.services.Agent;
import io.github.mzattera.predictivepowers.services.SimpleToolProvider;
import io.github.mzattera.predictivepowers.services.Tool;
import io.github.mzattera.predictivepowers.services.ToolInitializationException;
import io.github.mzattera.predictivepowers.services.ToolProvider;
import io.github.mzattera.predictivepowers.services.messages.ToolCall;
import io.github.mzattera.predictivepowers.services.messages.ToolCallResult;
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

	/** Return names for all chat /completions models */
	static Stream<OpenAiModelMetaData> allCompletionModelsProvider() {
		return modelSvc.listModels().stream() //
				.filter(model -> !model.startsWith("gpt-4-32k")) //
				.map(model -> modelSvc.get(model)) //
				.filter(meta -> meta != null) //
				.filter(meta -> (meta.getSupportedApi() == SupportedApi.CHAT)
						|| (meta.getSupportedApi() == SupportedApi.COMPLETIONS));
	}

	/** Return names for models supporting function calls */
	static Stream<OpenAiModelMetaData> functionCallCompletionsModelsProvider() {
		return allCompletionModelsProvider() //
				.filter(e -> e.getSupportedCallType() == SupportedCallType.FUNCTIONS); //
	}

	/** Return names for models supporting tool calls */
	static Stream<OpenAiModelMetaData> toolCallCompletionsModelsProvider() {
		return allCompletionModelsProvider() //
				.filter(e -> e.getSupportedCallType() == SupportedCallType.TOOLS); //
	}

	/** List of messages without tool calls or tools results */
	private final static List<OpenAiChatMessage> SIMPLE_MESSAGES = new ArrayList<>();
	static {
		SIMPLE_MESSAGES.add(new OpenAiChatMessage(Role.SYSTEM, "You are a nice bot"));
		SIMPLE_MESSAGES.add(new OpenAiChatMessage(Role.SYSTEM, "You are a nice bot", "system_user"));
		SIMPLE_MESSAGES.add(new OpenAiChatMessage(Role.SYSTEM, "Thank you!", "system_assistant"));
		SIMPLE_MESSAGES.add(new OpenAiChatMessage(Role.ASSISTANT, "HI."));
		SIMPLE_MESSAGES.add(new OpenAiChatMessage(Role.ASSISTANT, "HI.", "Tom_the_Assistant"));
		SIMPLE_MESSAGES.add(new OpenAiChatMessage(Role.USER, "Hi", "maxi"));
		SIMPLE_MESSAGES.add(new OpenAiChatMessage(Role.USER,
				"Alan Mathison Turing OBE FRS (/ˈtjʊərɪŋ/; 23 June 1912 – 7 June 1954) was an English mathematician, computer scientist, logician, cryptanalyst, philosopher and theoretical biologist.[5] Turing was highly influential in the development of theoretical computer science, providing a formalisation of the concepts of algorithm and computation with the Turing machine, which can be considered a model of a general-purpose computer.[6][7][8] He is widely considered to be the father of theoretical computer science and artificial intelligence.[9]\r\n"));
	}

	/**
	 * Length of messages. No tool calls or tool results.
	 * 
	 * @throws JsonProcessingException
	 */
	@ParameterizedTest
	@MethodSource("allCompletionModelsProvider")
	void test01(OpenAiModelMetaData md) throws JsonProcessingException {

		long tokens, realTokens;
		String model = md.getModel();
		SupportedApi api = md.getSupportedApi();
		OpenAiTokenizer counter = md.getTokenizer();
		switch (api) {
		case CHAT:
			ChatCompletionsRequest req = ChatCompletionsRequest.builder().model(model).messages(SIMPLE_MESSAGES)
					.build();
			tokens = counter.count(req);
			realTokens = realTokens(req);
			break;
		case COMPLETIONS:
			CompletionsRequest creq = CompletionsRequest.builder() //
					.model(model) //
					.prompt("This is a prompt, quite short, but it's OK").build();
			tokens = counter.count(creq.getPrompt());
			realTokens = realTokens(creq);
			break;
		default:
			throw new IllegalArgumentException();
		}

		System.out.println(model + " - " + api + "\t" + tokens + "\t" + realTokens);
		assertEquals(realTokens, tokens);
	}

	private static enum ENUM {
		BANANE, MELE
	}

	/** List of messages with function calls and function results */
	private final static List<OpenAiChatMessage> FUNCTION_CALL_MESSAGES = new ArrayList<>();
	static {

		Map<String, Object> params = new HashMap<>();
		FunctionCall call = FunctionCall.builder().name("functionName").arguments(params).build();
		FUNCTION_CALL_MESSAGES.add(new OpenAiChatMessage(call));
		params = new HashMap<>();
		params.put("s", "Prague");
		call = FunctionCall.builder().name("functionName").arguments(params).build();
		FUNCTION_CALL_MESSAGES.add(new OpenAiChatMessage(call));
		params = new HashMap<>();
		params.put("s", "Prague");
		params.put("i", 3);
		call = FunctionCall.builder().name("functionName").arguments(params).build();
		FUNCTION_CALL_MESSAGES.add(new OpenAiChatMessage(call));
		params = new HashMap<>();
		params.put("s", "Prague");
		params.put("i", 3);
		params.put("d", 3.0d);
		call = FunctionCall.builder().name("functionName").arguments(params).build();
		FUNCTION_CALL_MESSAGES.add(new OpenAiChatMessage(call));
		params = new HashMap<>();
		params.put("s", "Prague");
		params.put("i", 3);
		params.put("d", 3.0d);
		params.put("b", ENUM.BANANE);
		call = FunctionCall.builder().name("functionName").arguments(params).build();
		FUNCTION_CALL_MESSAGES.add(new OpenAiChatMessage(call));

		List<OpenAiChatMessage> replies = new ArrayList<>();
		for (OpenAiChatMessage msg : FUNCTION_CALL_MESSAGES) {
			FunctionCall c = msg.getFunctionCall();
			replies.add(new OpenAiChatMessage(Role.FUNCTION, "Result", c.getName()));
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
	private static class GetCurrentWeatherTool implements Tool {

		// The function parameters
		private static class GetCurrentWeatherParameters {

			private enum TemperatureUnits {
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
		}

		@Override
		public String getId() {
			return "getCurrentWeather";
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
		}

		@Override
		public ToolCallResult invoke(@NonNull ToolCall call) throws Exception {
			// Function implementation goes here.
			// In this example we simply return a random temperature.
			return new ToolCallResult(call, "20°C");
		}

		@Override
		public void close() {
		}
	}

	// List of functions available to the bot (for now it is only 1).
	private final static List<Tool> TOOLS = new ArrayList<>();
	static {
		TOOLS.add(new OpenAiTool(new GetCurrentWeatherTool()));
		TOOLS.add(new OpenAiTool(new GetCurrentWeatherTool()));
		TOOLS.add(new OpenAiTool(new GetCurrentWeatherTool()));
	}
	
	private final static ToolProvider PROVIDER = new SimpleToolProvider(TOOLS);

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
		bot.setToolProvider(PROVIDER);
		bot.setTools(PROVIDER.getToolIds());

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
		bot.setToolProvider(PROVIDER);
		bot.setTools(PROVIDER.getToolIds());

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
	 * Counts the actual token by calling OpenAI API.
	 * 
	 * @param req
	 * @return
	 */
	private static long realTokens(ChatCompletionsRequest req) {

		try (OpenAiEndpoint endpoint = new OpenAiEndpoint()) {
			req.setMaxTokens(1);
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
}