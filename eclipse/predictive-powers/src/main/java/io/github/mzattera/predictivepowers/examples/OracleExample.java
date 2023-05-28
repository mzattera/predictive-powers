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

package io.github.mzattera.predictivepowers.examples;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.knowledge.KnowledgeBase;
import io.github.mzattera.predictivepowers.services.EmbeddedText;
import io.github.mzattera.predictivepowers.services.EmbeddingService;
import io.github.mzattera.predictivepowers.services.QnAPair;
import io.github.mzattera.predictivepowers.services.QuestionAnsweringService;

public class OracleExample {

	public static void main(String[] args) throws MalformedURLException, IOException, SAXException, TikaException {

		// OpenAI end-point
		// Make sure you specify your API key in OPENAI_KEY system environment variable.
		try (OpenAiEndpoint endpoint = new OpenAiEndpoint()) {
			QuestionAnsweringService questionAnswer = endpoint.getQuestionAnsweringService();

			try (Scanner console = new Scanner(System.in)) {

				// Get the web page you are interested in
				System.out.print("Web Page Url: ");
				String pageUrl = console.nextLine();
				System.out.println("Reading page " + pageUrl + "...\n");

				// Read the page text, embed it and store it into an in-memory knowledge base
				EmbeddingService embeddingService = endpoint.getEmbeddingService();
				KnowledgeBase knowledgeBase = new KnowledgeBase();
				knowledgeBase.insert(embeddingService.embedURL(pageUrl));

				// Loop to reads questions from user and answer them
				QnAPair answer = null;
				while (true) {

					// Get user question
					System.out.print("Your Question: ");
					String question = console.nextLine();

					// Does user want an explanation?
					if (question.toLowerCase().equals("explain")) {
						if (answer == null)
							continue;
						System.out.println();
						System.out.println(answer.getExplanation());
						System.out.println();
						continue;
					}

					// If not, answer the question
					// Create context by finding similar text in the web page
					List<Pair<EmbeddedText, Double>> context = knowledgeBase
							.search(embeddingService.embed(question).get(0), 50, 0);

					// Use the context when answering
					answer = questionAnswer.answerWithEmbeddings(question, context);

					System.out.println("My Answer: " + answer.getAnswer() + "\n");
				}
			}
		}
	} // closes endpoint
}
