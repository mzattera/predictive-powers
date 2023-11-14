package io.github.mzattera.predictivepowers.services;

import java.util.List;

public interface QuestionExtractionService extends AiService {

	/**
	 * Extracts question/answer pairs from given text.
	 */
	List<QnAPair> getQuestions(String text);

	/**
	 * Extracts true/false type of questions from given text.
	 */
	List<QnAPair> getTFQuestions(String text);

	/**
	 * Extracts "fill the blank" type of questions from given text.
	 */
	List<QnAPair> getFillQuestions(String text);

	/**
	 * Extracts multiple-choice questions from given text.
	 */
	List<QnAPair> getMCQuestions(String text);

}