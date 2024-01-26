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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.mzattera.predictivepowers.openai.client.chat.Function;
import io.github.mzattera.predictivepowers.openai.client.chat.OpenAiTool;
import io.github.mzattera.predictivepowers.openai.client.models.Model;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiModelMetaData;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiModelMetaData.SupportedApi;
import io.github.mzattera.predictivepowers.openai.services.ToolCallTest.GetCurrentWeatherParameters;

class OpenAiModelServiceTest {

	// TODO URGENT add tests to check max context size for all models

	// Models still returned by models API, but decommissioned
	private final static Set<String> OLD_MODELS = new HashSet<>();
	static {
		// Not needed any longer, left here in case the problem re-appears
	}

	private final static Function FUNCTION = Function.builder() //
			.name("get_current_weather") //
			.description("Get the current weather in a given location.") //
			.parameters(GetCurrentWeatherParameters.class).build() //
	;
	private final static List<Function> FUNCTIONS = new ArrayList<>();
	static {
		FUNCTIONS.add(FUNCTION);
	}
	private final static List<OpenAiTool> TOOLS = new ArrayList<>();
	static {
		TOOLS.add(new OpenAiTool(FUNCTION));
	}

	/**
	 * Check list of models is complete.
	 */
	@Test
	void test01() {
		try (OpenAiEndpoint oai = new OpenAiEndpoint()) {
			OpenAiModelService modelSvc = oai.getModelService();
			OpenAiChatService chatSvc = oai.getChatService();

			Set<String> deprecated = new HashSet<>(OLD_MODELS);
			Set<String> actual = OpenAiModelService.getModelsMetadata() //
					.map(OpenAiModelMetaData::getModel) //
					.collect(Collectors.toSet());

			List<Model> models = oai.getClient().listModels();
			assertTrue(models.size() > 0);

			for (Model m : models) {
				String model = m.getId();

				// TODO add these to metadata as well
				if (model.startsWith("whisper"))
					continue; // Whisper models do not need size
				if (model.startsWith("dall-e"))
					continue; // DALL-E models do not need size
				if (model.contains("ft-"))
					continue; // fine-tunes can be ignored
				if (deprecated.remove(model))
					continue; // Skip old models

				// Check that tokenizer and context size are provided
				if (!model.startsWith("tts-")) // Text to speech models do not have encoders
					assertTrue(modelSvc.getTokenizer(model) != null);
				assertTrue(modelSvc.getContextSize(model) > 0);

				assertTrue(actual.remove(model));
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

	/** Return names for all chat /completions models */
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
			OpenAiChatService chatSvc = oai.getChatService();

			// Bypass setDefaultTools() to make sure we test the correct function call type
			switch (md.getSupportedCallType()) {
			case TOOLS:
				chatSvc.getDefaultReq().setFunctions(null);
				chatSvc.getDefaultReq().setTools(TOOLS);
				OpenAiTextCompletion result = chatSvc.chat("What is the temperature il London?");
				assertTrue(result.hasToolCalls());
				break;
			case FUNCTIONS:
				chatSvc.getDefaultReq().setFunctions(FUNCTIONS);
				chatSvc.getDefaultReq().setTools(null);
				result = chatSvc.chat("What is the temperature il London?");
				assertTrue(result.hasToolCalls());
				break;
			case NONE:
				chatSvc.getDefaultReq().setFunctions(FUNCTIONS);
				chatSvc.getDefaultReq().setTools(null);
				try {
					result = chatSvc.chat("What is the temperature il London?");
					assertFalse(result.hasToolCalls());
				} catch (Exception e) {
					// the model should error indeed
				}
				chatSvc.getDefaultReq().setFunctions(null);
				chatSvc.getDefaultReq().setTools(TOOLS);
				try {
					result = chatSvc.chat("What is the temperature il London?");
					assertFalse(result.hasToolCalls());
				} catch (Exception e) {
					// the model should error indeed
				}
				break;
			default:
				throw new IllegalArgumentException(); // paranoid
			}
		} // Close endpoint
	}
}
