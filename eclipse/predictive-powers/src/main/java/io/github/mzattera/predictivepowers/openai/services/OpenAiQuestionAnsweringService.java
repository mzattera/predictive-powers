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
package io.github.mzattera.predictivepowers.openai.services;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.util.ModelUtil;
import io.github.mzattera.predictivepowers.openai.util.TokenUtil;
import io.github.mzattera.predictivepowers.services.ChatMessage;
import io.github.mzattera.predictivepowers.services.ChatService;
import io.github.mzattera.predictivepowers.services.EmbeddedText;
import io.github.mzattera.predictivepowers.services.QnAPair;
import io.github.mzattera.predictivepowers.services.QuestionAnsweringService;
import io.github.mzattera.predictivepowers.services.TextResponse;
import io.github.mzattera.util.LlmUtil;
import lombok.Getter;
import lombok.NonNull;

/**
 * This class provides method to answer questions relying on a context that is
 * provided. The service tries to prevent hallucinations (answers that do not
 * use the information in the context).
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class OpenAiQuestionAnsweringService implements QuestionAnsweringService {

	public OpenAiQuestionAnsweringService(OpenAiEndpoint ep) {
		this(ep, ep.getChatService());

		// TODO test best settings.
		completionService.getDefaultReq().setTemperature(0.0);
	}

	public OpenAiQuestionAnsweringService(OpenAiEndpoint ep, ChatService completionService) {
		this.endpoint = ep;
		this.completionService = completionService;
		maxContextTokens = Math.max(ModelUtil.getContextSize(this.completionService.getDefaultReq().getModel()), 2046)
				* 3 / 4;
	}

	// Maps from-to POJO <-> JSON
	private final static ObjectMapper mapper;
	static {
		mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
	}

	@NonNull
	@Getter
	private final OpenAiEndpoint endpoint;

	@Override
	public String getModel() {
		return completionService.getModel();
	}

	@Override
	public void setModel(@NonNull String model) {
		completionService.setModel(model);
	}

	/**
	 * This underlying service is used for executing required prompts.
	 */
	@NonNull
	@Getter
	private final ChatService completionService;

	/**
	 * Maximum number of tokens to keep in the question context when answering.
	 * 
	 * As there is no Java code available for an exact calculation, this is
	 * approximated.
	 */
	@Getter
	private int maxContextTokens;

	public void setMaxContextTokens(int n) {
		if (n < 1)
			throw new IllegalArgumentException("Must keep at least 1 token.");
		maxContextTokens = n;
	}

	@Override
	public QnAPair answer(String question) {
		TextResponse answer = completionService.complete(question);
		return QnAPair.builder().question(question).answer(answer.getText()).build();
	}

	@Override
	public QnAPair answer(String question, String context) {

		List<String> l = new ArrayList<>();
		l.add(LlmUtil.split(context, maxContextTokens - TokenUtil.count(question) - 400).get(0)); // 400 is to keep
																									// space for
																									// instructions and
																									// examples

		return answer(question, l);
	}

	@Override
	public QnAPair answerWithEmbeddings(String question, List<Pair<EmbeddedText, Double>> context) {

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
	public QnAPair answer(String question, List<String> context) {

		String qMsg = "\nQuestion: " + question;

		// Provides instructions and examples
		List<ChatMessage> instructions = new ArrayList<>();
		if (completionService.getPersonality() == null)
			instructions.add(new ChatMessage("system", "You are an AI assistant answering questions truthfully."));
		instructions.add(new ChatMessage("user",
				"Answer the below questions truthfully, using only the information in the context. " + //
						"When providing an answer, provide your reasoning as well, step by step. " + //
						"If the answer cannot be found in the context, reply with \"I do not know.\". " + //
						"Strictly return the answer and explanation in JSON format, as shown below."));
		instructions.add(new ChatMessage("user", "Context:\n" + //
				"Biglydoos are small rodent similar to mice.\n" + //
				"Biglydoos eat cranberries.\n" + //
				"Question: What color are biglydoos?"));
		instructions.add(new ChatMessage("assistant", //
				"{\"answer\": \"I do not know.\", \"explanation\": \"1. This information is not provided in the context.\"}"));
		instructions.add(new ChatMessage("user", "Context:\n" + //
				"Biglydoos are small rodent similar to mice.\n" + //
				"Biglydoos eat cranberries.\n" + //
				"Question: Do biglydoos eat fruits?"));
		instructions.add(new ChatMessage("assistant", //
				"{\"answer\": \"Yes, biglydoos eat fruits.\", " + //
						"\"explanation\": " + //
						"\"1. The context states: \"Biglydoos eat cranberries.\"\\n" + //
						"2. Cranberries are a kind of fruit.\\n" + //
						"3. Therefore, biglydoos eat fruits.\"}"));
		int instTok = TokenUtil.count(new ChatMessage("user", qMsg)) + TokenUtil.count(instructions);

		StringBuffer ctx = new StringBuffer("Context:\n");
		int i = 0;
		for (; i < context.size(); ++i) {
			ChatMessage m = new ChatMessage("user", ctx.toString() + "\n" + context.get(i));
			if ((instTok + TokenUtil.count(m)) > maxContextTokens)
				break;
			ctx.append('\n').append(context.get(i));
		}
		ctx.append(qMsg);
		instructions.add(new ChatMessage("user", ctx.toString()));

		// No context, no answer
		if (i == 0)
			return QnAPair.builder().question(question).answer("I do not know.").explanation("No context was provided.")
					.build();

		TextResponse answerJson = completionService.complete(instructions);
		QnAPair result = null;
		try {
			result = mapper.readValue(answerJson.getText(), QnAPair.class);
			result.setQuestion(question);
		} catch (JsonProcessingException e) {
			// Sometimes API returns only the answer, not as a JSON
			result = QnAPair.builder().question(question).answer(answerJson.getText()).build();
		}

		for (int j = 0; j < i; ++j) {
			result.getContext().add(context.get(j));
		}

		return result;
	}
}