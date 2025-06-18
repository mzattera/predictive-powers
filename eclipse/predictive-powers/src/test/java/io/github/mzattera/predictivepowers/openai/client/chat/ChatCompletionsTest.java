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
package io.github.mzattera.predictivepowers.openai.client.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.mzattera.predictivepowers.TestConfiguration;
import io.github.mzattera.predictivepowers.openai.client.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.chat.LogProbs.ContentToken;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatMessage;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatMessage.Role;

class ChatCompletionsTest {

	private static List<ImmutablePair<OpenAiEndpoint, String>> svcs;

	@BeforeAll
	static void init() {
		svcs = TestConfiguration.getChatServices().stream() //
				.filter(p -> p.getLeft() instanceof OpenAiEndpoint) //
				.map(p -> new ImmutablePair<OpenAiEndpoint, String>((OpenAiEndpoint) p.getLeft(), p.getRight())) //
				.collect(Collectors.toList());
	}

	@AfterAll
	static void tearDown() {
		TestConfiguration.close(svcs.stream().map(p -> p.getLeft()).collect(Collectors.toList()));
	}

	static Stream<ImmutablePair<OpenAiEndpoint, String>> services() {
		return svcs.stream();
	}

	@DisplayName("Tests stop token")
	@ParameterizedTest
	@MethodSource("services")
	void test01(Pair<OpenAiEndpoint, String> p) {
		OpenAiEndpoint endpoint = p.getLeft();
		String model = p.getRight();
		String prompt = "How high is Mt. Everest (in feet)?";
		ChatCompletionsRequest cr = new ChatCompletionsRequest();

		cr.setModel(model);
		cr.getMessages().add(new OpenAiChatMessage(Role.USER, prompt));
		assertTrue(endpoint.getModelService().getContextSize(model, -1) > 0);
		cr.setMaxCompletionTokens(30);
		cr.setStop(new ArrayList<>());
		cr.getStop().add("feet");

		ChatCompletionsResponse resp = endpoint.getClient().createChatCompletion(cr);

		assertEquals(1, resp.getChoices().size(), 1);
		assertEquals("stop", resp.getChoices().get(0).getFinishReason());
	}

	@DisplayName("Test setting TopP")
	@ParameterizedTest
	@MethodSource("services")
	void test02(Pair<OpenAiEndpoint, String> p) {
		OpenAiEndpoint endpoint = p.getLeft();
		String model = p.getRight();
		String prompt = "How high is Mt. Everest?";
		ChatCompletionsRequest cr = new ChatCompletionsRequest();

		cr.setModel(model);
		cr.getMessages().add(new OpenAiChatMessage(Role.USER, prompt));
		assertTrue(endpoint.getModelService().getContextSize(model, -1) > 0);
		cr.setMaxCompletionTokens(30);
		cr.setTopP(0.8);

		ChatCompletionsResponse resp = endpoint.getClient().createChatCompletion(cr);

		assertEquals(1, resp.getChoices().size());
		assertTrue("stop".equals(resp.getChoices().get(0).getFinishReason())
				|| "length".equals(resp.getChoices().get(0).getFinishReason()));
	}

	@DisplayName("Test returning logprobs")
	@Test
	void testLogProbs() {

		if (!TestConfiguration.TEST_OPENAI_SERVICES)
			return;

		try (OpenAiEndpoint endpoint = new OpenAiEndpoint();) {
			ChatCompletionsRequest cr = new ChatCompletionsRequest();

			cr.setModel("gpt-4");
			cr.getMessages().add(new OpenAiChatMessage(Role.USER, "Hi!"));
			cr.setLogprobs(true);
			cr.setTopLogprobs(3);

			ChatCompletionsResponse resp = endpoint.getClient().createChatCompletion(cr);
			assertEquals(resp.getChoices().size(), 1);
			assertEquals(resp.getChoices().get(0).getFinishReason(), "stop");

			assertTrue(resp.getChoices().get(0).getLogprobs().getTextContent().size() > 0);
			for (ContentToken lp : resp.getChoices().get(0).getLogprobs().getTextContent()) {
				assertTrue(lp.getTopLogprobs().size() > 0);
				assertTrue(lp.getTopLogprobs().size() <= 3);
			}
		}
	}

	@DisplayName("Testing seed and fingerprint")
	@ParameterizedTest
	@MethodSource("services")
	void testReproduceable(Pair<OpenAiEndpoint, String> p) {
		OpenAiEndpoint endpoint = p.getLeft();
		String model = p.getRight();

		ChatCompletionsRequest cr = new ChatCompletionsRequest();

		cr.setModel(model);
		cr.getMessages().add(new OpenAiChatMessage(Role.USER, "Hi!"));
		cr.setSeed(666);
		assertEquals(666, cr.getSeed());

		ChatCompletionsResponse resp = endpoint.getClient().createChatCompletion(cr);
		assertEquals(1, resp.getChoices().size());
		assertEquals("stop", resp.getChoices().get(0).getFinishReason());

		// It is null indeed sometimes
		if (resp.getSystemFingerprint() != null)
			assertTrue(resp.getSystemFingerprint().length() > 0);
	}

	@DisplayName("Testing response format")
	@Test
	void testResponseFormat() {

		if (!TestConfiguration.TEST_OPENAI_SERVICES)
			return;

		try (OpenAiEndpoint endpoint = new OpenAiEndpoint();) {
			ChatCompletionsRequest cr = new ChatCompletionsRequest();

			cr.setModel("gpt-4-turbo-preview");
			cr.getMessages().add(new OpenAiChatMessage(Role.USER,
					"Create fake person record in JSON format containing a name and a birth date."));
			cr.setResponseFormat(ResponseFormat.JSON_OBJECT);
			assertEquals(ResponseFormat.JSON_OBJECT, cr.getResponseFormat());

			// TODO We could check here true JSON is returned

			ChatCompletionsResponse resp = endpoint.getClient().createChatCompletion(cr);
			assertEquals(resp.getChoices().size(), 1);
			assertEquals(resp.getChoices().get(0).getFinishReason(), "stop");
			System.out.println(resp.getChoices().get(0).getMessage().getTextContent());

			cr.getMessages().add(new OpenAiChatMessage(Role.USER,
					"Create fake person record in JSON format containing a name and a birth date."));
			cr.setResponseFormat(ResponseFormat.TEXT);
			assertEquals(ResponseFormat.TEXT, cr.getResponseFormat());
			System.out.println(resp.getChoices().get(0).getMessage().getTextContent());

			resp = endpoint.getClient().createChatCompletion(cr);
			assertEquals(resp.getChoices().size(), 1);
			assertEquals(resp.getChoices().get(0).getFinishReason(), "stop");
		}
	}
}
