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
package io.github.mzattera.predictivepowers.openai.client.edits;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;

class EditsTest {

	@Test
	void test01() {
		try (OpenAiEndpoint oai = new OpenAiEndpoint()) {
			String model = "text-davinci-edit-001";
			String prompt = "Put all text in uppercase.";
			String input = "How high is Mt. Everest?";
			EditsRequest req = new EditsRequest();

			req.setModel(model);
			req.setInstruction(prompt);
			req.setInput(input);
			req.setTemperature(0.0);

			EditsResponse resp = oai.getClient().createEdit(req);
			assertEquals(resp.getChoices().size(), 1);
			assertEquals(resp.getChoices().get(0).getText().trim(), "HOW HIGH IS MT. EVEREST?");
		} // Close endpoint
	}
}
