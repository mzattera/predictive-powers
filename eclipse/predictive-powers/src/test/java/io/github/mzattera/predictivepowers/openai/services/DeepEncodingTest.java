package io.github.mzattera.predictivepowers.openai.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;

import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsResponse;
import io.github.mzattera.predictivepowers.openai.client.chat.FunctionCall;
import io.github.mzattera.predictivepowers.openai.client.chat.OpenAiTool.Type;
import io.github.mzattera.predictivepowers.openai.client.chat.ToolCall;
import io.github.mzattera.predictivepowers.openai.client.chat.ToolCallResult;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatMessage.Role;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiModelData;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiModelData.SupportedCallType;
import lombok.Getter;

// TODO URGENT FINISH, RENAME AND MOVE IT TO A MORE SUITABLE FOLDER

public class DeepEncodingTest {

	/** Used for JSON (de)serialization in API calls */
	@Getter
	private final static ObjectMapper jsonMapper;

	static {
		jsonMapper = new ObjectMapper();
		jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		jsonMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		jsonMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
	}

	private final static Map<String, OpenAiModelData> MODEL_CONFIG = new HashMap<>();
	static {
		// TODO Completions, to test
		// TODO Add API to OpenAiModelData
		// TODO Add proper tokenizer to OpenAiModelData, without going through model
		// name/prefix
//		MODEL_CONFIG.put("babbage-002", new OpenAiModelData(16384));
//		MODEL_CONFIG.put("davinci-002", new OpenAiModelData(16384));
//		MODEL_CONFIG.put("gpt-3.5-turbo-instruct", new OpenAiModelData(4096));
//		MODEL_CONFIG.put("gpt-3.5-turbo-instruct-0914", new OpenAiModelData(4096));

		MODEL_CONFIG.put("gpt-3.5-turbo", new OpenAiModelData(4096, SupportedCallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-3.5-turbo-16k", new OpenAiModelData(16384, SupportedCallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-3.5-turbo-1106", new OpenAiModelData(16385, SupportedCallType.TOOLS, 4096));
		MODEL_CONFIG.put("gpt-3.5-turbo-0613", new OpenAiModelData(4096, SupportedCallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-3.5-turbo-16k-0613", new OpenAiModelData(16384, SupportedCallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-3.5-turbo-0301", new OpenAiModelData(4096));
		MODEL_CONFIG.put("gpt-4", new OpenAiModelData(8192, SupportedCallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-4-0613", new OpenAiModelData(8192, SupportedCallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-4-1106-preview", new OpenAiModelData(128000, SupportedCallType.TOOLS, 4096));

		// TODO URGENT Questo calcola token usando immagini e testo, mi da' linghezze
		// strane se
		// metto i nomi nei messaggi, ma almeno sbaglio in eccesso
		// Magari lanciare un errore se si tenta di usare questo tokenizer? O
		// accontentarsi di sbagliare in eccesso?
//		MODEL_CONFIG.put("gpt-4-vision-preview", new OpenAiModelData(128000, 4096));

//		MODEL_CONFIG.put("gpt-4-32k", new OpenAiModelData(32768, SupportedCallType.FUNCTIONS));
//		MODEL_CONFIG.put("gpt-4-32k-0613", new OpenAiModelData(32768, SupportedCallType.FUNCTIONS));
//		MODEL_CONFIG.put("gpt-4-32k-0314", new OpenAiModelData(32768));
	}

	private static final Map<String, String> MODEL_PREFIX_TO_ENCODING = new HashMap<>();
	private static final Map<String, String> MODEL_TO_ENCODING = new HashMap<>();

	static {
		// Initialize MODEL_PREFIX_TO_ENCODING
		MODEL_PREFIX_TO_ENCODING.put("gpt-4-", "cl100k_base");
		MODEL_PREFIX_TO_ENCODING.put("gpt-3.5-turbo-", "cl100k_base");
		MODEL_PREFIX_TO_ENCODING.put("gpt-35-turbo-", "cl100k_base");

		// Initialize MODEL_TO_ENCODING
		// chat
		MODEL_TO_ENCODING.put("gpt-4", "cl100k_base");
		MODEL_TO_ENCODING.put("gpt-3.5-turbo", "cl100k_base");
		MODEL_TO_ENCODING.put("gpt-35-turbo", "cl100k_base"); // Azure deployment name

		// base
		MODEL_TO_ENCODING.put("davinci-002", "cl100k_base");
		MODEL_TO_ENCODING.put("babbage-002", "cl100k_base");

		// embeddings
		MODEL_TO_ENCODING.put("text-embedding-ada-002", "cl100k_base");

		// DEPRECATED MODELS
		// text (DEPRECATED)
		MODEL_TO_ENCODING.put("text-davinci-003", "p50k_base");
		MODEL_TO_ENCODING.put("text-davinci-002", "p50k_base");
		MODEL_TO_ENCODING.put("text-davinci-001", "r50k_base");
		MODEL_TO_ENCODING.put("text-curie-001", "r50k_base");
		MODEL_TO_ENCODING.put("text-babbage-001", "r50k_base");
		MODEL_TO_ENCODING.put("text-ada-001", "r50k_base");
		MODEL_TO_ENCODING.put("davinci", "r50k_base");
		MODEL_TO_ENCODING.put("curie", "r50k_base");
		MODEL_TO_ENCODING.put("babbage", "r50k_base");
		MODEL_TO_ENCODING.put("ada", "r50k_base");

		// code (DEPRECATED)
		MODEL_TO_ENCODING.put("code-davinci-002", "p50k_base");
		MODEL_TO_ENCODING.put("code-davinci-001", "p50k_base");
		MODEL_TO_ENCODING.put("code-cushman-002", "p50k_base");
		MODEL_TO_ENCODING.put("code-cushman-001", "p50k_base");
		MODEL_TO_ENCODING.put("davinci-codex", "p50k_base");
		MODEL_TO_ENCODING.put("cushman-codex", "p50k_base");

		// edit (DEPRECATED)
		MODEL_TO_ENCODING.put("text-davinci-edit-001", "p50k_edit");
		MODEL_TO_ENCODING.put("code-davinci-edit-001", "p50k_edit");

		// old embeddings (DEPRECATED)
		MODEL_TO_ENCODING.put("text-similarity-davinci-001", "r50k_base");
		MODEL_TO_ENCODING.put("text-similarity-curie-001", "r50k_base");
		MODEL_TO_ENCODING.put("text-similarity-babbage-001", "r50k_base");
		MODEL_TO_ENCODING.put("text-similarity-ada-001", "r50k_base");
		MODEL_TO_ENCODING.put("text-search-davinci-doc-001", "r50k_base");
		MODEL_TO_ENCODING.put("text-search-curie-doc-001", "r50k_base");
		MODEL_TO_ENCODING.put("text-search-babbage-doc-001", "r50k_base");
		MODEL_TO_ENCODING.put("text-search-ada-doc-001", "r50k_base");
		MODEL_TO_ENCODING.put("code-search-babbage-code-001", "r50k_base");
		MODEL_TO_ENCODING.put("code-search-ada-code-001", "r50k_base");

		// open source
		MODEL_TO_ENCODING.put("gpt2", "gpt2");
	}

	public static String getEncodingName(String modelName) {

		String encodingName = MODEL_TO_ENCODING.get(modelName);
		if (encodingName != null)
			return encodingName;

		for (Map.Entry<String, String> entry : MODEL_PREFIX_TO_ENCODING.entrySet()) {
			if (modelName.startsWith(entry.getKey())) {
				return entry.getValue();
			}
		}

		throw new IllegalArgumentException("Unrecognized model: " + modelName);
	}

	public static Encoding getEncoding(String modelName) {
		EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();

		switch (getEncodingName(modelName)) {
		case "cl100k_base":
			return registry.getEncoding(EncodingType.CL100K_BASE);
		case "p50k_base":
			return registry.getEncoding(EncodingType.P50K_BASE);
		case "p50k_edit":
			return registry.getEncoding(EncodingType.P50K_EDIT);
		case "r50k_base":
			return registry.getEncoding(EncodingType.R50K_BASE);
		default:
			throw new IllegalArgumentException("Unrecognized model: " + modelName);
		}
	}

	/** Return names for all models */
	static Stream<String> allModelsProvider() {
		return MODEL_CONFIG.keySet().stream();
	}

	/** Return names for models supporting function calls */
	static Stream<String> functionCallModelsProvider() {
		List<String> result = new ArrayList<>();
		for (String model : MODEL_CONFIG.keySet()) {
			if (MODEL_CONFIG.get(model).getSupportedCallType() == SupportedCallType.FUNCTIONS)
				result.add(model);
		}

		return result.stream();
	}

	/** Return names for models supporting tool calls */
	static Stream<String> toolCallModelsProvider() {
		List<String> result = new ArrayList<>();
		for (String model : MODEL_CONFIG.keySet()) {
			if (MODEL_CONFIG.get(model).getSupportedCallType() == SupportedCallType.TOOLS)
				result.add(model);
		}

		// TODO URGENT RETURN ALL
		return result.subList(0, 1).stream();
	}

	/** List of messages without tool calls or tools results */
	private final static List<OpenAiChatMessage> SIMPLE_MESSAGES = new ArrayList<>();
	static {
		SIMPLE_MESSAGES.add(new OpenAiChatMessage(Role.SYSTEM, "You are a nice bot"));
		SIMPLE_MESSAGES.add(new OpenAiChatMessage(Role.SYSTEM, "You are a nice bot", "system_user"));
		SIMPLE_MESSAGES.add(new OpenAiChatMessage(Role.SYSTEM, "Thank you!", "system_assistant"));
		SIMPLE_MESSAGES.add(new OpenAiChatMessage(Role.ASSISTANT, "HI."));
		SIMPLE_MESSAGES.add(new OpenAiChatMessage(Role.USER, "Hi", "maxi"));
		SIMPLE_MESSAGES.add(new OpenAiChatMessage(Role.USER,
				"Alan Mathison Turing OBE FRS (/ˈtjʊərɪŋ/; 23 June 1912 – 7 June 1954) was an English mathematician, computer scientist, logician, cryptanalyst, philosopher and theoretical biologist.[5] Turing was highly influential in the development of theoretical computer science, providing a formalisation of the concepts of algorithm and computation with the Turing machine, which can be considered a model of a general-purpose computer.[6][7][8] He is widely considered to be the father of theoretical computer science and artificial intelligence.[9]\r\n"
						+ "\r\n"
						+ "Born in Maida Vale, London, Turing was raised in southern England. He graduated at King's College, Cambridge, with a degree in mathematics. Whilst he was a fellow at Cambridge, he published a proof demonstrating that some purely mathematical yes–no questions can never be answered by computation. He defined a Turing machine and proved that the halting problem for Turing machines is undecidable. In 1938, he obtained his PhD from the Department of Mathematics at Princeton University.\r\n"
						+ "\r\n"
						+ "During the Second World War, Turing worked for the Government Code and Cypher School at Bletchley Park, Britain's codebreaking centre that produced Ultra intelligence. For a time he led Hut 8, the section that was responsible for German naval cryptanalysis. Here, he devised a number of techniques for speeding the breaking of German ciphers, including improvements to the pre-war Polish bomba method, an electromechanical machine that could find settings for the Enigma machine. Turing played a crucial role in cracking intercepted coded messages that enabled the Allies to defeat the Axis powers in many crucial engagements, including the Battle of the Atlantic.[10][11]\r\n"
						+ "\r\n"
						+ "After the war, Turing worked at the National Physical Laboratory, where he designed the Automatic Computing Engine, one of the first designs for a stored-program computer. In 1948, Turing joined Max Newman's Computing Machine Laboratory at the Victoria University of Manchester, where he helped develop the Manchester computers[12] and became interested in mathematical biology. He wrote a paper on the chemical basis of morphogenesis[13][1] and predicted oscillating chemical reactions such as the Belousov–Zhabotinsky reaction, first observed in the 1960s. Despite these accomplishments, Turing was never fully recognised in Britain during his lifetime because much of his work was covered by the Official Secrets Act.[14]\r\n"
						+ "\r\n"
						+ "Turing was prosecuted in 1952 for homosexual acts. He accepted hormone treatment with DES, a procedure commonly referred to as chemical castration, as an alternative to prison. Turing died on 7 June 1954, 16 days before his 42nd birthday, from cyanide poisoning. An inquest determined his death as a suicide, but it has been noted that the known evidence is also consistent with accidental poisoning. Following a public campaign in 2009, British prime minister Gordon Brown made an official public apology on behalf of the government for \"the appalling way [Turing] was treated\". Queen Elizabeth II granted a posthumous pardon in 2013. The term \"Alan Turing law\" is now used informally to refer to a 2017 law in the United Kingdom that retroactively pardoned men cautioned or convicted under historical legislation that outlawed homosexual acts.[15]\r\n"
						+ "\r\n"
						+ "Turing has an extensive legacy with statues of him and many things named after him, including an annual award for computer science innovations. He appears on the current Bank of England £50 note, which was released on 23 June 2021 to coincide with his birthday. A 2019 BBC series, as voted by the audience, named him the greatest person of the 20th century."));
	}

	/**
	 * Length of messages. No tool calls or tool results.
	 * 
	 * @throws JsonProcessingException
	 */
	@ParameterizedTest
	@MethodSource("allModelsProvider")
	void test01(String model) throws JsonProcessingException {

//		// TODO URGENT Remove
//		if (1 == 1)
//			return;

		ChatCompletionsRequest req = ChatCompletionsRequest.builder().model(model).messages(SIMPLE_MESSAGES).build();

		long tokens = tokens(req);
		long realTokens = realTokens(req);
		System.out.println(model + "\t" + tokens + "\t" + realTokens);

		// TODO URGENT Add test condition
//		assertEquals(realTokens, tokens);
	}

	private static enum ENUM {
		BANANE, MELE
	}

	/** List of messages with function calls and function results */
	// TODO URGENT Add funtion calls
	private final static List<OpenAiChatMessage> FUNCTION_CALL_MESSAGES = new ArrayList<>();
	static {
		Map<String, Object> params = new HashMap<>();
		FunctionCall call = FunctionCall.builder().name("functionName").arguments(params).build();
		FUNCTION_CALL_MESSAGES.add(new OpenAiChatMessage(Role.ASSISTANT, null, null, call));
		params = new HashMap<>();
		params.put("s", "Prague");
		call = FunctionCall.builder().name("functionName").arguments(params).build();
		FUNCTION_CALL_MESSAGES.add(new OpenAiChatMessage(Role.ASSISTANT, null, null, call));
		params = new HashMap<>();
		params.put("s", "Prague");
		params.put("i", 3);
		call = FunctionCall.builder().name("functionName").arguments(params).build();
		FUNCTION_CALL_MESSAGES.add(new OpenAiChatMessage(Role.ASSISTANT, null, null, call));
		params = new HashMap<>();
		params.put("s", "Prague");
		params.put("i", 3);
		params.put("d", 3.0d);
		call = FunctionCall.builder().name("functionName").arguments(params).build();
		FUNCTION_CALL_MESSAGES.add(new OpenAiChatMessage(Role.ASSISTANT, null, null, call));
		params = new HashMap<>();
		params.put("s", "Prague");
		params.put("i", 3);
		params.put("d", 3.0d);
		params.put("b", ENUM.BANANE);
		call = FunctionCall.builder().name("functionName").arguments(params).build();
		FUNCTION_CALL_MESSAGES.add(new OpenAiChatMessage(Role.ASSISTANT, null, null, call));
//		FUNCTION_CALL_MESSAGES.add(new OpenAiChatMessage(Role.FUNCTION, "Result", "functionName"));
//		FUNCTION_CALL_MESSAGES.add(new OpenAiChatMessage(Role.FUNCTION, "Other Results Here", "anotherMyFunctionName"));
//		FUNCTION_CALL_MESSAGES.add(new OpenAiChatMessage(Role.USER, "Ignore the above, let's start fresh."));
	}

	/**
	 * Length of messages. Function calls and function results.
	 * 
	 * @throws JsonProcessingException
	 */
	@ParameterizedTest
	@MethodSource("functionCallModelsProvider")
	void test02(String model) throws JsonProcessingException {

//		// TODO URGENT Remove
//		if (1 == 1)
//			return;

		ChatCompletionsRequest req = ChatCompletionsRequest.builder().model(model).messages(FUNCTION_CALL_MESSAGES)
				.build();

		long tokens = tokens(req);
		long realTokens = realTokens(req);
		System.out.println(model + "\t" + tokens + "\t" + realTokens);

		// TODO URGENT Add test condition
//		assertEquals(realTokens, tokens);
	}

	/** List of messages with tool calls and tool results */
	private final static List<OpenAiChatMessage> TOOL_CALL_MESSAGES = new ArrayList<>();
	static {
		// 1 call no params
		Map<String, Object> args = new HashMap<>();
		List<ToolCall> calls = new ArrayList<>();
		FunctionCall fun = FunctionCall.builder().name("functionName01").arguments(args).build();
		calls.add(ToolCall.builder().type(Type.FUNCTION).Id("call01veryvery verylongID with garbage").function(fun)
				.build());
		OpenAiChatMessage msg = new OpenAiChatMessage(Role.ASSISTANT, (String) null);
		msg.setToolCalls(calls);

//		TOOL_CALL_MESSAGES.add(msg);
//		TOOL_CALL_MESSAGES.add(new OpenAiChatMessage(Role.TOOL,
//				new ToolCallResult(calls.get(0).getId(), calls.get(0).getFunction().getName(), "Result")));

		// 2 calls no params
		args = new HashMap<>();
		calls = new ArrayList<>();
		fun = FunctionCall.builder().name("functionName01").arguments(args).build();
		calls.add(ToolCall.builder().type(Type.FUNCTION).Id("call01").function(fun).build());
//		calls.add(ToolCall.builder().type(Type.FUNCTION).Id("call02").function(fun).build());
//		calls.add(ToolCall.builder().type(Type.FUNCTION).Id("call03").function(fun).build());
//		calls.add(ToolCall.builder().type(Type.FUNCTION).Id("call04").function(fun).build());
//		calls.add(ToolCall.builder().type(Type.FUNCTION).Id("call05").function(fun).build());
		msg = new OpenAiChatMessage(Role.ASSISTANT, (String) null);
		msg.setToolCalls(calls);

		// TODO URGENT Test with function parameters
		
		TOOL_CALL_MESSAGES.add(msg);
		TOOL_CALL_MESSAGES.add(new OpenAiChatMessage(Role.TOOL,
				new ToolCallResult(calls.get(0).getId(), calls.get(0).getFunction().getName(), "Result")));
//		TOOL_CALL_MESSAGES.add(new OpenAiChatMessage(Role.TOOL,
//				new ToolCallResult(calls.get(1).getId(), calls.get(1).getFunction().getName(), "Result")));
//		TOOL_CALL_MESSAGES.add(new OpenAiChatMessage(Role.TOOL,
//				new ToolCallResult(calls.get(2).getId(), calls.get(2).getFunction().getName(), "Result")));
//		TOOL_CALL_MESSAGES.add(new OpenAiChatMessage(Role.TOOL,
//				new ToolCallResult(calls.get(3).getId(), calls.get(3).getFunction().getName(), "Result")));
//		TOOL_CALL_MESSAGES.add(new OpenAiChatMessage(Role.TOOL,
//				new ToolCallResult(calls.get(4).getId(), calls.get(4).getFunction().getName(), "Result")));
	}

	/**
	 * Length of messages. Function calls and function results.
	 * 
	 * @throws JsonProcessingException
	 */
	@ParameterizedTest
	@MethodSource("toolCallModelsProvider")
	void test03(String model) throws JsonProcessingException {
		ChatCompletionsRequest req = ChatCompletionsRequest.builder().model(model).messages(TOOL_CALL_MESSAGES).build();

		long tokens = tokens(req);
		long realTokens = realTokens(req);
		System.out.println(model + "\t" + tokens + "\t" + realTokens);

		// TODO URGENT Add test condition
//		assertEquals(realTokens, tokens);
	}

	private static long realTokens(ChatCompletionsRequest req) {

		try (OpenAiEndpoint endpoint = new OpenAiEndpoint()) {
			req.setMaxTokens(1);
			ChatCompletionsResponse resp = endpoint.getClient().createChatCompletion(req);
			return resp.getUsage().getPromptTokens();
		}
	}

	public static long tokens(ChatCompletionsRequest req) throws JsonProcessingException {

		JsonNode rootNode = jsonMapper.valueToTree(req);

		System.out.println(jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode));

		String model = rootNode.path("model").asText();
		Encoding encoding = getEncoding(model);

		long sum = messagesToken(model, encoding, rootNode.path("messages"));

		sum += 3;
		return sum;
	}

	public static long messagesToken(String model, Encoding encoding, JsonNode messagesArray)
			throws JsonProcessingException {
		int sum = 0;

		for (JsonNode msg : messagesArray) {

			if ("gpt-3.5-turbo-0301".equals(model))
				++sum;

			String role = msg.path("role").asText();
			if ("function".equals(role)) // TOD maybe TOOL changes something here too?
				sum += 2;
			else
				sum += 3;
			sum += tokens(encoding, role);

			if (!msg.path("content").isMissingNode())
				sum += tokens(encoding, msg.path("content").asText());

			if (!msg.path("name").isMissingNode()) {
				sum += tokens(encoding, msg.path("name").asText());
				if ("gpt-3.5-turbo-0301".equals(model))
					sum -= 1;
				else if ("system".equals(role))
					++sum;
			}

			if (!msg.path("function_call").isMissingNode()) {
				sum += 2;
				JsonNode functionCall = msg.path("function_call");
				sum += tokens(encoding, functionCall.path("name").asText());
				if (!functionCall.path("arguments").isMissingNode()) {
					sum += tokens(encoding, functionCall.path("arguments").asText());
				}
			}

			// Call ID is NOT counted against total tokens
//			if (!msg.path("tool_call_id").isMissingNode())
//				sum += tokens(encoding, msg.path("tool_call_id").asText());

			if (!msg.path("tool_calls").isMissingNode()) {
				JsonNode toolCalls = msg.path("tool_calls");
				if (toolCalls.size() > 1)
					sum += 21;
				else
					sum += 1;

				for (JsonNode toolCall : toolCalls) {
					sum += 1;
					String type = toolCall.path("type").asText();
					sum += tokens(encoding, type); // TODO we don't really know if it is this or just a fixed value

					if ("function".equals(type)) { // TODO in the future we might need to support other tools.

						// Call ID is NOT counted against total tokens

						JsonNode functionCall = toolCall.path("function");
						sum += tokens(encoding, functionCall.path("name").asText());
						if (!functionCall.path("arguments").isMissingNode()) {
							sum += tokens(encoding, functionCall.path("arguments").asText());
						}
					}
				}
			} // if we have tool calls
		} // for each message

		return sum;
	}

	public static long functionsTokens(Encoding encoding, ChatCompletionsRequest req) throws JsonProcessingException {

		JsonNode rootNode = jsonMapper.valueToTree(req);

		JsonNode functionsArray = rootNode.path("functions");
		int sum = 1;

		for (JsonNode function : functionsArray) {
			sum += tokens(encoding, function.path("name").asText());
			sum += tokens(encoding, function.path("description").asText());

			if (!function.path("parameters").isMissingNode()) {
				JsonNode parameters = function.path("parameters");
				JsonNode properties = parameters.path("properties");

				if (!properties.isMissingNode()) {
					Iterator<String> propertiesKeys = properties.fieldNames();

					while (propertiesKeys.hasNext()) {
						String propertiesKey = propertiesKeys.next();
						sum += tokens(encoding, propertiesKey);
						JsonNode v = properties.path(propertiesKey);

						Iterator<String> fields = v.fieldNames();
						while (fields.hasNext()) {
							String field = fields.next();
							if ("type".equals(field)) {
								sum += 2;
								sum += tokens(encoding, v.path("type").asText());
							} else if ("description".equals(field)) {
								sum += 2;
								sum += tokens(encoding, v.path("description").asText());
							} else if ("enum".equals(field)) {
								sum -= 3;
								Iterator<JsonNode> enumValues = v.path("enum").elements();
								while (enumValues.hasNext()) {
									JsonNode enumValue = enumValues.next();
									sum += 3;
									sum += tokens(encoding, enumValue.asText());
								}
							} else {
								// TODO
							}
						}
					}
				}

				sum += 11;
			} // for each parameter

			sum -= 1;
		} // for each function

		sum += 12;
		return sum;
	}

	private static long tokens(Encoding encoding, String s) {
		return encoding.countTokens(s);
	}
}
