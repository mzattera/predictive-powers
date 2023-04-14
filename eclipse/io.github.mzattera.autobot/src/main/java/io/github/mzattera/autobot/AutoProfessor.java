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