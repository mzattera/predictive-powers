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
package io.github.mzattera.predictivepowers.services;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Abstract {@link QuestionAnsweringService} that can be sub-classed to create
 * other services faster (hopefully).
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public abstract class AbstractQuestionAnsweringService implements QuestionAnsweringService {

	@NonNull
	@Getter
	@Setter
	private String model;

	@Getter
	private Integer maxContextTokens = null;

	@Override
	public void setMaxContextTokens(Integer maxContextTokens) {
		if ((maxContextTokens != null) && (maxContextTokens.intValue() < 1))
			throw new IllegalArgumentException("Context needs to be of at least 1 token in size: " + maxContextTokens);

		this.maxContextTokens = maxContextTokens;
	}

	@Override
	public QnAPair answer(String question, @NonNull String context) {

		List<String> l = new ArrayList<>();
		l.add(context);
		return answer(question, l);
	}

	@Override
	public QnAPair answerWithEmbeddings(String question, @NonNull List<Pair<EmbeddedText, Double>> context) {

		List<String> l = new ArrayList<>(context.size());
		for (Pair<EmbeddedText, Double> p : context)
			l.add(p.getLeft().getText());

		QnAPair result = answer(question, l);

		// Enrich answer with embeddings, as they have in some case useful properties
		for (int i = 0; i < result.getContext().size(); ++i)
			result.getEmbeddingContext().add(context.get(i).getLeft());

		return result;
	}

	@Override
	public void close() {
	}
}