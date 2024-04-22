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

import io.github.mzattera.predictivepowers.huggingface.client.HuggingFaceEndpoint;
import io.github.mzattera.predictivepowers.huggingface.client.Options;
import io.github.mzattera.predictivepowers.huggingface.client.nlp.ConversationalRequest;
import io.github.mzattera.predictivepowers.huggingface.client.nlp.ConversationalResponse;
import io.github.mzattera.predictivepowers.services.AbstractChatService;
import io.github.mzattera.predictivepowers.services.ModelService.Tokenizer;
import io.github.mzattera.predictivepowers.services.messages.ChatCompletion;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage.Author;
import io.github.mzattera.predictivepowers.services.messages.FinishReason;
import io.github.mzattera.util.CharTokenizer;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Hugging Face based chat service.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class HuggingFaceChatService extends AbstractChatService {

	public static final String DEFAULT_MODEL = "mistralai/Mistral-7B-Instruct-v0.1";

	public HuggingFaceChatService(HuggingFaceEndpoint ep) {
		this(ep, ConversationalRequest.builder().options(Options.builder().waitForModel(true).build()).build());
	}

	public HuggingFaceChatService(HuggingFaceEndpoint ep, ConversationalRequest defaultReq) {
		this.endpoint = ep;
		this.defaultReq = defaultReq;
		this.modelService = endpoint.getModelService();
		setMaxHistoryLength(100);
	}

	@NonNull
	@Getter
	protected final HuggingFaceEndpoint endpoint;

	/**
	 * This request, with its parameters, is used as default setting for each call.
	 * 
	 * You can change any parameter to change these defaults (e.g. the model used)
	 * and the change will apply to all subsequent calls.
	 * 
	 * Getters and setters that change servie parameters, also change this object
	 * fields.
	 */
	@Getter
	@NonNull
	private final ConversationalRequest defaultReq;

	@Getter
	@Setter
	private String model = DEFAULT_MODEL;

	private final HuggingFaceModelService modelService;

	// In HF service, chat history is two synchronized arrays of user utterances and
	// bot responses
	private final List<String> userMsgHistory = new ArrayList<>();
	private final List<String> botMsgHistory = new ArrayList<>();

	@Override
	public List<ChatMessage> getHistory() {
		List<ChatMessage> result = new ArrayList<>(userMsgHistory.size());
		for (int i = 0; i < userMsgHistory.size(); ++i) {
			result.add(new ChatMessage(Author.USER, userMsgHistory.get(i)));
			result.add(new ChatMessage(Author.BOT, botMsgHistory.get(i)));
		}
		return result;
	}

	@Override
	public void clearConversation() {
		userMsgHistory.clear();
		botMsgHistory.clear();
	}

	@Override
	public Integer getTopK() {
		return defaultReq.getParameters().getTopK();
	}

	@Override
	public void setTopK(Integer topK) {
		defaultReq.getParameters().setTopK(topK);
	}

	@Override
	public Double getTopP() {
		return defaultReq.getParameters().getTopP();
	}

	@Override
	public void setTopP(Double topP) {
		defaultReq.getParameters().setTopP(topP);
	}

	@Override
	public Double getTemperature() {
		return defaultReq.getParameters().getTemperature();
	}

	@Override
	public void setTemperature(Double temperature) {
		defaultReq.getParameters().setTemperature(temperature);
	}

	@Override
	public Integer getMaxNewTokens() {
		return defaultReq.getParameters().getMaxLength();
	}

	@Override
	public void setMaxNewTokens(Integer maxNewTokens) {
		defaultReq.getParameters().setMaxLength(maxNewTokens);
	}

	@Override
	public ChatCompletion chat(String msg) {
		return chat(msg, defaultReq);
	}

	/**
	 * Continues current chat, with the provided message.
	 * 
	 * The exchange is added to the conversation history.
	 */
	public ChatCompletion chat(String msg, ConversationalRequest req) {

		ChatCompletion resp = chatCompletion(msg, trimConversation(msg), req);
		userMsgHistory.add(msg);
		botMsgHistory.add(resp.getText());

		// Make sure history is of desired length
		// TODO Make it more efficient
		int toTrim = userMsgHistory.size() - (getMaxHistoryLength() / 2);
		if (toTrim > 0) {
			userMsgHistory.subList(toTrim, userMsgHistory.size()).clear();
			botMsgHistory.subList(toTrim, botMsgHistory.size()).clear();
		}

		return resp;
	}

	@Override
	public ChatCompletion chat(ChatMessage msg) {
		return chat(msg, defaultReq);
	}

	/**
	 * Continues current chat, with the provided message.
	 * 
	 * The exchange is added to the conversation history.
	 */
	public ChatCompletion chat(ChatMessage msg, ConversationalRequest req) {
		if (!msg.isText())
			throw new IllegalArgumentException("This service supports only pure text messages.");

		return chat(msg.getContent(), req);
	}

	@Override
	public ChatCompletion complete(String prompt) {
		return complete(prompt, defaultReq);
	}

	/**
	 * Completes text (executes given prompt).
	 * 
	 * Notice this does not consider or affects chat history but agent personality
	 * is used, if provided.
	 */
	public ChatCompletion complete(String prompt, ConversationalRequest req) {
		return chatCompletion(prompt, trimConversation(prompt, new ArrayList<>(), new ArrayList<>()), req);
	}

	@Override
	public ChatCompletion complete(ChatMessage prompt) {
		return complete(prompt, defaultReq);
	}

	/**
	 * Completes text (executes given prompt).
	 * 
	 * Notice this does not consider or affects chat history but agent personality
	 * is used, if provided.
	 */
	public ChatCompletion complete(ChatMessage prompt, ConversationalRequest req) {
		if (!prompt.isText())
			throw new IllegalArgumentException("This service supports only pure text messages.");

		return complete(prompt.getContent(), req);
	}

	/**
	 * Completes given conversation.
	 * 
	 * Notice this does not consider or affects chat history. In addition, agent
	 * personality is NOT considered.
	 * 
	 * @param msg     Last user message, to be replied to.
	 * @param history Two Lists with user utterances and corresponding bot replies,
	 *                respectively.
	 */
	private ChatCompletion chatCompletion(String msg, List<String>[] history, ConversationalRequest req) {

		int hSize = history[0].size();
		if (hSize != history[1].size())
			throw new IllegalArgumentException("Conversation history must have a reply for each user message");
		req.getInputs().setPastUserInputs(hSize == 0 ? null : history[0]);
		req.getInputs().setGeneratedResponses(hSize == 0 ? null : history[1]);
		req.getInputs().setText(msg);

		ConversationalResponse resp = endpoint.getClient().conversational(model, req);

		return new ChatCompletion(FinishReason.COMPLETED, new ChatMessage(Author.BOT, resp.getGeneratedText()));
	}

	/**
	 * 
	 * @param msg Last message (not yet in history).
	 * @return Trimmed version of conversation history considering context
	 *         limitations set in this instance.
	 */
	private List<String>[] trimConversation(String msg) {
		return trimConversation(msg, userMsgHistory, botMsgHistory);
	}

	/**
	 * 
	 * @param msg         Last message (not yet in history).
	 * @param userHistory History of user messages.
	 * @param botHistory  History of bot messages.
	 * @return Trimmed version of history passed to the call, considering context
	 *         limitations set in this instance.
	 */
	private List<String>[] trimConversation(String msg, List<String> userHistory, List<String> botHistory) {

		// In some cases, we do not get the proper tokenizer, therefore we fall back to
		// counting chars
		Tokenizer counter = modelService.getTokenizer(model, CharTokenizer.getInstance());

		int tok = counter.count(msg);
		if (tok > getMaxConversationTokens())
			throw new IllegalArgumentException("Last message size is " + tok
					+ " and getMaxConversationTokens() returns " + getMaxConversationTokens());

		int steps = 0;
		for (int i = userHistory.size() - 1; i >= 0; --i) {
			if (steps >= getMaxConversationSteps())
				break;

			tok += counter.count(userHistory.get(i)) + counter.count(botHistory.get(i));
			if (tok >= getMaxConversationTokens())
				break;

			++steps;
		}
		// Notice last message will be considered anyway

		@SuppressWarnings("unchecked")
		List<String>[] result = new ArrayList[2];
		if (steps == 0) {
			result[0] = new ArrayList<>();
			result[1] = new ArrayList<>();
		} else if (steps < userHistory.size()) {
			result[0] = new ArrayList<>(userHistory.subList(userHistory.size() - steps, userHistory.size()));
			result[1] = new ArrayList<>(botHistory.subList(botHistory.size() - steps, botHistory.size()));
		} else {
			result[0] = new ArrayList<>(userHistory);
			result[1] = new ArrayList<>(botHistory);
		}

		return result;
	}
}