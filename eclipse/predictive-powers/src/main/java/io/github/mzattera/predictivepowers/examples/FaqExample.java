package io.github.mzattera.predictivepowers.examples;

import java.util.List;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.QnAPair;
import io.github.mzattera.predictivepowers.services.QuestionExtractionService;
import io.github.mzattera.util.ExtractionUtil;
import io.github.mzattera.util.LlmUtil;

public class FaqExample {

	public static void main(String[] args) throws Exception {

		// OpenAI end-point
		// Make sure you specify your API key n OPENAI_KEY system environment variable.
		try (OpenAiEndpoint endpoint = new OpenAiEndpoint()) {

			// Download Credit Suisse financial statement 2022 PDF and extract its text
			// We keep only one piece of 750 tokens
			String statment = LlmUtil.split(
					ExtractionUtil.fromUrl("https://www.credit-suisse.com/media/assets/corporate/docs/about-us/investor-relations/financial-disclosures/financial-reports/csg-ar-2022-en.pdf"),
					750)
					.get(2);

			// Our query generation service
			QuestionExtractionService q = endpoint.getQuestionExtractionService();

			// Get some FAQs and print them
			List<QnAPair> QnA = q.getQuestions(statment);
			for (int i = 0; (i < 3) & (i < QnA.size()); ++i) {
				System.out.println(QnA.get(i).toString());
			}
			System.out.println();

			// fill-the-gap questions
			QnA = q.getFillQuestions(statment);
			for (int i = 0; (i < 3) & (i < QnA.size()); ++i) {
				System.out.println(QnA.get(i).toString());
			}
			System.out.println();

			// true/false questions
			QnA = q.getTFQuestions(statment);
			for (int i = 0; (i < 3) & (i < QnA.size()); ++i) {
				System.out.println(QnA.get(i).toString());
			}
			System.out.println();

			// multiple choice questions
			QnA = q.getMCQuestions(statment);
			for (int i = 0; (i < 3) & (i < QnA.size()); ++i) {
				System.out.println(QnA.get(i).toString());
			}
			System.out.println();
		}
	} // closes endpoint
}
