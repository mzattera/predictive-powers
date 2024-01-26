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
package io.github.mzattera.predictivepowers.openai.client.completions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiCompletionService;
import io.github.mzattera.predictivepowers.services.TextCompletion;

class CompletionsTest {

	private static final String MODEL = "davinci-002";

	@Test
	void test01() {
		try (OpenAiEndpoint oai = new OpenAiEndpoint()) {
			String model = MODEL;
			String prompt = "How high is Mt. Everest (in meters)?";
			CompletionsRequest cr = new CompletionsRequest();

			cr.setModel(model);
			cr.setPrompt(prompt);
			cr.setMaxTokens(oai.getModelService().getContextSize(model) - 10);
			cr.setStop(new ArrayList<>());
			cr.getStop().add("feet");

			CompletionsResponse resp = oai.getClient().createCompletion(cr);

			assertEquals(1, resp.getChoices().size());
			assertEquals("stop", resp.getChoices().get(0).getFinishReason());
		} // Close endpoint
	}

	@Test
	void test02() {
		try (OpenAiEndpoint oai = new OpenAiEndpoint()) {
			String model = MODEL;
			String prompt = "How high is Mt. Everest (in meters)?";
			CompletionsRequest cr = new CompletionsRequest();

			cr.setModel(model);
			cr.setPrompt(prompt);
			cr.setMaxTokens(oai.getModelService().getContextSize(model) - 10);
			cr.setN(3);

			CompletionsResponse resp = oai.getClient().createCompletion(cr);

			assertEquals(resp.getChoices().size(), 3);
			assertEquals(resp.getChoices().get(0).getFinishReason(), "stop");

			for (CompletionsChoice c : resp.getChoices()) {
				assertTrue(c.getText().contains("848"));
			}
		} // Close endpoint
	}

	@Test
	void test03() {
		try (OpenAiEndpoint oai = new OpenAiEndpoint()) {
			String model = MODEL;
			String prompt = "How high is Mt. Everest (in meters)?";
			CompletionsRequest cr = new CompletionsRequest();

			cr.setModel(model);
			cr.setPrompt(prompt);
			cr.setMaxTokens(oai.getModelService().getContextSize(model) - 10);
			cr.setLogprobs(2);

			CompletionsResponse resp = oai.getClient().createCompletion(cr);

			assertEquals(1, resp.getChoices().size());
			assertEquals("stop", resp.getChoices().get(0).getFinishReason());
		} // Close endpoint
	}

	@Test
	void test04() {
		try (OpenAiEndpoint oai = new OpenAiEndpoint()) {
			OpenAiCompletionService cs = oai.getCompletionService();

			cs.getDefaultReq().setEcho(true);
			cs.getDefaultReq().setTemperature(0.0);
			String prompt = "Mt. Everest is";
			TextCompletion resp = cs.complete(prompt);

			assertTrue(resp.getText().startsWith(prompt));
		} // Close endpoint
	}

	@Test
	void test05() {
		try (OpenAiEndpoint oai = new OpenAiEndpoint()) {
			OpenAiCompletionService cs = oai.getCompletionService();

			cs.getDefaultReq().setEcho(false);
			cs.getDefaultReq().setTemperature(0.0);
			String prompt = "Mt. Everest is";
			String suffix = "meters high.";
	        assertThrows(
	                UnsupportedOperationException.class, 
	                () -> {
	                	cs.insert(prompt, suffix);
	                }
	            );
		} // Close endpoint
	}

	@Test
	void test06() {
		try (OpenAiEndpoint oai = new OpenAiEndpoint()) {
			String model = MODEL;
			String prompt = "How high is Mt. Everest (in meters)?";
			CompletionsRequest cr = new CompletionsRequest();

			cr.setModel(model);
			cr.setPrompt(prompt);
			cr.setMaxTokens(oai.getModelService().getContextSize(model) - 10);
			cr.setTopP(0.8);
			CompletionsResponse resp = oai.getClient().createCompletion(cr);

			assertEquals(1, resp.getChoices().size());
			assertEquals("stop", resp.getChoices().get(0).getFinishReason());
		} // Close endpoint
	}
}
