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

/**
 * 
 */
package io.github.mzattera.predictivepowers.applications;

import java.io.File;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;

import org.apache.commons.lang3.tuple.Pair;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.knowledge.KnowledgeBase;
import io.github.mzattera.predictivepowers.services.EmbeddedText;
import io.github.mzattera.predictivepowers.services.EmbeddingService;
import io.github.mzattera.predictivepowers.services.QnAPair;
import io.github.mzattera.predictivepowers.services.QuestionAnsweringService;

/**
 * An Oracle ingests a knowledge base, in form of documents stored in a folder,
 * then answers questions on the ingested materials.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class Oracle {

	/** Folder with the knowledge base data. */
	private final static File KB_FOLDER = new File("D:\\KB\\TMP");

	/**
	 * File with a pre-saved knowledge base. If this file exists, the knowledge base
	 * is read form there instead of reading from KB_FOLDER
	 */
	private final static File SAVED_KB_FILE = new File("D:\\kb_banana.object");

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {

			// OpenAI end-point
			// Make sure you specify your API key n OPENAI_KEY system environment variable.
			OpenAiEndpoint ep = OpenAiEndpoint.getInstance();
			EmbeddingService es = ep.getEmbeddingService();
			QuestionAnsweringService qas = ep.getQuestionAnsweringService();

			KnowledgeBase kb = new KnowledgeBase();
			if (SAVED_KB_FILE.exists()) {
				System.out.println("Reading Knowledge Base: " + SAVED_KB_FILE.getCanonicalPath());

				// Reads saved KB
				kb = KnowledgeBase.load(SAVED_KB_FILE);
			} else {
				System.out.println("Knowledge Base Folder: " + KB_FOLDER.getCanonicalPath());
				
				// Creates a KB out of the folder
				for (Entry<File, List<EmbeddedText>> fileEmbeddings : es.embedFolder(KB_FOLDER).entrySet()) {
					for (EmbeddedText embedding : fileEmbeddings.getValue()) {
						embedding.set("file_name", fileEmbeddings.getKey().getName());
						kb.insert(embedding);
					}
				}
				
				// Save it
				kb.save(SAVED_KB_FILE);
			}

			try (Scanner console = new Scanner(System.in)) {
				while (true) {
					System.out.print("Your Question: ");
					String question = console.nextLine();

					// Fetch embeddings matching question and show them
					System.out.println("===[ Context ]===================================== ");
					List<Pair<EmbeddedText, Double>> context = kb.search(es.embed(question).get(0), 50, 0);
					for (Pair<EmbeddedText, Double> p : context) {
						System.out.println("---[" + p.getRight() + "]-----------------------------------");
						System.out.println(p.getLeft().getText());
						System.out.println();
					}
					System.out.println("===================================================\n ");

					// Answer
					System.out.println("My Answer: ");
					QnAPair answer = qas.answerWithEmbeddings(question, context);
					System.out.println(answer.getAnswer() + "\n");
				}
			}

		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

}
