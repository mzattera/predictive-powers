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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.knowledge.KnowledgeBase;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiEmbeddingService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiQuestionAnsweringService;
import io.github.mzattera.util.ResourceUtil;

public class QuestionAnsweringTest {
	
	@Test
	public void test00() {
	}

	/**
	 * Makes some tests by reading a knowledge base and asking a relevant question
	 * using the question answering service.
	 * 
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	@Test
	public void test01() throws ClassNotFoundException, IOException {
		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {
			OpenAiEmbeddingService es = ep.getEmbeddingService();
			OpenAiQuestionAnsweringService qas = ep.getQuestionAnsweringService();
			KnowledgeBase kb = KnowledgeBase.load(ResourceUtil.getResourceFile("kb_banana.object"));

			String question = "What does Olaf like?";
			List<Pair<EmbeddedText, Double>> context = kb.search(es.embed(question).get(0), 50, 0);
			QnAPair answer = qas.answerWithEmbeddings(question, context);

			assertTrue(answer.getAnswer().toLowerCase().contains("banana"));
			assertEquals(context.size(), answer.getEmbeddingContext().size());
			for (int i = 0; i < context.size(); ++i)
				assertEquals(context.get(i).getLeft().getText(), answer.getEmbeddingContext().get(i).getText());
		} // Close endpoint
	}

	/**
	 * Simple completion.
	 */
	@Test
	public void test02() {
		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {
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
	@Test
	public void test03() throws ClassNotFoundException, IOException {
		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {
			OpenAiEmbeddingService es = ep.getEmbeddingService();
			OpenAiQuestionAnsweringService qas = ep.getQuestionAnsweringService();
			KnowledgeBase kb = KnowledgeBase.load(ResourceUtil.getResourceFile("kb_banana.object"));

			qas.setMaxContextTokens(1);

			String question = "What does Olaf like?";
			List<Pair<EmbeddedText, Double>> context = kb.search(es.embed(question).get(0), 50, 0);
			QnAPair answer = qas.answerWithEmbeddings(question, context);

			assertEquals("I do not know.", answer.getAnswer());
			assertEquals("No context was provided.", answer.getExplanation());
		} // Close endpoint
	}

	/**
	 * String as context.
	 */
	@Test
	public void test0() {
		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {
			OpenAiQuestionAnsweringService qas = ep.getQuestionAnsweringService();

			String question = "What does Olaf like?";
			QnAPair answer = qas.answer(question, "Olaf likes pears.");

			assertEquals("Olaf likes pears.", answer.getAnswer());
			assertEquals(1, answer.getContext().size());
			assertEquals("Olaf likes pears.", answer.getContext().get(0));
		} // Close endpoint
	}

	/**
	 * This can be used to build a KnowledgeBase that can later be used in testing.
	 * 
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void main(String args[]) throws FileNotFoundException, IOException {

		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {
			OpenAiEmbeddingService es = ep.getEmbeddingService();
			KnowledgeBase kb = new KnowledgeBase();

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
