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

import io.github.mzattera.predictivepowers.huggingface.client.nlp.QuestionAnsweringRequest;
import io.github.mzattera.predictivepowers.huggingface.client.nlp.QuestionAnsweringResponse;
import io.github.mzattera.predictivepowers.huggingface.endpoint.HuggingFaceEndpoint;
import io.github.mzattera.predictivepowers.services.AbstractQuestionAnsweringService;
import io.github.mzattera.predictivepowers.services.EmbeddedText;
import io.github.mzattera.predictivepowers.services.ModelService.Tokenizer;
import io.github.mzattera.predictivepowers.services.QnAPair;
import io.github.mzattera.predictivepowers.services.QuestionAnsweringService;
import io.github.mzattera.util.LlmUtil;
import lombok.Getter;
import lombok.NonNull;

/**
 * OpenAI implementation of {@link QuestionAnsweringService}.
 * 
 * Notice that Inference API does not put a limit on context size, however we
 * set it to {@link DEFAULT_MAX_CONTEXT_SIZE} tokens by default.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class HuggingFaceQuestionAnsweringService extends AbstractQuestionAnsweringService {

	public final static String DEFAULT_MODEL = "allenai/longformer-large-4096-finetuned-triviaqa";

	public final static int DEFAULT_MAX_CONTEXT_SIZE = 2000;

//	public final static String DEFAULT_MODEL = "deepset/roberta-base-squad2"; // Always super busy
//	public final static String DEFAULT_MODEL = "deepset/tinyroberta-squad2";
//	public final static String DEFAULT_MODEL = "deepset/roberta-large-squad2";

	public HuggingFaceQuestionAnsweringService(HuggingFaceEndpoint ep) {
		this.endpoint = ep;
		defaultReq = new QuestionAnsweringRequest();
		defaultReq.getOptions().setWaitForModel(true); // TODO remove? Improve?
		setModel(DEFAULT_MODEL);
		setMaxContextTokens(DEFAULT_MAX_CONTEXT_SIZE);
	}

	@NonNull
	@Getter
	protected final HuggingFaceEndpoint endpoint;

	/**
	 * This request, with its parameters, is used as default setting for each call.
	 * 
	 * You can change any parameter to change these defaults (e.g. the model used)
	 * and the change will apply to all subsequent calls.
	 */
	@Getter
	@NonNull
	private final QuestionAnsweringRequest defaultReq;

	/**
	 * Answer a question, using only information from provided context.
	 * 
	 * @param context The information to be used to answer the question. Notice this
	 *                might be shortened if it is too big for the model being used.
	 * @param req     Request to use in the API call.
	 */
	public QnAPair answer(String question, @NonNull String context, @NonNull QuestionAnsweringRequest req) {

		List<String> l = new ArrayList<>();
		l.add(context);

		return answer(question, l, req);
	}

	@Override
	public QnAPair answer(String question, @NonNull List<String> context) {
		return answer(question, context, defaultReq);
	}

	/**
	 * Answer a question, using only information from provided context.
	 * 
	 * @param context The information to be used to answer the question. Notice this
	 *                might be shortened if it is too big for the model being used.
	 * @param req     Request to use in the API call.
	 */
	public QnAPair answer(String question, @NonNull List<String> context, @NonNull QuestionAnsweringRequest req) {

		// No context, no answer
		if (context.size() == 0)
			return QnAPair.builder().question(question).answer("I do not know.").explanation("No context was provided.")
					.build();

		// Builds biggest context possible
		Tokenizer counter = getEndpoint().getModelService().getTokenizer(getModel());
		int instTok = 0;
		StringBuilder ctx = new StringBuilder();
		int i = 0;
		for (; i < context.size(); ++i) {
			if ((instTok + counter.count("\n" + context.get(i))) > getMaxContextTokens())
				break;
			ctx.append(context.get(i)).append('\n');
		}

		if (i == 0) { // The first context was too big already, take a share
			ctx.append(LlmUtil.splitByTokens(context.get(0), getMaxContextTokens(), counter).get(0));
		}

		req.getInputs().setQuestion(question);
		req.getInputs().setContext(ctx.toString());

		QuestionAnsweringResponse resp = endpoint.getClient().questionAnswering(getModel(), req);

		return QnAPair.builder().question(question).answer(resp.getAnswer()).context(context.subList(0, Math.max(1, i)))
				.explanation("Unfortunately, this model cannot provide any explanation.").build();

	}

	/**
	 * Answer a question, using only information from provided context.
	 * 
	 * @param context The information to be used to answer the question. Notice this
	 *                might be shortened if it is too big for the model being used.
	 * @param req     Request to use in the API call.
	 */
	public QnAPair answerWithEmbeddings(String question, @NonNull List<Pair<EmbeddedText, Double>> context,
			@NonNull QuestionAnsweringRequest req) {

		List<String> l = new ArrayList<>(context.size());
		for (Pair<EmbeddedText, Double> p : context)
			l.add(p.getLeft().getText());

		QnAPair result = answer(question, l, req);

		// Enrich answer with embeddings, as they have in some case useful properties
		for (int i = 0; i < result.getContext().size(); ++i)
			result.getEmbeddingContext().add(context.get(i).getLeft());

		return result;
	}
}