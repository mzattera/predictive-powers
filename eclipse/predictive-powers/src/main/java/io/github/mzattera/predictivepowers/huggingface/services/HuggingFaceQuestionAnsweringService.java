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
package io.github.mzattera.predictivepowers.huggingface.services;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import io.github.mzattera.predictivepowers.huggingface.endpoint.HuggingFaceEndpoint;
import io.github.mzattera.predictivepowers.huggingface.nlp.QuestionAnsweringRequest;
import io.github.mzattera.predictivepowers.huggingface.nlp.QuestionAnsweringResponse;
import io.github.mzattera.predictivepowers.services.EmbeddedText;
import io.github.mzattera.predictivepowers.services.QnAPair;
import io.github.mzattera.predictivepowers.services.QuestionAnsweringService;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * This class provides method to answer questions relying on a context that is
 * provided.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class HuggingFaceQuestionAnsweringService implements QuestionAnsweringService {

	public final static String DEFAULT_MODEL = "allenai/longformer-large-4096-finetuned-triviaqa";
//	public final static String DEFAULT_MODEL = "deepset/roberta-base-squad2"; // Always super busy
//	public final static String DEFAULT_MODEL = "deepset/tinyroberta-squad2";
//	public final static String DEFAULT_MODEL = "deepset/roberta-large-squad2";

	// Maps from-to POJO <-> JSON
	private final static ObjectMapper mapper;
	static {
		mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
	}

	public HuggingFaceQuestionAnsweringService(HuggingFaceEndpoint ep) {
		this.endpoint = ep;
		defaultReq = new QuestionAnsweringRequest();
		defaultReq.getOptions().setWaitForModel(true); // TODO remove? Improve?
	}

	@NonNull
	@Getter
	private final HuggingFaceEndpoint endpoint;

	/**
	 * This request, with its parameters, is used as default setting for each call.
	 * 
	 * You can change any parameter to change these defaults (e.g. the model used)
	 * and the change will apply to all subsequent calls.
	 */
	@Getter
	@NonNull
	private final QuestionAnsweringRequest defaultReq;

	@NonNull
	@Getter
	@Setter
	private String model = DEFAULT_MODEL;

	@Override
	public QnAPair answer(String question) {
		return answer(question, "");
	}

	@Override
	public QnAPair answer(String question, String context) {

		return answer(question, context, defaultReq);
	}

	public QnAPair answer(String question, String context, QuestionAnsweringRequest req) {

		req.getInputs().setQuestion(question);
		req.getInputs().setContext(context);

		QuestionAnsweringResponse resp = endpoint.getClient().questionAnswering(model, req);

		List<String> l = new ArrayList<>();
		l.add(context);
		return QnAPair.builder().question(question).answer(resp.getAnswer()).context(l)
				.explanation("Unfortunately, this model cannot provide any explanation.").build();
	}

	@Override
	public QnAPair answerWithEmbeddings(String question, List<Pair<EmbeddedText, Double>> context) {
		return answerWithEmbeddings(question, context, defaultReq);
	}

	public QnAPair answerWithEmbeddings(String question, List<Pair<EmbeddedText, Double>> context,
			QuestionAnsweringRequest req) {

		List<String> l = new ArrayList<>(context.size());
		for (Pair<EmbeddedText, Double> p : context)
			l.add(p.getLeft().getText());

		QnAPair result = answer(question, l, req);

		// Enrich answer with embeddings, as they have in some case useful properties
		for (int i = 0; i < result.getContext().size(); ++i)
			result.getEmbeddingContext().add(context.get(i).getLeft());

		return result;
	}

	@Override
	public QnAPair answer(String question, List<String> context) {
		return answer(question, context, defaultReq);
	}

	public QnAPair answer(String question, List<String> context, QuestionAnsweringRequest req) {
		StringBuffer ctx = new StringBuffer();
		for (String c : context) {
			ctx.append(c.trim()).append('\n');
		}

		return answer(question, ctx.toString().trim(), req);
	}
}