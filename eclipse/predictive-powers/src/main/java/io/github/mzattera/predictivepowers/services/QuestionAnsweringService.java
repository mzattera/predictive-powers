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

import org.apache.commons.lang3.tuple.Pair;

public interface QuestionAnsweringService extends Service {

	/**
	 * Answer a question, no context provided.
	 */
	QnAPair answer(String question);

	/**
	 * Answer a question, using only information from provided context.
	 * 
	 * @param context The information to be used to answer the question. Notice this
	 *                might be shortened if it is too big for the model being used.
	 */
	QnAPair answer(String question, String context);

	/**
	 * Answer a question, using only information from provided context.
	 * 
	 * @param context The information to be used to answer the question. Notice this
	 *                might be shortened if it is too big for the model being used.
	 */
	QnAPair answerWithEmbeddings(String question, List<Pair<EmbeddedText, Double>> context);

	/**
	 * Answer a question, using only information from provided context.
	 * 
	 * @param context The information to be used to answer the question. Notice this
	 *                might be shortened if it is too big for the model being used.
	 */
	QnAPair answer(String question, List<String> context);
}