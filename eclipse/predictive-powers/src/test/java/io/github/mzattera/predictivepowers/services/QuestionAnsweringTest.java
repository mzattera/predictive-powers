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
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.AiEndpoint;
import io.github.mzattera.predictivepowers.huggingface.client.nlp.QuestionAnsweringRequest;
import io.github.mzattera.predictivepowers.huggingface.endpoint.HuggingFaceEndpoint;
import io.github.mzattera.predictivepowers.huggingface.services.HuggingFaceQuestionAnsweringService;
import io.github.mzattera.predictivepowers.knowledge.KnowledgeBase;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiQuestionAnsweringService;
import io.github.mzattera.util.ResourceUtil;

public class QuestionAnsweringTest {

	// TODO URGENT Make it work for both providers and break it into smaller test
	// TODO If possible, for OpenAI, add a test to check if context was built and
	// trimmed properly

	@Test
	public void test() throws ClassNotFoundException, IOException {
		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {
			test(ep);
		}

		// TODO URGENT re-enable
//		try (HuggingFaceEndpoint ep = new HuggingFaceEndpoint()) {
//			test(ep);
//		}
	}

	private void test(AiEndpoint e) throws ClassNotFoundException, IOException {
		test01(e);
		test03(e);
		test04(e);
		test05(e);
		test06(e);
	}

	/**
	 * Makes some tests by reading a knowledge base and asking a relevant question
	 * using the question answering service.
	 * 
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public void test01(AiEndpoint ep) throws ClassNotFoundException, IOException {
		try (OpenAiEndpoint oai = new OpenAiEndpoint()) {
			EmbeddingService es = oai.getEmbeddingService();
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
	}

	/**
	 * Same as test01(), but tests HF embedding with specific configuration
	 * 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@Test
	public void test91() throws ClassNotFoundException, IOException {
		try (HuggingFaceEndpoint ep = new HuggingFaceEndpoint()) {
			try (OpenAiEndpoint oai = new OpenAiEndpoint()) {
				EmbeddingService es = oai.getEmbeddingService();
				HuggingFaceQuestionAnsweringService qas = ep.getQuestionAnsweringService();
				KnowledgeBase kb = KnowledgeBase.load(ResourceUtil.getResourceFile("kb_banana.object"));

				String question = "What does Olaf like?";
				List<Pair<EmbeddedText, Double>> context = kb.search(es.embed(question).get(0), 50, 0);
				QnAPair answer = qas.answerWithEmbeddings(question, context, new QuestionAnsweringRequest());

				assertTrue(answer.getAnswer().toLowerCase().contains("banana"));
				assertEquals(context.size(), answer.getEmbeddingContext().size());
				for (int i = 0; i < context.size(); ++i)
					assertEquals(context.get(i).getLeft().getText(), answer.getEmbeddingContext().get(i).getText());
			}
		}
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
	public void test03(AiEndpoint ep) throws ClassNotFoundException, IOException {
		try (OpenAiEndpoint oai = new OpenAiEndpoint()) {
			EmbeddingService es = oai.getEmbeddingService();
			QuestionAnsweringService qas = ep.getQuestionAnsweringService();
			KnowledgeBase kb = KnowledgeBase.load(ResourceUtil.getResourceFile("kb_banana.object"));

			qas.setMaxContextTokens(3); // 3 is size of "Olaf "

			String question = "What does Olaf like?";
			List<Pair<EmbeddedText, Double>> context = kb.search(es.embed(question).get(0), 50, 0);
			QnAPair answer = qas.answerWithEmbeddings(question, context);

			assertTrue("I do not know.".equals(answer.getAnswer()) || "Olaf".equals(answer.getAnswer()));
		}
	}

	/**
	 * Same as test01(), but tests HF embedding with specific configuration
	 * 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@Test
	public void test93() throws ClassNotFoundException, IOException {
		try (HuggingFaceEndpoint ep = new HuggingFaceEndpoint()) {
			try (OpenAiEndpoint oai = new OpenAiEndpoint()) {
				EmbeddingService es = oai.getEmbeddingService();
				HuggingFaceQuestionAnsweringService qas = ep.getQuestionAnsweringService();
				KnowledgeBase kb = KnowledgeBase.load(ResourceUtil.getResourceFile("kb_banana.object"));

				qas.setMaxContextTokens(5); // 4 is the size of "Olaf"

				String question = "What does Olaf like?";
				List<Pair<EmbeddedText, Double>> context = kb.search(es.embed(question).get(0), 50, 0);
				QnAPair answer = qas.answerWithEmbeddings(question, context);

				assertEquals(0, answer.getContext().size());
				assertTrue("I do not know.".equals(answer.getAnswer()) || "Olaf".equals(answer.getAnswer()));
			}
		}
	}

	/**
	 * String as context.
	 */
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
	public void test06(AiEndpoint ep) {
		QuestionAnsweringService qas = ep.getQuestionAnsweringService();

		String question = "What does Olaf like?";
		QnAPair answer = qas.answer(question, new ArrayList<>());

		assertEquals("I do not know.", answer.getAnswer());
		assertEquals("No context was provided.", answer.getExplanation());
	}

	/**
	 * Getters and setters
	 */
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

		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {
			EmbeddingService es = ep.getEmbeddingService();
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
