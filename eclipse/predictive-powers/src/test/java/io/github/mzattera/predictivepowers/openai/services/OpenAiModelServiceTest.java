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

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.openai.client.chat.Function;
import io.github.mzattera.predictivepowers.openai.client.chat.OpenAiTool;
import io.github.mzattera.predictivepowers.openai.client.models.Model;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiModelMetaData;
import io.github.mzattera.predictivepowers.openai.services.ToolCallTest.GetCurrentWeatherParameters;

class OpenAiModelServiceTest {

	private final static Set<String> OLD_MODELS = new HashSet<>();
	static {
		// Model for decommissioned search point seem to be still there for some users
		OLD_MODELS.add("ada-code-search-code");
		OLD_MODELS.add("ada-code-search-text");
		OLD_MODELS.add("ada-search-document");
		OLD_MODELS.add("ada-search-query");
		OLD_MODELS.add("babbage-search-document");
		OLD_MODELS.add("babbage-search-query");
		OLD_MODELS.add("curie-search-document");
		OLD_MODELS.add("curie-search-query");
		OLD_MODELS.add("davinci-search-document");
		OLD_MODELS.add("davinci-search-query");
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

				// Test function calling for GPT models to ensure supported call model is set
				// correctly
				if (model.startsWith("gpt-")) {
					System.out.println("Testing function call for " + model);
					chatSvc.setModel(model);
					chatSvc.clearConversation();

					// Bypass setDefaultTools() to make sure we test the correct function call type
					switch (modelSvc.getSupportedCall(model)) {
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
				}

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
}
