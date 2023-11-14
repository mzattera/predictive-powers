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
package io.github.mzattera.predictivepowers.openai.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.QnAPair;

public class OpenAiQuestionExtractionTest {

	@Test
	void test01() throws Exception {

		// OpenAI end-point
		// Make sure you specify your API key n OPENAI_KEY system environment variable.
		try (OpenAiEndpoint endpoint = new OpenAiEndpoint()) {

			// Download Credit Suisse financial statement 2022 PDF and extract its text.
			// We keep only one piece of 750 tokens, as extracting questions from a long
			// text might result in a timeout
			String context = "Donatien Alphonse Franquois, Marquis de Sade, was a French nobleman, revolutionary politician, philosopher and writer famous for his literary depictions of a libertine sexuality as well as numerous accusations of sex crimes.";

			OpenAiQuestionExtractionService q = endpoint.getQuestionExtractionService();
			q.getCompletionService().getDefaultReq().setTemperature(0.0);

			// Get some FAQs and print them
			List<QnAPair> QnA = q.getQuestions(context);
			for (int i = 0; (i < 3) & (i < QnA.size()); ++i) {
				System.out.println(QnA.get(i).toString());
			}
			System.out.println();

			// Demo fill-in questions
			QnA = q.getFillQuestions(context);
			for (int i = 0; (i < 3) & (i < QnA.size()); ++i) {
				System.out.println(QnA.get(i).toString());
			}
			System.out.println();

			// Demo true/false questions
			QnA = q.getTFQuestions(context);
			for (int i = 0; (i < 3) & (i < QnA.size()); ++i) {
				System.out.println(QnA.get(i).toString());
			}
			System.out.println();

			// Demo multiple choice questions
			QnA = q.getMCQuestions(context);
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