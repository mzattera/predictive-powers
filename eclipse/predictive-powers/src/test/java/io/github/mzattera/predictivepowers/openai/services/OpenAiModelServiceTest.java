/*
 * Copyright 2023-2024 Massimiliano "Maxi" Zattera
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.openai.core.JsonMissing;
import com.openai.core.JsonValue;
import com.openai.errors.OpenAIException;
import com.openai.models.FunctionDefinition;
import com.openai.models.ResponseFormatJsonSchema;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionCreateParams.Builder;
import com.openai.models.chat.completions.ChatCompletionCreateParams.Function;
import com.openai.models.chat.completions.ChatCompletionCreateParams.ResponseFormat;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionTool;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;

import io.github.mzattera.predictivepowers.TestConfiguration;
import io.github.mzattera.predictivepowers.examples.FunctionCallExample.GetCurrentWeatherTool;
import io.github.mzattera.predictivepowers.openai.client.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiModelMetaData;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiModelMetaData.SupportedApi;
import io.github.mzattera.predictivepowers.openai.util.OpenAiUtil;
import io.github.mzattera.predictivepowers.services.Tool;
import io.github.mzattera.predictivepowers.services.messages.JsonSchema;

class OpenAiModelServiceTest {

	private static List<OpenAiEndpoint> endpoint = new ArrayList<>();;

	@BeforeAll
	static void init() {
		if (TestConfiguration.TEST_OPENAI_SERVICES)
			endpoint.add(new OpenAiEndpoint());
	}

	@AfterAll
	static void tearDown() {
		TestConfiguration.close(endpoint.stream().collect(Collectors.toList()));
	}

	static Stream<OpenAiModelService> services() {
		return endpoint.stream().map(ep -> ep.getModelService());
	}

	// Models still returned by models API, but de-commissioned (we cannot use them)
	private final static Set<String> OLD_MODELS = new HashSet<>();
	static {
		OLD_MODELS.add("gpt-3.5-turbo-16k");
	}

	@ParameterizedTest
	@MethodSource("services")
	@DisplayName("Check list of models is complete.")
	public void testCompleteness(OpenAiModelService modelSvc) {
		try (modelSvc) {
			Set<String> deprecated = new HashSet<>(OLD_MODELS);
			List<String> defined = OpenAiModelService.getDefinedModelIDs();
			List<String> oaiModels = modelSvc.listModels();
			assertTrue(oaiModels.size() > 0);

			for (String model : oaiModels) {
				System.out.println(model);
				defined.remove(model);

				if (model.contains("ft"))
					continue; // fine-tunes can be ignored
				if (deprecated.remove(model))
					continue; // Skip old models

				OpenAiModelMetaData md = modelSvc.get(model);
				assertTrue(md != null, "Missing model: " + model);
				assertEquals(model, md.getModel(), "Model mismatch: " + model + " " + md.getModel());

				// Check that tokenizer and context size are provided
				if (!md.getSupportedApis().contains(SupportedApi.CHAT)
						&& !md.getSupportedApis().contains(SupportedApi.ASSISTANTS) //
						&& !md.getSupportedApis().contains(SupportedApi.RESPONSES) //
						|| "codex-mini-latest".equals(model) // TODO Add tokenizer once we find out what it is
						|| model.startsWith("computer-use-preview")) // TODO Add tokenizer once we find out what it is
					continue;
				assertTrue(modelSvc.getTokenizer(model) != null);
				assertTrue(modelSvc.getContextSize(model, -1) > 0);
				assertTrue(modelSvc.getMaxNewTokens(model, -1) > 0);
			}

			// Check OLD_MODELS does not contain things we do not need any longer.
			for (String m : deprecated) {
				System.out.println("OLD model no longer there: " + m);
			}
			assertEquals(0, deprecated.size());

			// Check we do not keep things we do not need any longer.
			for (String m : defined) {
				System.out.println("Model no longer there: " + m);
			}
			assertEquals(0, defined.size());

		} // Close endpoint
	}

	// TODO Add tests to check supported inputs and outputs

	@ParameterizedTest
	@MethodSource("services")
	@DisplayName("Check function call mode incl. strict")
	@SuppressWarnings({ "deprecation", "unchecked" })
	public void testCallMode(OpenAiModelService modelSvc) throws Exception {
		try (modelSvc;
				OpenAiChatService chatSvc = modelSvc.getEndpoint().getChatService();
				Tool t = new GetCurrentWeatherTool();) {

			List<String> oaiModels = modelSvc.listModels();
			for (String model : oaiModels) {

				if (OLD_MODELS.contains(model))
					continue; // Skip old models

				OpenAiModelMetaData md = modelSvc.get(model);
				assertTrue(md != null, "Missing model: " + model);

				if (!md.supportsApi(SupportedApi.CHAT))
					continue; // TODO test with ASSISTANTS or RESPONSE
				if (md.supportsAudioInput() || md.supportsAudioOutput())
					continue; // TODO test audio models

				System.out.println(model + " -> " + md.getSupportedCallType() + ": " + md.supportsStrictModeToolCall());
				Builder b = chatSvc.getDefaultRequest().toBuilder();
				b.model(model);

				List<ChatCompletionTool> tools = List.of( //
						ChatCompletionTool.builder().function( //
								FunctionDefinition.builder() //
										.name(t.getId()) //
										.description(t.getDescription()) //
										.strict(false)
										.parameters(OpenAiUtil.toFunctionParameters(t.getParameters(), false)).build() //
						).build());
				List<ChatCompletionTool> toolsStrict = List.of( //
						ChatCompletionTool.builder().function( //
								FunctionDefinition.builder() //
										.name(t.getId()) //
										.description(t.getDescription()) //
										.strict(true)
										.parameters(OpenAiUtil.toFunctionParameters(t.getParameters(), true)).build() //
						).build());
				List<Function> functions = List.of( //
						Function.builder() //
								.name(t.getId()) //
								.description(t.getDescription()) //
								.parameters(OpenAiUtil.toFunctionParameters(t.getParameters(), false)).build());

				switch (md.getSupportedCallType()) {
				case TOOLS:
					b.messages(List.of( //
							ChatCompletionMessageParam.ofUser( //
									ChatCompletionUserMessageParam.builder() //
											.content("What is the temperature il London?") //
											.build() //
							)));
					b.functions(JsonMissing.of());

					if (md.supportsStrictModeToolCall()) {
						b.tools(toolsStrict);
						ChatCompletion resp = modelSvc.getEndpoint().getClient().chat().completions().create(b.build());
						assertTrue(resp.choices().get(0).message().toolCalls().orElse(new ArrayList<>()).size() > 0);
						assertTrue(resp.choices().get(0).message().functionCall().isEmpty());
					} else {
						// Make sure we do not support strict
						b.tools(toolsStrict);
						assertThrows(OpenAIException.class,
								() -> modelSvc.getEndpoint().getClient().chat().completions().create(b.build()));

						// Test non strict works
						b.tools(tools);
						ChatCompletion resp = modelSvc.getEndpoint().getClient().chat().completions().create(b.build());
						assertTrue(resp.choices().get(0).message().toolCalls().orElse(new ArrayList<>()).size() > 0);
						assertTrue(resp.choices().get(0).message().functionCall().isEmpty());
					}
					break;
				case FUNCTIONS:
					b.functions(functions);
					b.tools(JsonMissing.of());
					b.messages(List.of( //
							ChatCompletionMessageParam.ofUser( //
									ChatCompletionUserMessageParam.builder() //
											.content("What is the temperature il London?") //
											.build() //
							)));

					ChatCompletion resp = modelSvc.getEndpoint().getClient().chat().completions().create(b.build());
					assertTrue(resp.choices().get(0).message().toolCalls().isEmpty());
					assertTrue(resp.choices().get(0).message().functionCall().isPresent());
					break;
				case NONE:
					b.messages(List.of( //
							ChatCompletionMessageParam.ofUser( //
									ChatCompletionUserMessageParam.builder() //
											.content("What is the temperature il London?") //
											.build() //
							)));
					b.functions(JsonMissing.of());
					b.tools(tools);
					assertThrows(OpenAIException.class,
							() -> modelSvc.getEndpoint().getClient().chat().completions().create(b.build()));
					b.functions(functions);
					b.tools(JsonMissing.of());
					assertThrows(OpenAIException.class,
							() -> modelSvc.getEndpoint().getClient().chat().completions().create(b.build()));
					break;
				default:
					throw new IllegalArgumentException(); // paranoid
				}
			} // for each model
		} // Close endpoint
	}

	@ParameterizedTest
	@MethodSource("services")
	@DisplayName("Check strict mode format")
	public void testStrictMode(OpenAiModelService modelSvc) throws JsonProcessingException {
		try (modelSvc; OpenAiChatService chatSvc = modelSvc.getEndpoint().getChatService();) {

			JsonSchema schema = JsonSchema.fromSchema(GetCurrentWeatherTool.Parameters.class);

			String prompt = "Extract data from the below sentence and return them as JSON. "
					+ "Location is the city of Padua, the unit is Farenheit. "
					+ "Output data according to the below schema\n" + schema.asJsonSchema();

			@SuppressWarnings("unchecked")
			ChatCompletionCreateParams.ResponseFormat strictFormat = ResponseFormat
					.ofJsonSchema(ResponseFormatJsonSchema.builder() //
							.jsonSchema( //
									ResponseFormatJsonSchema.JsonSchema.builder() //
											.name(schema.getTitle() == null ? "null"
													: schema.getTitle().replaceAll("[^a-zA-Z0-9_-]", "")) //
											.description(schema.getDescription()) //
											.schema(JsonValue.from(schema.asMap(true))).build() //
							).build());

			List<String> oaiModels = modelSvc.listModels();
			for (String model : oaiModels) {

				if (OLD_MODELS.contains(model))
					continue; // Skip old models

				OpenAiModelMetaData md = modelSvc.get(model);
				assertTrue(md != null, "Missing model: " + model);

				if (!md.supportsApi(SupportedApi.CHAT))
					continue; // TODO test with ASSISTANTS or RESPONSE
				if (md.supportsAudioInput() || md.supportsAudioOutput())
					continue; // TODO test audio models

				System.out.println(model + " -> " + md.supportsStructuredOutput());
				chatSvc.setModel(model);

				if (md.supportsStructuredOutput()) {
					// if this work, then the model does support structured output
					// need to bypass chatSvc.setResponseFormat();
					chatSvc.setDefaultRequest(
							chatSvc.getDefaultRequest().toBuilder().responseFormat(strictFormat).build());
					chatSvc.complete(prompt);
				} else {
					// Let's make sure the model does not support structured output
					// need to bypass chatSvc.setResponseFormat();
					chatSvc.setDefaultRequest(
							chatSvc.getDefaultRequest().toBuilder().responseFormat(strictFormat).build());
					assertThrows(OpenAIException.class, () -> chatSvc.complete(prompt));
				}
			} // for each model
		} // Close endpoint
	}

	@ParameterizedTest
	@MethodSource("services")
	@DisplayName("Check max context size")
	public void testContextLength(OpenAiModelService modelSvc) {

		try (modelSvc; OpenAiChatService chatSvc = modelSvc.getEndpoint().getChatService();) {

			List<String> oaiModels = modelSvc.listModels();
			for (String model : oaiModels) {

				if (OLD_MODELS.contains(model))
					continue; // Skip old models

				OpenAiModelMetaData md = modelSvc.get(model);
				assertTrue(md != null, "Missing model: " + model);

				if (!md.supportsApi(SupportedApi.CHAT))
					continue; // TODO test with ASSISTANTS or RESPONSE
				if (md.supportsAudioInput() || md.supportsAudioOutput())
					continue; // TODO test audio models

				chatSvc.setModel(model);
				String prompt = ". /".repeat(modelSvc.getContextSize(model));

				try {
					chatSvc.chat(prompt);
					assertTrue(false, "Call should have failed.");
				} catch (OpenAIException e) {
					int max = OpenAiUtil.getExceptionData(e).getContextSize();
					System.out.println(model + " context is " + max);
					if (max < 0) {
						// we cannot test
						System.out.println("\t--" + e.getMessage());
						continue;
					}
					assertEquals(max, md.getContextSize());
				}
			} // Close endpoint
		}
	}

@ParameterizedTest
	@MethodSource("services")
	@DisplayName("Check max new tokens")
	public void testMaxNewTkn(OpenAiModelService modelSvc) {

		try (modelSvc; OpenAiChatService chatSvc = modelSvc.getEndpoint().getChatService();) {

			List<String> oaiModels = modelSvc.listModels();
			for (String model : oaiModels) {

				if (OLD_MODELS.contains(model))
					continue; // Skip old models

				OpenAiModelMetaData md = modelSvc.get(model);
				assertTrue(md != null, "Missing model: " + model);

				if (!md.supportsApi(SupportedApi.CHAT))
					continue; // TODO test with ASSISTANTS or RESPONSE
				if (md.supportsAudioInput() || md.supportsAudioOutput())
					continue; // TODO test audio models

				chatSvc.setModel(model);
				chatSvc.setMaxNewTokens(1_047_576 * 5);
				chatSvc.setDefaultRequest(
						chatSvc.getDefaultRequest().toBuilder()
								.messages(List.of(ChatCompletionMessageParam
										.ofUser(ChatCompletionUserMessageParam.builder().content("Hello").build())))
								.build());
				System.out.println(model + " max new tokens is " + md.getMaxNewTokens());

				try {
					// Must bypass the call to cause exception
					modelSvc.getEndpoint().getClient().chat().completions().create(chatSvc.getDefaultRequest());
					assertTrue(false, "Call should have failed.");
				} catch (OpenAIException e) {
					int max = OpenAiUtil.getExceptionData(e).getMaxNewTokens();
					if (max < 0) {
						// we cannot test
						System.out.println("\t--" + e.getMessage());
						continue;
					}
					assertEquals(max, md.getMaxNewTokens());
				}
			} // Close endpoint
		}
	}
	
// TODO URGENT Test that vision and audio support are set correctly by sending images & audio
// If you do, probably you can remove testing audio files

//	@ParameterizedTest
//	@MethodSource("services")
	@DisplayName("Check audio and video support")
	public void testAudioVideo(OpenAiModelService modelSvc) {

		try (modelSvc; OpenAiChatService chatSvc = modelSvc.getEndpoint().getChatService();) {

			List<String> oaiModels = modelSvc.listModels();
			for (String model : oaiModels) {

				if (OLD_MODELS.contains(model))
					continue; // Skip old models

				OpenAiModelMetaData md = modelSvc.get(model);
				assertTrue(md != null, "Missing model: " + model);

				if (!md.supportsApi(SupportedApi.CHAT))
					continue; // TODO test with ASSISTANTS or RESPONSE
				if (md.supportsAudioInput() || md.supportsAudioOutput())
					continue; // TODO test audio models

				chatSvc.setModel(model);
				chatSvc.setMaxNewTokens(1_047_576 * 5);
				chatSvc.setDefaultRequest(
						chatSvc.getDefaultRequest().toBuilder()
								.messages(List.of(ChatCompletionMessageParam
										.ofUser(ChatCompletionUserMessageParam.builder().content("Hello").build())))
								.build());
				System.out.println(model + " max new tokens is " + md.getMaxNewTokens());

				try {
					// Must bypass the call to cause exception
					modelSvc.getEndpoint().getClient().chat().completions().create(chatSvc.getDefaultRequest());
					assertTrue(false, "Call should have failed.");
				} catch (OpenAIException e) {
					int max = OpenAiUtil.getExceptionData(e).getMaxNewTokens();
					if (max < 0) {
						// we cannot test
						System.out.println("\t--" + e.getMessage());
						continue;
					}
					assertEquals(max, md.getMaxNewTokens());
				}
			} // Close endpoint
		}
	}
	
}
