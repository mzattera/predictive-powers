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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.mzattera.predictivepowers.openai.client.OpenAiException;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsResponse;
import io.github.mzattera.predictivepowers.openai.client.chat.Function;
import io.github.mzattera.predictivepowers.openai.client.chat.OpenAiTool;
import io.github.mzattera.predictivepowers.openai.client.models.Model;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.FunctionCallTest.GetCurrentWeatherTool;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatMessage.Role;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiModelMetaData;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiModelMetaData.SupportedApi;

class OpenAiModelServiceTest {

	// Models still returned by models API, but decommissioned
	private final static Set<String> OLD_MODELS = new HashSet<>();
	static {
		// Not needed any longer, left here in case the problem re-appears
	}

	private final static Function FUNCTION = new Function("get_current_weather", //
			"Get the current weather in a given location.", //
			GetCurrentWeatherTool.GetCurrentWeatherParameters.class);
	private final static List<Function> FUNCTIONS = new ArrayList<>();
	static {
		FUNCTIONS.add(FUNCTION);
	}
	private final static List<OpenAiTool> TOOLS = new ArrayList<>();
	static {
		TOOLS.add(new OpenAiTool(new GetCurrentWeatherTool()));
	}

	/**
	 * Check list of models is complete.
	 */
	@Test
	void test01() {
		try (OpenAiEndpoint oai = new OpenAiEndpoint()) {
			OpenAiModelService modelSvc = oai.getModelService();

			Set<String> deprecated = new HashSet<>(OLD_MODELS);
			Set<String> actual = OpenAiModelService.getModelsMetadata() //
					.map(OpenAiModelMetaData::getModel) //
					.collect(Collectors.toSet());

			List<Model> models = oai.getClient().listModels();
			assertTrue(models.size() > 0);

			for (Model m : models) {
				String model = m.getId();

				if (model.contains("ft-"))
					continue; // fine-tunes can be ignored
				if (deprecated.remove(model))
					continue; // Skip old models

				boolean found = actual.remove(model);
				if (!found)
					System.out.println("NEW model not listed: " + m);
				assertTrue(found);

				OpenAiModelMetaData md = modelSvc.get(model);
				assertTrue(md != null);
				if ((md.getSupportedApi() == SupportedApi.IMAGES) || (md.getSupportedApi() == SupportedApi.STT))
					continue;

				// Check that tokenizer and context size are provided
				if (md.getSupportedApi() != SupportedApi.TTS) // Text to speech models do not have encoders
					assertTrue(modelSvc.getTokenizer(model) != null);
				assertTrue(modelSvc.getContextSize(model) > 0);
			}

			// Check OLD_MODELS does not contain things we do not need any longer.
			for (String m : deprecated) {
				System.out.println("OLD model no longer there: " + m);
			}
			assertEquals(0, deprecated.size());

			// Check CONTEXT_SIZES does not contain things we do not need any longer.
			int skip = 0;
			for (String m : actual) {
				if (m.startsWith("gpt-4-32k")) { // For some reason, we do not have access to these models
					++skip;
					continue;
				}
				System.out.println("Model no longer there: " + m);
			}
			assertEquals(skip, actual.size());

		} // Close endpoint
	}

	@Test
	void test02() {
		try (OpenAiEndpoint oai = new OpenAiEndpoint()) {
			String id = "gpt-3.5-turbo";
			Model model = oai.getClient().retrieveModel(id);
			assertEquals(id, model.getId());
			assertEquals("model", model.getObject());
		} // Close endpoint
	}

	/** @return The meta data for all chat models */
	static Stream<OpenAiModelMetaData> allChatModelsProvider() {
		try (OpenAiEndpoint endpoint = new OpenAiEndpoint()) {
			OpenAiModelService modelSvc = endpoint.getModelService();
			return modelSvc.listModels().stream() //
					.filter(model -> !model.startsWith("gpt-4-32k")) //
					.map(model -> modelSvc.get(model)) //
					.filter(meta -> meta != null) //
					.filter(meta -> meta.getSupportedApi() == SupportedApi.CHAT);
		}
	}

	/**
	 * Check function call mode is listed correctly.
	 */
	@ParameterizedTest
	@MethodSource("allChatModelsProvider")
	void test03(OpenAiModelMetaData md) {
		try (OpenAiEndpoint oai = new OpenAiEndpoint()) {
			ChatCompletionsRequest req = ChatCompletionsRequest.builder().model(md.getModel()).build();

			// Bypass setTools() to make sure we test the correct function call type
			switch (md.getSupportedCallType()) {
			case TOOLS:
				req.setFunctions(null);
				req.setTools(TOOLS);
				req.getMessages().clear();
				req.getMessages().add(new OpenAiChatMessage(Role.USER, "What is the temperature il London?"));
				ChatCompletionsResponse result = oai.getClient().createChatCompletion(req);
				assertTrue(result.getChoices().get(0).getMessage().getToolCalls().size() != 0);
				assertNull(result.getChoices().get(0).getMessage().getFunctionCall());
				break;
			case FUNCTIONS:
				req.setFunctions(FUNCTIONS);
				req.setTools(null);
				req.getMessages().clear();
				req.getMessages().add(new OpenAiChatMessage(Role.USER, "What is the temperature il London?"));
				result = oai.getClient().createChatCompletion(req);
				assertNull(result.getChoices().get(0).getMessage().getToolCalls());
				assertTrue(result.getChoices().get(0).getMessage().getFunctionCall() != null);
				break;
			case NONE:
				req.setFunctions(FUNCTIONS);
				req.setTools(null);
				req.getMessages().clear();
				req.getMessages().add(new OpenAiChatMessage(Role.USER, "What is the temperature il London?"));
				assertThrows(OpenAiException.class, () -> oai.getClient().createChatCompletion(req));
				req.setFunctions(null);
				req.setTools(TOOLS);
				req.getMessages().clear();
				req.getMessages().add(new OpenAiChatMessage(Role.USER, "What is the temperature il London?"));
				assertThrows(OpenAiException.class, () -> oai.getClient().createChatCompletion(req));
				break;
			default:
				throw new IllegalArgumentException(); // paranoid
			}
		} // Close endpoint
	}

	// TODO Fix to see if we can check context length somehow

	/**
	 * Check max context size it set correctly.
	 */
	@ParameterizedTest
	@MethodSource("allChatModelsProvider")
	void testContextLength(OpenAiModelMetaData md) {
//		try (OpenAiEndpoint oai = new OpenAiEndpoint()) {
//			OpenAiChatService bot = oai.getChatService(md.getModel(), "You are an helpful assistant.");
//			bot.setMaxNewTokens(Integer.MAX_VALUE);
//			try {
//				bot.chat("Hi!");
//				assertTrue(false, "Call should have failed.");
//			} catch (OpenAiException e) {
//				assertEquals(e.getMaxContextLength(), md.getContextSize());
//			}
//		} // Close endpoint
	}
}
