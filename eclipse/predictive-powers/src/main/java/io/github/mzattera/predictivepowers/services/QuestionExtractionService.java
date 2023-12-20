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