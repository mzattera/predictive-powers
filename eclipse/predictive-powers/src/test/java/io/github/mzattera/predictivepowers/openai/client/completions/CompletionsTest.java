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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.mzattera.predictivepowers.TestConfiguration;
import io.github.mzattera.predictivepowers.openai.client.OpenAiEndpoint;

/**
 * Tests OpenAI Completions API.
 */
class CompletionsTest {

	private static List<ImmutablePair<OpenAiEndpoint, String>> svcs;

	@BeforeAll
	static void init() {
		svcs = TestConfiguration.getCompletionServices().stream() //
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

	@ParameterizedTest
	@MethodSource("services")
	void test01(Pair<OpenAiEndpoint, String> p) {
		String prompt = "How high is Mt. Everest (in meters)?";
		String model = p.getRight();
		CompletionsRequest cr = new CompletionsRequest();

		cr.setModel(model);
		cr.setPrompt(prompt);
		assertTrue(p.getLeft().getModelService().getContextSize(model, -1) > 0);
		cr.setMaxTokens(50);
		cr.setStop(new ArrayList<>());
		cr.getStop().add("feet");

		CompletionsResponse resp = p.getLeft().getClient().createCompletion(cr);

		assertEquals(1, resp.getChoices().size());
		String stop = resp.getChoices().get(0).getFinishReason();
		assertTrue("stop".equals(stop) || "length".equals(stop));
	}

	@ParameterizedTest
	@MethodSource("services")
	void test02(Pair<OpenAiEndpoint, String> p) {
		String prompt = "How high is Mt. Everest (in meters)?";
		String model = p.getRight();
		CompletionsRequest cr = new CompletionsRequest();

		cr.setModel(model);
		cr.setPrompt(prompt);
		assertTrue(p.getLeft().getModelService().getContextSize(model, -1) > 0);
		cr.setMaxTokens(50);
		cr.setN(3);

		CompletionsResponse resp = p.getLeft().getClient().createCompletion(cr);

		assertEquals(3, resp.getChoices().size());
		String stop = resp.getChoices().get(0).getFinishReason();
		assertTrue("stop".equals(stop) || "length".equals(stop));
	}

	@ParameterizedTest
	@MethodSource("services")
	void test03(Pair<OpenAiEndpoint, String> p) {
		String prompt = "How high is Mt. Everest (in meters)?";
		String model = p.getRight();
		CompletionsRequest cr = new CompletionsRequest();

		cr.setModel(model);
		cr.setPrompt(prompt);
		assertTrue(p.getLeft().getModelService().getContextSize(model, -1) > 0);
		cr.setMaxTokens(50);
		cr.setLogprobs(2);
		cr.setLogitBias(null);

		CompletionsResponse resp = p.getLeft().getClient().createCompletion(cr);

		assertEquals(1, resp.getChoices().size());
		String stop = resp.getChoices().get(0).getFinishReason();
		assertTrue("stop".equals(stop) || "length".equals(stop));
	}

	@ParameterizedTest
	@MethodSource("services")
	void test04(Pair<OpenAiEndpoint, String> p) {
		String prompt = "How high is Mt. Everest (in meters)?";
		String model = p.getRight();
		CompletionsRequest cr = new CompletionsRequest();

		cr.setModel(model);
		cr.setPrompt(prompt);
		assertTrue(p.getLeft().getModelService().getContextSize(model, -1) > 0);
		cr.setMaxTokens(50);
		cr.setEcho(true);

		CompletionsResponse resp = p.getLeft().getClient().createCompletion(cr);

		assertEquals(1, resp.getChoices().size());
		String stop = resp.getChoices().get(0).getFinishReason();
		assertTrue("stop".equals(stop) || "length".equals(stop));
		assertTrue(resp.getChoices().get(0).getText().startsWith(prompt));
	}

	@ParameterizedTest
	@MethodSource("services")
	void test06(Pair<OpenAiEndpoint, String> p) {
		String prompt = "How high is Mt. Everest (in meters)?";
		String model = p.getRight();
		CompletionsRequest cr = new CompletionsRequest();

		cr.setModel(model);
		cr.setPrompt(prompt);
		assertTrue(p.getLeft().getModelService().getContextSize(model, -1) > 0);
		cr.setMaxTokens(50);
		cr.setTopP(0.8);
		CompletionsResponse resp = p.getLeft().getClient().createCompletion(cr);

		assertEquals(1, resp.getChoices().size());
		String stop = resp.getChoices().get(0).getFinishReason();
		assertTrue("stop".equals(stop) || "length".equals(stop));
	}
}
