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
package io.github.mzattera.predictivepowers.openai.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.openai.client.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.QnAPair;

public class OpenAiQuestionExtractionTest {

	// TODO Add a test to check text is split and trimmed correctly....if possible...
	
	
	@Test
	public void test01() throws Exception {

		// OpenAI end-point
		// Make sure you specify your API key n OPENAI_KEY system environment variable.
		try (OpenAiEndpoint endpoint = new OpenAiEndpoint()) {

			OpenAiQuestionExtractionService q = endpoint.getQuestionExtractionService();
			String context = "Donatien Alphonse Franquois, Marquis de Sade, was a French nobleman, revolutionary politician, philosopher and writer famous for his literary depictions of a libertine sexuality as well as numerous accusations of sex crimes.";

			// Get some FAQs and print them
			List<QnAPair> QnA = q.getQuestions(context);
			assertTrue(QnA.size() > 0);
			for (int i = 0; (i < 3) & (i < QnA.size()); ++i) {
				System.out.println(QnA.get(i).toString());
			}
			System.out.println();

			// Demo fill-in questions
			QnA = q.getFillQuestions(context);
			assertTrue(QnA.size() > 0);
			for (int i = 0; (i < 3) & (i < QnA.size()); ++i) {
				System.out.println(QnA.get(i).toString());
			}
			System.out.println();

			// Demo true/false questions
			QnA = q.getTFQuestions(context);
			assertTrue(QnA.size() > 0);
			for (int i = 0; (i < 3) & (i < QnA.size()); ++i) {
				System.out.println(QnA.get(i).toString());
			}
			System.out.println();

			// Demo multiple choice questions
			QnA = q.getMCQuestions(context);
			assertTrue(QnA.size() > 0);
			for (int i = 0; (i < 3) & (i < QnA.size()); ++i) {
				System.out.println(QnA.get(i).toString());
			}
			System.out.println();
		} // Close endpoint
	}

	/**
	 * Getters and setters
	 */
	@Test
	public void test02() {

		// OpenAI end-point
		// Make sure you specify your API key n OPENAI_KEY system environment variable.
		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {
			OpenAiQuestionExtractionService s = ep.getQuestionExtractionService();

			String m = s.getModel();
			assertNotNull(m);
			s.setModel("pippo");
			assertEquals("pippo", s.getModel());
			s.setModel(m);
		}
	}
}