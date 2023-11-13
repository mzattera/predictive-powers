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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.openai.client.models.Model;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;

class OpenAiModelServiceTest {

	private final static Set<String> OLD_MODELS = new HashSet<>();
	static {
		OLD_MODELS.add("ada-code-search-code");
		OLD_MODELS.add("ada-code-search-text");
		OLD_MODELS.add("ada-search-document");
		OLD_MODELS.add("ada-search-query");
		OLD_MODELS.add("ada-similarity");
		OLD_MODELS.add("babbage-code-search-code");
		OLD_MODELS.add("babbage-code-search-text");
		OLD_MODELS.add("babbage-search-document");
		OLD_MODELS.add("babbage-search-query");
		OLD_MODELS.add("babbage-similarity");
		;
		OLD_MODELS.add("curie-instruct-beta");
		OLD_MODELS.add("curie-search-document");
		OLD_MODELS.add("curie-search-query");
		OLD_MODELS.add("curie-similarity");
		OLD_MODELS.add("davinci-instruct-beta");
		OLD_MODELS.add("davinci-search-document");
		OLD_MODELS.add("davinci-search-query");
		OLD_MODELS.add("davinci-similarity");
		;
		OLD_MODELS.add("whisper-1"); // OK, this is a trick
		OLD_MODELS.add("canary-whisper"); // OK, this is a trick
		OLD_MODELS.add("canary-tts"); // OK, this is a trick
	}

	@Test
	void test01() {
		try (OpenAiEndpoint oai = new OpenAiEndpoint()) {
			Set<String> deprecated = new HashSet<>(OLD_MODELS);
			Set<String> actual = new HashSet<>(OpenAiModelService.CONTEXT_SIZES.keySet());

			List<Model> models = oai.getClient().listModels();
			assertTrue(models.size() > 0);

			for (Model m : models) {
				if (m.getId().startsWith("dall-e"))
					continue; // DALL-E models do not need size
				if (m.getId().contains("-edit"))
					continue; // Edits model do not need size
				if (m.getId().contains("ft-"))
					continue; // fine-tunes can be ignored
				if (deprecated.remove(m.getId()))
					continue; // Skip old models

				if (!m.getId().startsWith("tts-")) // Text to speech models do not have encoders
					assertTrue(oai.getModelService().getTokenizer(m.getId()) != null);
				assertTrue(oai.getModelService().getContextSize(m.getId()) > 0);

				assertTrue(actual.remove(m.getId()));
			}

			// Check OLD_MODELS does not contain things we do not need any longer.
			for (String m : deprecated) {
				System.out.println("OLD model no longer there: " + m);
			}
			assertEquals(0, deprecated.size());

			// Check CONTEXT_SIZES does not contain things we do not need any longer.
			int skip = 0;
			for (String m : actual) {
				if (m.startsWith("gpt-4-32k")) {
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
