/*
 * Copyright 2023-2025 Massimiliano "Maxi" Zattera
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
package io.github.mzattera.predictivepowers.openai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.junit.jupiter.api.condition.EnabledIf;
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

import io.github.mzattera.predictivepowers.EndpointException;
import io.github.mzattera.predictivepowers.TestConfiguration;
import io.github.mzattera.predictivepowers.examples.FunctionCallExample.GetCurrentWeatherTool;
import io.github.mzattera.predictivepowers.openai.OpenAiChatService;
import io.github.mzattera.predictivepowers.openai.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.OpenAiModelService;
import io.github.mzattera.predictivepowers.openai.OpenAiUtil;
import io.github.mzattera.predictivepowers.openai.OpenAiModelService.OpenAiModelMetaData;
import io.github.mzattera.predictivepowers.openai.OpenAiModelService.OpenAiModelMetaData.SupportedApi;
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

	static boolean hasServices() {
		return services().findAny().isPresent();
	}

	// Models still returned by models API, but de-commissioned (we cannot use them)
	private final static Set<String> OLD_MODELS = new HashSet<>();
	static {
		OLD_MODELS.add("gpt-3.5-turbo-16k");
	}

	@ParameterizedTest
	@MethodSource("services")
	@EnabledIf("hasServices")
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
				if ((!md.getSupportedApis().contains(SupportedApi.CHAT)
						&& !md.getSupportedApis().contains(SupportedApi.ASSISTANTS) //
						&& !md.getSupportedApis().contains(SupportedApi.RESPONSES)) //
						|| "codex-mini-latest".equals(model) // TODO Add tokenizer once we find out what it is
						|| model.startsWith("computer-use-preview") // TODO Add tokenizer once we find out what it
																	// is
						|| model.startsWith("gpt-realtime")) // TODO Add tokenizer once we find out what it is
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
	@EnabledIf("hasServices")
	@DisplayName("Check function call mode incl. strict mode support")
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
						assertThrows(EndpointException.class,
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
					assertThrows(EndpointException.class,
							() -> modelSvc.getEndpoint().getClient().chat().completions().create(b.build()));
					b.functions(functions);
					b.tools(JsonMissing.of());
					assertThrows(EndpointException.class,
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
	@EnabledIf("hasServices")
	@DisplayName("Check strict mode format for responses")
	public void testStrictOutputFormatMode(OpenAiModelService modelSvc) throws JsonProcessingException {

		boolean error = false;

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
					try {
						chatSvc.setDefaultRequest(
								chatSvc.getDefaultRequest().toBuilder().responseFormat(strictFormat).build());
						chatSvc.complete(prompt);
					} catch (Exception e) {
						System.out.println("\tCaused an exception: " + e.getMessage());
						error = true;
					}
				} else {
					// Let's make sure the model does not support structured output
					// need to bypass chatSvc.setResponseFormat();
					try {
						chatSvc.setDefaultRequest(
								chatSvc.getDefaultRequest().toBuilder().responseFormat(strictFormat).build());
						chatSvc.complete(prompt);
						System.out.println("\tWas expected to cause exception but didn't");
						error = true;
					} catch (EndpointException e) {
						// This is expected
					}
				}
			} // for each model
		} // Close endpoint

		assertFalse(error);
	}

	@ParameterizedTest
	@MethodSource("services")
	@EnabledIf("hasServices")
	@DisplayName("Check max context size")
	public void testContextLength(OpenAiModelService modelSvc) {

		String longestPrompt = "T0KKns".repeat(512_000 / 4);

		try (modelSvc; OpenAiChatService chatSvc = modelSvc.getEndpoint().getChatService();) {

			boolean error = false;
			List<String> oaiModels = modelSvc.listModels();
			for (String model : oaiModels) {

				if (OLD_MODELS.contains(model))
					continue; // Skip old models

				OpenAiModelMetaData md = modelSvc.get(model);
				assertTrue(md != null, "Missing model: " + model);

				if (!md.supportsApi(SupportedApi.CHAT))
					continue; // TODO test with ASSISTANTS or RESPONSE
				if (md.supportsAudioInput() || md.supportsAudioOutput() || md.supportsApi(SupportedApi.IMAGES))
					continue; // TODO test audio and (some) models

				assertTrue(md.getContextSize() != null, "Missing context or model: " + model);
				System.out.println(model + " -> " + md.getContextSize());

				chatSvc.setModel(model);
				String prompt = longestPrompt.substring("T0KKns".length() * (modelSvc.getContextSize(model) / 4 + 10));

				try {
					chatSvc.chat(prompt);
					System.err.println("Call to " + model + " should have failed");
					error = true;
				} catch (EndpointException e) {
					int max = OpenAiUtil.getExceptionData((OpenAIException) e.getCause()).getContextSize();
					if (max < 0) {
						// we cannot test
						System.out.println("\t--" + e.getMessage());
						continue;
					}

					if (max != md.getContextSize()) {
						System.err.println(model + " context is " + max + " and now set to " + md.getContextSize());
						error = true;
					}
				}
			}

			assertFalse(error, "An error occurred testing model context lenght; check error console.");
		} // Close endpoint
	}

	@ParameterizedTest
	@MethodSource("services")
	@EnabledIf("hasServices")
	@DisplayName("Check max new tokens")
	public void testMaxNewTkn(OpenAiModelService modelSvc) {

		try (modelSvc; OpenAiChatService chatSvc = modelSvc.getEndpoint().getChatService();) {

			boolean error = false;
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

					if (max != md.getMaxNewTokens()) {
						System.err.println(
								model + " max new token is " + max + " and now set to " + md.getMaxNewTokens());
						error = true;
					}
				}
			}

			assertFalse(error, "An error occurred testing model max output lenght; check error console.");
		} // Close endpoint
	}
}
