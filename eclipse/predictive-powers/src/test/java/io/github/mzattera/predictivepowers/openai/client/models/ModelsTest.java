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
package io.github.mzattera.predictivepowers.openai.client.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;

class ModelsTest {

	private final static Set<String> oldModels = new HashSet<>();
	static {
		oldModels.add("babbage-code-search-code");
		oldModels.add("text-davinci-001");
		oldModels.add("babbage-code-search-text");
		oldModels.add("babbage-similarity");
		oldModels.add("whisper-1");
		oldModels.add("curie-instruct-beta");
		oldModels.add("ada-code-search-code");
		oldModels.add("ada-similarity");
		oldModels.add("davinci-search-document");
		oldModels.add("ada-code-search-text");
		oldModels.add("davinci-instruct-beta");
		oldModels.add("ada-search-query");
		oldModels.add("curie-search-query");
		oldModels.add("davinci-search-query");
		oldModels.add("babbage-search-document");
		oldModels.add("ada-search-document");
		oldModels.add("curie-search-document");
		oldModels.add("babbage-search-query");
		oldModels.add("curie-similarity");
		oldModels.add("davinci-similarity");
		oldModels.add("cushman:2020-05-03");
		oldModels.add("ada:2020-05-03");
		oldModels.add("babbage-search-query");
		oldModels.add("babbage:2020-05-03");
		oldModels.add("curie:2020-05-03");
		oldModels.add("davinci:2020-05-03");
		oldModels.add("if-davinci-v2");
		oldModels.add("if-curie-v2");
		oldModels.add("if-davinci:3.0.0");
		oldModels.add("davinci-if:3.0.0");
		oldModels.add("davinci-instruct-beta:2.0.0");
		oldModels.add("text-ada:001");
		oldModels.add("text-davinci:001");
		oldModels.add("text-curie:001");
		oldModels.add("text-babbage:001");
	}

	@Test
	void test01() {
		try (OpenAiEndpoint oai = new OpenAiEndpoint()) {
			List<Model> models = oai.getClient().listModels();
			assertTrue(models.size() > 0);

			for (Model m : models) {
				if (m.getId().contains("-edit"))
					continue; // Edits model do not need size
				if (m.getId().contains("ft-personal"))
					continue; // fine-tunes can be ignored
				if (oldModels.contains(m.getId()))
					continue; // Skip old models

				assertTrue(oai.getModelService().getContextSize(m.getId()) > 0);
				
				// TODO test tokenizers are available here
			}
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
