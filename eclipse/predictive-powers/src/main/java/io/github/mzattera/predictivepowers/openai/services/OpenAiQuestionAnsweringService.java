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

import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatMessage.Role;
import io.github.mzattera.predictivepowers.services.AbstractQuestionAnsweringService;
import io.github.mzattera.predictivepowers.services.QnAPair;
import io.github.mzattera.predictivepowers.services.QuestionAnsweringService;
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
		completionService = ep.getChatService();
		completionService.setPersonality(null);
		completionService.getDefaultReq().setTemperature(0.0); // TODO test best settings.

		// TODO URGENT set a completion model that supports this
//		completionService.getDefaultReq().setResponseFormat(ResponseFormat.JSON); // More robust replies
	}

	@Override
	public OpenAiEndpoint getEndpoint() {
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

		// Provides instructions and examples
		List<OpenAiChatMessage> instructions = new ArrayList<>();
		instructions.add(new OpenAiChatMessage(Role.SYSTEM,
				"You are an agent that answers users' questions truthfully.\n"
						+ "At each conversation step, you will be provided with a user's question, in a <question> tag,"
						+ " and a context, in a <context> tags, containing the information you must use to answer"
						+ " the question.\n"
						+ "Your task is to answer the question using only the data provided in the corresponding context."
						+ " Importantly, do not use any additional information when answering the question,"
						+ " except that is contained in the context. If the answer cannot be found in the context,"
						+ " reply with \"I do not know.\".\n"
						+ "When creating the answer, think step by step and provide your reasoning as well.\n"
						+ "Return both the answer and the reasoning in JSON format."));
		instructions.add(new OpenAiChatMessage(Role.SYSTEM,
				"<context>\n" + "Biglydoos are small rodent similar to mice.\n" + "Biglydoos eat cranberries.\n"
						+ "Biglydoos are green.\n" + "</context>\n" + "<question>Do biglydoos eat fruits?</question>",
				"example_user"));
		instructions.add(new OpenAiChatMessage(Role.SYSTEM, //
				"{\n" + "  \"answer\": \"Yes, biglydoos eat fruits.\",\n"
						+ "  \"explanation\": \"1. The context states: \"Biglydoos eat cranberries\".\\n"
						+ "2. Cranberries are a kind of fruit.\\n" + "3. Therefore, biglydoos eat fruits.\"\n" + "}",
				"example_assistant"));
		instructions.add(new OpenAiChatMessage(Role.SYSTEM,
				"<context>\n" + "Biglydoos are small rodent similar to mice.\n" + "Biglydoos eat cranberries.\n"
						+ "</context>\n" + "<question>What color are biglydoos?</question>",
				"example_user"));
		instructions.add(new OpenAiChatMessage(Role.SYSTEM, //
				"{\n" + "  \"answer\": \"I do not know.\",\n"
						+ "  \"explanation\": \"1. This information is not provided in the context.\"\n" + "}",
				"example_assistant"));
		StringBuilder b = new StringBuilder();
		b.append("<context>\n</context>\n");
		b.append("<question>").append(question).append("</question>");
		OpenAiChatMessage userMsg = new OpenAiChatMessage(Role.USER, b.toString());
		instructions.add(userMsg);

		// Calculate size of instructions
		OpenAiTokenizer counter = getEndpoint().getModelService().getTokenizer(getModel());
		int ctxSize = getEndpoint().getModelService().getContextSize(getModel());
		int instructionSize = completionService.getBaseTokens() + counter.count(instructions) + 5;
		if (instructionSize >= ctxSize)
			throw new IllegalArgumentException("Instrutions too long to fit the context");

		// TODO Allow the developer to set this ratio?
		// 3/4 of the remaining context is allocated for the input text
		int txtSize = 3 * (ctxSize - instructionSize) / 4;
		if (txtSize <= 0)
			throw new IllegalArgumentException("Instrutions too long to fit the context");

		// the remaining 1/4 for the generated list of questions
		int replySize = ctxSize - instructionSize - txtSize;
		if (replySize <= 0)
			throw new IllegalArgumentException("Instrutions too long to fit the context");
		completionService
				.setMaxNewTokens(Math.min(replySize, getEndpoint().getModelService().getMaxNewTokens(getModel())));

		// Builds biggest context possible
		b.setLength(0);
		int tok = 0;
		int i = 0;
		for (; i < context.size(); ++i) {
			String ctx = context.get(i);
			int t = counter.count(ctx);
			if (tok + t > txtSize)
				break;
			tok += t;

			if (i > 0)
				b.append("\n");
			b.append(context.get(i));
		}
		if (i == 0)
			throw new IllegalArgumentException("First piece of context too big");
		String ctx = b.toString();

		b.setLength(0);
		b.append("<context>\n").append(ctx).append("\n</context>\n");
		b.append("<question>").append(question).append("</question>");
		userMsg.setContent(b.toString());

		OpenAiTextCompletion answerJson = completionService.complete(instructions);
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