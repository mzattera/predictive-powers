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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.github.mzattera.predictivepowers.openai.client.DirectOpenAiClient;
import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;
import io.github.mzattera.predictivepowers.openai.client.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatMessage.Role;
import io.github.mzattera.predictivepowers.services.QnAPair;
import io.github.mzattera.predictivepowers.services.QuestionExtractionService;
import io.github.mzattera.util.ChunkUtil;
import lombok.NonNull;

/**
 * This class provides method to extract different types of questions from a
 * text.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class OpenAiQuestionExtractionService implements QuestionExtractionService {

	private final static Logger LOG = LoggerFactory.getLogger(OpenAiQuestionExtractionService.class);

	public static final String DEFAULT_MODEL = "gpt-4-turbo-preview";

	/**
	 * This underlying service is used for executing required prompts.
	 */
	@NonNull
	private final OpenAiChatService completionService;

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

	public OpenAiQuestionExtractionService(@NonNull OpenAiEndpoint endpoint) {
		this(endpoint, DEFAULT_MODEL);
	}

	public OpenAiQuestionExtractionService(@NonNull OpenAiEndpoint endpoint, @NonNull String model) {
		completionService = endpoint.getChatService(model);
		completionService.setPersonality(null);
		completionService.setTemperature(0.0); // TODO test best settings.
//		completionService.getDefaultReq().setResponseFormat(ResponseFormat.JSON); // Adding this produces less results
	}

	/**
	 * Extracts question/answer pairs from given text.
	 */
	@Override
	public List<QnAPair> getQuestions(String text) {

		// Provides instructions and examples
		List<OpenAiChatMessage> instructions = new ArrayList<>();
		instructions.add(new OpenAiChatMessage(Role.SYSTEM,
				"You are a teacher and you are preparing an assessment from some text materials.", null));
		instructions.add(new OpenAiChatMessage(Role.SYSTEM,
				"Given a context, extract a set of questions and corresponding answers."
						+ " Extract as many question/answering pairs as you can."
						+ " Format question/answer pairs as a JSON array, importantly always return an array, even if you extract a single pair."
						+ " Some examples are provided below.",
				"example_user"));
		instructions.add(new OpenAiChatMessage(Role.SYSTEM, "Context:\n'''\n" //
				+ "Mount Everest  is Earth's highest mountain above sea level, located in the Mahalangur Himal sub-range of the Himalayas. The China–Nepal border runs across its summit point. Its elevation (snow height) of 8,848.86 m (29,031 ft 8+1⁄2 in) was most recently established in 2020 by the Chinese and Nepali authorities.\n" //
				+ "Mount Everest attracts many climbers, including highly experienced mountaineers. There are two main climbing routes, one approaching the summit from the southeast in Nepal (known as the 'standard route') and the other from the north in Tibet. While not posing substantial technical climbing challenges on the standard route, Everest presents dangers such as altitude sickness, weather, and wind, as well as hazards from avalanches and the Khumbu Icefall. As of 2019, over 300 people have died on Everest, many of whose bodies remain on the mountain.\n" //
				+ "'''", "example_user"));
		instructions.add(new OpenAiChatMessage(Role.SYSTEM, "[\n" //
				+ "   {\n" //
				+ "      \"question\":\"What is the highest mountain on Earth?\",\n" //
				+ "      \"answer\":\"Mount Everest is Earth's highest mountain above sea level, located in the Mahalangur Himal sub-range of the Himalayas.\"\n" //
				+ "   },\n" //
				+ "   {\n" //
				+ "      \"question\":\"What are the two main climbing routes for Mount Everest?\",\n" //
				+ "      \"answer\":\"There are two main climbing routes, one approaching the summit from the southeast in Nepal (known as the 'standard route') and the other from the north in Tibet.\"\n" //
				+ "   },\n" //
				+ "   {\n" //
				+ "      \"question\":\"How many people have died on Everest as of 2019?\",\n" //
				+ "      \"answer\":\"As of 2019, over 300 people have died on Everest.\"\n" //
				+ "   }\n" //
				+ "]", "example_assistant"));

		return getQuestions(instructions, text);
	}

	/**
	 * Extracts true/false type of questions from given text.
	 */
	@Override
	public List<QnAPair> getTFQuestions(String text) {

		// Provides instructions and examples
		List<OpenAiChatMessage> instructions = new ArrayList<>();
		instructions.add(new OpenAiChatMessage(Role.SYSTEM,
				"You are a teacher and you are preparing an assessment from some text materials.", null));
		instructions.add(new OpenAiChatMessage(Role.SYSTEM,
				"Given a context, extract a set of true/false exercises and corresponding answers; make sure some questions require a 'true' answer and  some require a 'false' answer. "
						+ " Extract as many exercises as you can."
						+ " Format exercises as a JSON array, importantly always return an array, even if you extract a single exercise."
						+ " Some examples are provided below.",
				"example_user"));
		instructions.add(new OpenAiChatMessage(Role.SYSTEM, "Context:\n'''\n" //
				+ "Mount Everest  is Earth's highest mountain above sea level, located in the Mahalangur Himal sub-range of the Himalayas. The China–Nepal border runs across its summit point. Its elevation (snow height) of 8,848.86 m (29,031 ft 8+1⁄2 in) was most recently established in 2020 by the Chinese and Nepali authorities.\n" //
				+ "Mount Everest attracts many climbers, including highly experienced mountaineers. There are two main climbing routes, one approaching the summit from the southeast in Nepal (known as the 'standard route') and the other from the north in Tibet. While not posing substantial technical climbing challenges on the standard route, Everest presents dangers such as altitude sickness, weather, and wind, as well as hazards from avalanches and the Khumbu Icefall. As of 2019, over 300 people have died on Everest, many of whose bodies remain on the mountain.\n" //
				+ "'''", "example_user"));
		instructions.add(new OpenAiChatMessage(Role.ASSISTANT, "[\n" //
				+ "   {\n" //
				+ "      \"question\":\"Mount Everest is the highest mountain on Earth.\",\n" //
				+ "      \"answer\":\"true\"\n" //
				+ "   },\n" //
				+ "   {\n" //
				+ "      \"question\":\"The so-called 'standard route' is one of the main climbing routes.\",\n" //
				+ "      \"answer\":\"true\"\n" //
				+ "   },\n" //
				+ "   {\n" //
				+ "      \"question\":\"As of 2019, less than 300 people have died on Everest.\",\n" //
				+ "      \"answer\":\"false\"\n" //
				+ "   }\n" //
				+ "]", "example_assistant"));

		List<QnAPair> result = getQuestions(instructions, text);
		Iterator<QnAPair> it = result.iterator();
		while (it.hasNext()) {
			QnAPair q = it.next();
			q.setAnswer(q.getAnswer().trim().toLowerCase());
			if (!"true".equals(q.getAnswer()) && "false".equals(q.getAnswer())) {// bad result
				LOG.info("QnA pair removed: {}", q);
				it.remove();
			}
		}

		return result;
	}

	/**
	 * Extracts "fill the blank" type of questions from given text.
	 */
	@Override
	public List<QnAPair> getFillQuestions(String text) {
		// Provides instructions and examples
		List<OpenAiChatMessage> instructions = new ArrayList<>();
		instructions.add(new OpenAiChatMessage(Role.SYSTEM,
				"You are a teacher and you are preparing an assessment from some text materials.", null));
		instructions.add(new OpenAiChatMessage(Role.SYSTEM,
				"Create 'fill the blank' exercises with corresponding fill words from the given context."
						+ " Make sure to generate questions where a missing word is replaced with a blank, denoted as '______', and provide the missing word as the answer."
						+ " Generate as many exercises as you can."
						+ " Format exercises as a JSON array, importantly always return an array, even if you extract a single result."
						+ " Some examples are provided below.",
				"example_user"));
		instructions.add(new OpenAiChatMessage(Role.SYSTEM, "Context:\r\n" + "'''\r\n"
				+ "Mount Everest  is Earth's highest mountain above sea level, located in the Mahalangur Himal sub-range of the Himalayas. The China\u2013Nepal border runs across its summit point. Its elevation (snow height) of 8,848.86 m (29,031 ft 8+1\u20442 in) was most recently established in 2020 by the Chinese and Nepali authorities.\r\n"
				+ "'''", "example_user"));
		instructions.add(new OpenAiChatMessage(Role.SYSTEM,
				"[\r\n" + "   {\r\n" + "      \"question\":\"Which is Earth's highest mountain above sea level?\",\r\n"
						+ "      \"answer\":\"Mount Everest\"\r\n" + "   }\r\n" + "]",
				"example_assistant"));
		instructions.add(new OpenAiChatMessage(Role.SYSTEM,
				"This is wrong, this is not a  'fill the blank' exercises. Try again.", "example_user"));
		instructions.add(new OpenAiChatMessage(Role.SYSTEM,
				"[\r\n" + "   {\r\n"
						+ "      \"question\":\"Mount ______ is Earth's highest mountain above sea level.\",\r\n"
						+ "      \"answer\":\"Everest\"\r\n" + "   }\r\n" + "]",
				"example_assistant"));
		instructions.add(new OpenAiChatMessage(Role.SYSTEM, "This is correct.", "example_user"));

		List<QnAPair> result = getQuestions(instructions, text);
		Iterator<QnAPair> it = result.iterator();
		while (it.hasNext()) {
			QnAPair q = it.next();
			if (q.getAnswer() == null) {// bad result
				LOG.info("QnA pair removed: {}", q);
				it.remove();
				continue;
			}

			q.setAnswer(q.getAnswer().trim());

			if (!q.getQuestion().contains("___")) {
				LOG.info("QnA pair removed: {}", q);
				it.remove();
				continue;
			}

			// how many words in the answer? Not more than 2 per question (e.g. "Alan
			// Turing").
			if (q.getAnswer().split("\\s").length > 2) {
				LOG.info("QnA pair removed: {}", q);
				it.remove();
				continue;
			}
		}

		return result;
	}

	/**
	 * Extracts multiple-choice questions from given text.
	 */
	@Override
	public List<QnAPair> getMCQuestions(String text) {

		// Provides instructions and examples
		List<OpenAiChatMessage> instructions = new ArrayList<>();
		instructions.add(new OpenAiChatMessage(Role.SYSTEM,
				"You are a teacher and you are preparing an assessment from some text materials.", null));
		instructions.add(new OpenAiChatMessage(Role.SYSTEM,
				"Given a context, extract a set of multiple-choice questions, corresponding answers, and a list of options for each question"
						+ " Make sure the options for one question are all different."
						+ " Extract as many question/answering pairs as you can."
						+ " Format question/answer pairs as a JSON array, importantly always return an array, even if you extract a single pair."
						+ " Some examples are provided below.",
				"example_user"));
		instructions.add(new OpenAiChatMessage(Role.SYSTEM, "Context:\n'''\n" //
				+ "Mount Everest  is Earth's highest mountain above sea level, located in the Mahalangur Himal sub-range of the Himalayas. The China–Nepal border runs across its summit point. Its elevation (snow height) of 8,848.86 m (29,031 ft 8+1⁄2 in) was most recently established in 2020 by the Chinese and Nepali authorities.\n" //
				+ "Mount Everest attracts many climbers, including highly experienced mountaineers. There are two main climbing routes, one approaching the summit from the southeast in Nepal (known as the 'standard route') and the other from the north in Tibet. While not posing substantial technical climbing challenges on the standard route, Everest presents dangers such as altitude sickness, weather, and wind, as well as hazards from avalanches and the Khumbu Icefall. As of 2019, over 300 people have died on Everest, many of whose bodies remain on the mountain.\n" //
				+ "'''", "example_user"));
		instructions.add(new OpenAiChatMessage(Role.SYSTEM, "[\n" //
				+ "   {\n" //
				+ "      \"question\":\"What is the highest mountain on Earth?\",\n" //
				+ "      \"options\":[\n" //
				+ "         \"1. Mount Everest\",\n" //
				+ "         \"2. K2\",\n" //
				+ "         \"3. Mount Kilimanjaro\",\n" //
				+ "         \"4. Mont Blanc\",\n" //
				+ "         \"5. Denali (Mount McKinley)\"\n" //
				+ "      ]," //
				+ "      \"answer\":\"1\"\n" //
				+ "   },\n" //
				+ "   {\n" //
				+ "      \"question\":\"How many people have died on Everest as of 2019?\",\n" //
				+ "      \"options\":[\n" //
				+ "         \"1. Nobody died on Everest\",\n" //
				+ "         \"2. Less than 10\",\n" //
				+ "         \"3. Around 100\",\n" //
				+ "         \"4. Over 300\",\n" //
				+ "         \"5. The number is unknown\"\n" //
				+ "      ],\n" //
				+ "      \"answer\":\"4\"\n" //
				+ "   },\n" //
				+ "   {\n" //
				+ "      \"question\":\"In which country is Mount Everest located?\",\n" //
				+ "      \"options\":[\n" //
				+ "         \"1. In China\",\n" //
				+ "         \"2. In India\",\n" //
				+ "         \"3. On the China–Nepal border.\",\n" //
				+ "         \"4. In Pakistan\",\n" //
				+ "         \"5. On the China–Russia border.\"\n" //
				+ "      ],\n" //
				+ "      \"answer\":\"3\"\n" //
				+ "   }\n" //
				+ "]", "example_assistant"));

		List<QnAPair> result = getQuestions(instructions, text);
		Iterator<QnAPair> it = result.iterator();
		while (it.hasNext()) {
			QnAPair q = it.next();
			if (q.getAnswer() == null) {// bad result
				LOG.info("QnA pair removed: {}", q);
				it.remove();
				continue;
			}

			q.setAnswer(q.getAnswer().trim());

			int c = -1;
			try {
				c = Integer.parseInt(q.getAnswer());
			} catch (Exception e) {
				LOG.info("QnA pair removed: {}", q);
				it.remove();
				continue;
			}

			if ((c <= 0) || (c > q.getOptions().size())) {
				LOG.info("QnA pair removed: {}", q);
				it.remove();
				continue;
			}
		}

		return result;
	}

	/**
	 * Extract questions, following given instructions.
	 * 
	 * It also splits the input text in smaller chunks, if needed.
	 * 
	 * @param instructions
	 * @param text
	 * @return
	 */
	private List<QnAPair> getQuestions(List<OpenAiChatMessage> instructions, String text) {

		// Calculate size of instructions
		OpenAiTokenizer counter = getEndpoint().getModelService().getTokenizer(getModel());
		int ctxSize = getEndpoint().getModelService().getContextSize(getModel());
		int instructionSize = completionService.getBaseTokens() + counter.count(instructions) + 5;
		if (instructionSize >= ctxSize)
			throw new IllegalArgumentException("Instrutions too long to fit the context");

		// TODO Allow the developer to set this ratio?
		// 2/3 of the remaining context is allocated for the input text
		int txtSize = 2 * (ctxSize - instructionSize) / 3;
		if (txtSize <= 5)
			throw new IllegalArgumentException("Instrutions too long to fit the context");

		// the remaining 1/3 for the generated list of questions
		int replySize = ctxSize - instructionSize - txtSize;
		if (replySize <= 0)
			throw new IllegalArgumentException("Instrutions too long to fit the context");
		completionService
				.setMaxNewTokens(Math.min(replySize, getEndpoint().getModelService().getMaxNewTokens(getModel())));

		// Chunk text with a sliding & slightly overlapping window
		List<QnAPair> result = new ArrayList<>();
		for (String t : ChunkUtil.split(text, txtSize / 5, 5, 4, counter)) {
			result.addAll(getQuestionsShort(instructions, t));
		}

		return result;
	}

	/**
	 * Extract questions, following given instructions.
	 * 
	 * @param instructions
	 * @param text
	 * @return
	 */
	private List<QnAPair> getQuestionsShort(List<OpenAiChatMessage> instructions, String shortText) {

		List<OpenAiChatMessage> prompt = new ArrayList<>(instructions);
		prompt.add(new OpenAiChatMessage(Role.USER, "Context:\n'''\n" //
				+ shortText //
				+ "\n'''"));

		String json = completionService.complete(prompt).getText();
		QnAPair[] result = new QnAPair[0];
		try {
			result = OpenAiClient.getJsonMapper().readValue(json, QnAPair[].class);
		} catch (JsonProcessingException e) {
			// Sometimes we have a single value
			try {
				result = new QnAPair[] { DirectOpenAiClient.getJsonMapper().readValue(json, QnAPair.class) };
			} catch (JsonProcessingException e1) {
				LOG.warn("Malformed JSON when parsing QnAPair[]", e);
			}
		}
		for (QnAPair r : result) {
			r.getContext().add(shortText);
		}

		return Arrays.asList(result);
	}

	@Override
	public void close() {
	}
}
