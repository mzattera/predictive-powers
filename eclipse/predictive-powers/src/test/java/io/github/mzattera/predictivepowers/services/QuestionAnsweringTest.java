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

package io.github.mzattera.predictivepowers.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.mzattera.predictivepowers.AiEndpoint;
import io.github.mzattera.predictivepowers.huggingface.client.HuggingFaceEndpoint;
import io.github.mzattera.predictivepowers.knowledge.KnowledgeBase;
import io.github.mzattera.predictivepowers.openai.client.DirectOpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiQuestionAnsweringService;
import io.github.mzattera.util.ResourceUtil;

public class QuestionAnsweringTest {

	// TODO If possible, for OpenAI, add a test to check if context was built and
	// trimmed properly

	private static AiEndpoint[] a;

	@BeforeAll
	static void init() {
		a = new AiEndpoint[] { new DirectOpenAiEndpoint(), new HuggingFaceEndpoint() };
	}

	@AfterAll
	static void tearDown() {
		for (AiEndpoint ep : a)
			try {
				ep.close();
			} catch (Exception e) {
			}
	}

	/** @return List of endpoints which QA services must be tested. */
	static Stream<AiEndpoint> endpoints() {
		return Arrays.stream(a);
	}

	/**
	 * Makes some tests by reading a knowledge base and asking a relevant question
	 * using the question answering service.
	 * 
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	@ParameterizedTest
	@MethodSource("endpoints")
	public void test01(AiEndpoint ep) throws ClassNotFoundException, IOException {
		EmbeddingService es = ep.getEmbeddingService();
		QuestionAnsweringService qas = ep.getQuestionAnsweringService();
		KnowledgeBase kb = KnowledgeBase.load(ResourceUtil.getResourceFile("kb_banana.object"));

		String question = "What does Olaf like?";
		List<Pair<EmbeddedText, Double>> context = kb.search(es.embed(question).get(0), 50, 0);
		QnAPair answer = qas.answerWithEmbeddings(question, context);

		assertTrue(answer.getAnswer().toLowerCase().contains("banana"));
		assertEquals(context.size(), answer.getEmbeddingContext().size());
		for (int i = 0; i < context.size(); ++i)
			assertEquals(context.get(i).getLeft().getText(), answer.getEmbeddingContext().get(i).getText());
	}

	/**
	 * Simple completion.
	 */
	@Test
	public void test02() {
		try (DirectOpenAiEndpoint ep = new DirectOpenAiEndpoint()) {
			OpenAiQuestionAnsweringService qas = ep.getQuestionAnsweringService();

			qas.setMaxContextTokens(1);

			String question = "How high is Mt. Everest (in meters)?";
			QnAPair answer = qas.answer(question);

			assertTrue(answer.getAnswer().contains("848"));
		} // Close endpoint
	}

	/**
	 * Test empty context.
	 * 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@ParameterizedTest
	@MethodSource("endpoints")
	public void test03(AiEndpoint ep) throws ClassNotFoundException, IOException {
		EmbeddingService es = ep.getEmbeddingService();
		QuestionAnsweringService qas = ep.getQuestionAnsweringService();
		KnowledgeBase kb = KnowledgeBase.load(ResourceUtil.getResourceFile("kb_banana.object"));

		qas.setMaxContextTokens(3); // 3 is size of "Olaf "

		String question = "What does Olaf like?";
		List<Pair<EmbeddedText, Double>> context = kb.search(es.embed(question).get(0), 50, 0);
		QnAPair answer = qas.answerWithEmbeddings(question, context);

		assertEquals(0, answer.getContext().size());
		assertTrue("I do not know.".equals(answer.getAnswer()) || "Olaf".equals(answer.getAnswer()));
	}

	/**
	 * String as context.
	 */
	@ParameterizedTest
	@MethodSource("endpoints")
	public void test04(AiEndpoint ep) {
		QuestionAnsweringService qas = ep.getQuestionAnsweringService();

		String question = "What does Olaf like?";
		QnAPair answer = qas.answer(question, "Olaf likes pears.");

		assertTrue(answer.getAnswer().contains("pears"));
		assertEquals(1, answer.getContext().size());
		assertEquals("Olaf likes pears.", answer.getContext().get(0));
	}

	/**
	 * Empty context.
	 */
	@ParameterizedTest
	@MethodSource("endpoints")
	public void test06(AiEndpoint ep) {
		QuestionAnsweringService qas = ep.getQuestionAnsweringService();

		String question = "What does Olaf like?";
		QnAPair answer = qas.answer(question, new ArrayList<>());

		assertEquals(0, answer.getContext().size());
		assertEquals("I do not know.", answer.getAnswer());
		assertEquals("No context was provided.", answer.getExplanation());
	}

	/**
	 * Getters and setters
	 */
	@ParameterizedTest
	@MethodSource("endpoints")
	public void test05(AiEndpoint ep) {
		QuestionAnsweringService s = ep.getQuestionAnsweringService();

		String m = s.getModel();
		assertNotNull(m);
		s.setModel("pippo");
		assertEquals("pippo", s.getModel());
		s.setModel(m);
	}

	/**
	 * This can be used to build a KnowledgeBase that can later be used in testing.
	 * 
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void main(String args[]) throws FileNotFoundException, IOException {

		try (DirectOpenAiEndpoint ep = new DirectOpenAiEndpoint(); KnowledgeBase kb = new KnowledgeBase()) {
			EmbeddingService es = ep.getEmbeddingService();

			List<String> test = new ArrayList<>();
			test.add("Olaf likes bananas");
			test.add("the sum of parts is more than the parts of the sum");
			test.add("a tiger is runnin in the forest");
			test.add("there is no prime number smaller than 1");
			test.add("Jupiter is a planet");
			kb.insert(es.embed(test));

			kb.save("D:\\kb_banana.object");
		} // Close endpoint
	}
}
