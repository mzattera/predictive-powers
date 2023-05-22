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
package io.github.mzattera.autobot;

import java.io.File;
import java.util.Scanner;

/**
 * Creates a "professor" that asks you questions based on the content of a
 * document or web page.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public final class AutoProfessor {

	private AutoProfessor() {
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try (Scanner scanner = new Scanner(System.in)) {
//			Document input = Document.fromOfficeFile(new File("D:\\Using Data to Gain an Advantage in ESG Investing.docx"));
			Document input = Document.fromURL("https://en.wikipedia.org/wiki/Imploding_the_Mirage");

			System.out.println("===[ Input Document ]==================================");
			System.out.println(input.text);

			input.addQnA();

			for (Document.QnA qna : input.qnas) {
				System.out.println("\nMy Question: " + qna.question);
				System.out.println("Your Answer: ");
				String inputString = scanner.nextLine();
				if (GPT.compareSentence(inputString, qna.answer)) {
					System.out.println("Exactly!");
				} else {
					System.out.println("Sorry, the correct answer is: " + qna.answer);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}