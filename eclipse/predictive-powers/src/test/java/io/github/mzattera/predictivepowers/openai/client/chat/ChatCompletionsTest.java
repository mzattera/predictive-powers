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
package io.github.mzattera.predictivepowers.openai.client.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest.ResponseFormat;
import io.github.mzattera.predictivepowers.openai.client.chat.LogProbs.ContentToken;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatMessage;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatMessage.Role;

class ChatCompletionsTest {

	@Test
	void test01() {
		try (OpenAiEndpoint endpoint = new OpenAiEndpoint()) {
			String model = "gpt-3.5-turbo";
			String prompt = "How high is Mt. Everest?";
			ChatCompletionsRequest cr = new ChatCompletionsRequest();

			cr.setModel(model);
			cr.getMessages().add(new OpenAiChatMessage(Role.USER, prompt));
			cr.setMaxTokens(endpoint.getModelService().getContextSize(model) - 15);
			cr.setStop(new ArrayList<>());
			cr.getStop().add("feet");

			ChatCompletionsResponse resp = endpoint.getClient().createChatCompletion(cr);

			assertEquals(resp.getChoices().size(), 1);
			assertEquals(resp.getChoices().get(0).getFinishReason(), "stop");
			assertTrue(resp.getChoices().get(0).getMessage().getContent().contains("848")
					|| resp.getChoices().get(0).getMessage().getContent().contains("029 "));
			assertTrue(resp.getChoices().get(0).getMessage().getContent().endsWith("029 ")
					|| resp.getChoices().get(0).getMessage().getContent().endsWith("31.7 "));
		} // Close endpoint
	}

	@Test
	void test02() {
		try (OpenAiEndpoint endpoint = new OpenAiEndpoint()) {
			String model = "gpt-3.5-turbo";
			String prompt = "How high is Mt. Everest?";
			ChatCompletionsRequest cr = new ChatCompletionsRequest();

			cr.setModel(model);
			cr.getMessages().add(new OpenAiChatMessage(Role.USER, prompt));
			cr.setMaxTokens(endpoint.getModelService().getContextSize(model) - 15);
			cr.setTopP(0.8);

			ChatCompletionsResponse resp = endpoint.getClient().createChatCompletion(cr);

			assertEquals(resp.getChoices().size(), 1);
			assertEquals(resp.getChoices().get(0).getFinishReason(), "stop");
			assertTrue(resp.getChoices().get(0).getMessage().getContent().contains("848")
					|| resp.getChoices().get(0).getMessage().getContent().contains("029 "));
		} // Close endpoint
	}

	/**
	 * Test returning logprobs.
	 */
	@Test
	void testLogProbs() {
		try (OpenAiEndpoint endpoint = new OpenAiEndpoint();) {
			ChatCompletionsRequest cr = new ChatCompletionsRequest();

			cr.setModel("gpt-4");
			cr.getMessages().add(new OpenAiChatMessage(Role.USER, "Hi!"));
			cr.setLogprobs(true);
			cr.setTopLogprobs(3);

			ChatCompletionsResponse resp = endpoint.getClient().createChatCompletion(cr);
			assertEquals(resp.getChoices().size(), 1);
			assertEquals(resp.getChoices().get(0).getFinishReason(), "stop");

			assertTrue(resp.getChoices().get(0).getLogprobs().getContent().size() > 0);
			for (ContentToken lp : resp.getChoices().get(0).getLogprobs().getContent()) {
				assertTrue(lp.getTopLogprobs().size() > 0);
				assertTrue(lp.getTopLogprobs().size() <= 3);
			}
		}
	}

	/**
	 * Testing seed and fingerprint.
	 */
	@Test
	void testReproduceable() {
		try (OpenAiEndpoint endpoint = new OpenAiEndpoint();) {
			ChatCompletionsRequest cr = new ChatCompletionsRequest();

			cr.setModel("gpt-4-turbo-preview");
			cr.getMessages().add(new OpenAiChatMessage(Role.USER, "Hi!"));
			cr.setSeed(666);
			assertEquals(666, cr.getSeed());

			ChatCompletionsResponse resp = endpoint.getClient().createChatCompletion(cr);
			assertEquals(resp.getChoices().size(), 1);
			assertEquals(resp.getChoices().get(0).getFinishReason(), "stop");

			// It is null indeed sometimes
			assertNotNull(resp.getSystemFingerprint());
			assertTrue(resp.getSystemFingerprint().length() > 0);
		}
	}

	/**
	 * Testing response format.
	 */
	@Test
	void testResponseFormat() {
		try (OpenAiEndpoint endpoint = new OpenAiEndpoint();) {
			ChatCompletionsRequest cr = new ChatCompletionsRequest();

			cr.setModel("gpt-4-turbo-preview");
			cr.getMessages().add(new OpenAiChatMessage(Role.USER, "Create fake person record in JSON fofmat containing a name and a birth date."));
			cr.setResponseFormat(ResponseFormat.JSON);
			assertEquals(ResponseFormat.JSON, cr.getResponseFormat());

			// TODO We could check here true JSON is returned
			
			ChatCompletionsResponse resp = endpoint.getClient().createChatCompletion(cr);
			assertEquals(resp.getChoices().size(), 1);
			assertEquals(resp.getChoices().get(0).getFinishReason(), "stop");
			System.out.println(resp.getChoices().get(0).getMessage().getContent());
			
			cr.getMessages().add(new OpenAiChatMessage(Role.USER, "Create fake person record in JSON fofmat containing a name and a birth date."));
			cr.setResponseFormat(ResponseFormat.TEXT);
			assertEquals(ResponseFormat.TEXT, cr.getResponseFormat());
			System.out.println(resp.getChoices().get(0).getMessage().getContent());

			resp = endpoint.getClient().createChatCompletion(cr);
			assertEquals(resp.getChoices().size(), 1);
			assertEquals(resp.getChoices().get(0).getFinishReason(), "stop");
		}
	}
}
