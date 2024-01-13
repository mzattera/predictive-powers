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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import io.github.mzattera.predictivepowers.AiEndpoint;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatMessage.Role;
import io.github.mzattera.predictivepowers.services.AbstractQuestionAnsweringService;
import io.github.mzattera.predictivepowers.services.ChatMessage;
import io.github.mzattera.predictivepowers.services.ModelService.Tokenizer;
import io.github.mzattera.predictivepowers.services.QnAPair;
import io.github.mzattera.predictivepowers.services.QuestionAnsweringService;
import io.github.mzattera.util.ChunkUtil;
import lombok.Getter;
import lombok.NonNull;

/**
 * OpenAI implementation of {@link QuestionAnsweringService}.
 * 
 * The service tries to prevent hallucinations (answers that do not use the
 * information in the context).
 * 
 * The service relies on an underlying {@link OpenAiChatService} to answer
 * questions. If you want to fine-tune this service, you can get the underlying
 * service (by calling {@link #getCompletionService()} and fine-tune that
 * instead.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class OpenAiQuestionAnsweringService extends AbstractQuestionAnsweringService {

	// De-searilize JSON answers
	private final static ObjectMapper mapper;
	static {
		mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
	}

	public OpenAiQuestionAnsweringService(OpenAiEndpoint ep) {
		this(ep.getChatService());

		// TODO test best settings.
		completionService.getDefaultReq().setTemperature(0.0);
	}

	public OpenAiQuestionAnsweringService(OpenAiChatService completionService) {
		this.completionService = completionService;
		setMaxContextTokens(getEndpoint().getModelService().getContextSize(getModel()) * 3 / 4);
	}

	@Override
	public AiEndpoint getEndpoint() {
		return completionService.getEndpoint();
	}

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
	 * 
	 * You can configure this underlying service to fine tune behavior of this
	 * instance.
	 */
	@NonNull
	@Getter
	private final OpenAiChatService completionService;

	/**
	 * Answer a question, using completion service.
	 */
	public QnAPair answer(String question) {
		OpenAiTextCompletion resp = completionService.complete(question);
		return QnAPair.builder().question(question).answer(resp.getText())
				.explanation("Answer provided by OpenAI competions API (model knowledge).").build();
	}

	@Override
	public QnAPair answer(String question, List<String> context) {

		// No context, no answer
		if (context.size() == 0)
			return QnAPair.builder().question(question).answer("I do not know.").explanation("No context was provided.")
					.build();

		String qMsg = "\nQuestion: " + question;
		Tokenizer counter = getEndpoint().getModelService().getTokenizer(getModel());

		// Provides instructions and examples
		// TODO URGENT Better delimiters for context and questions
		List<ChatMessage> instructions = new ArrayList<>();
		if (completionService.getPersonality() == null)
			instructions
					.add(new OpenAiChatMessage(Role.SYSTEM, "You are an AI assistant answering questions truthfully."));
		instructions.add(new OpenAiChatMessage(Role.SYSTEM,
				"Answer the below questions truthfully, strictly using only the information in the context. " + //
						"When providing an answer, provide your reasoning as well, step by step. " + //
						"If the answer cannot be found in the context, reply with \"I do not know.\". " + //
						"Strictly return the answer and explanation in JSON format.",
				"example_user"));
		instructions.add(new OpenAiChatMessage(Role.SYSTEM, "Context:\n" + //
				"Biglydoos are small rodent similar to mice.\n" + //
				"Biglydoos eat cranberries.\n" + //
				"Question: What color are biglydoos?", "example_user"));
		instructions.add(new OpenAiChatMessage(Role.SYSTEM, //
				"{\"answer\": \"I do not know.\", \"explanation\": \"1. This information is not provided in the context.\"}",
				"example_assistant"));
		instructions.add(new OpenAiChatMessage(Role.SYSTEM, "Context:\n" + //
				"Biglydoos are small rodent similar to mice.\n" + //
				"Biglydoos eat cranberries.\n" + //
				"Question: Do biglydoos eat fruits?", "example_user"));
		instructions.add(new OpenAiChatMessage(Role.SYSTEM, //
				"{\"answer\": \"Yes, biglydoos eat fruits.\", " + //
						"\"explanation\": " + //
						"\"1. The context states: \"Biglydoos eat cranberries.\"\\n" + //
						"2. Cranberries are a kind of fruit.\\n" + //
						"3. Therefore, biglydoos eat fruits.\"}",
				"example_assistant"));

		// Builds biggest context possible
		// TODO here the context size is correctly counted by looking only at the
		// context; however, this might cause problem as the actual prompt is longer,
		// should we add a method to let the developer know the size of the prompt?
		int tok = 0;
		StringBuilder ctx = new StringBuilder("Context:\n");
		int i = 0;
		for (; i < context.size(); ++i) {
			ChatMessage m = new OpenAiChatMessage(Role.USER, ctx.toString() + "\n" + context.get(i));
			if ((tok + counter.count(m)) > getMaxContextTokens())
				break;
			ctx.append('\n').append(context.get(i));
		}

		if (i == 0) { // The first context was too big already, take a share
			ctx.append(ChunkUtil.split(context.get(0), getMaxContextTokens(), counter).get(0));
		}

		ctx.append(qMsg);
		instructions.add(new OpenAiChatMessage(Role.USER, ctx.toString()));

		OpenAiTextCompletion answerJson = completionService.complete(instructions);
		QnAPair result = null;
		try {
			result = mapper.readValue(answerJson.getText(), QnAPair.class);
			result.setQuestion(question);
		} catch (JsonProcessingException e) {
			// Sometimes API returns only the answer, not as a JSON
			result = QnAPair.builder().question(question).answer(answerJson.getText()).build();
		}

		for (int j = 0; j < Math.max(1, i); ++j) {
			result.getContext().add(context.get(j));
		}

		return result;
	}
}