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

import io.github.mzattera.predictivepowers.TokenCounter;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsChoice;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsResponse;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.util.ModelUtil;
import io.github.mzattera.predictivepowers.services.AbstractChatService;
import io.github.mzattera.predictivepowers.services.ChatMessage;
import io.github.mzattera.predictivepowers.services.TextResponse;
import lombok.Getter;
import lombok.NonNull;

/**
 * This class manages a chat with an agent.
 * 
 * Chat history is kept in memory and updated as chat progresses. At the same
 * time, methods to use chat service as a simple completion service are
 * provided.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class OpenAiChatService extends AbstractChatService {

	// TODO add "slot filling" capabilities: fill a slot in the prompt based on
	// values from a Map

	public static final String DEFAULT_MODEL = "gpt-3.5-turbo";

	public OpenAiChatService(OpenAiEndpoint ep) {
		this(ep, ChatCompletionsRequest.builder().model(DEFAULT_MODEL).build());
	}

	public OpenAiChatService(OpenAiEndpoint ep, ChatCompletionsRequest defaultReq) {
		this.endpoint = ep;
		this.defaultReq = defaultReq;
		setMaxConversationTokens(Math.max(ModelUtil.getContextSize(defaultReq.getModel()), 2046) * 3 / 4);
	}

	@NonNull
	@Getter
	protected final OpenAiEndpoint endpoint;

	@Override
	public String getModel() {
		return defaultReq.getModel();
	}

	@Override
	public void setModel(@NonNull String model) {
		defaultReq.setModel(model);
	}

	/**
	 * This request, with its parameters, is used as default setting for each call.
	 * 
	 * You can change any parameter to change these defaults (e.g. the model used)
	 * and the change will apply to all subsequent calls.
	 */
	@Getter
	@NonNull
	private final ChatCompletionsRequest defaultReq;

	/**
	 * Continues current chat, with the provided message.
	 * 
	 * The exchange is added to the conversation history.
	 */
	@Override
	public TextResponse chat(String msg) {
		return chat(msg, defaultReq);
	}

	@Override
	public Integer getTopK() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setTopK(Integer topK) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Double getTopP() {
		return defaultReq.getTopP();
	}

	@Override
	public void setTopP(Double topP) {
		defaultReq.setTopP(topP);
	}

	@Override
	public Double getTemperature() {
		// Must scale from [0-2] to [0-100] considering default value as well
		if (defaultReq.getTemperature() == null)
			return null;
		return defaultReq.getTemperature() * 50;
	}

	@Override
	public void setTemperature(Double temperature) {
		// Must scale from [0-2] to [0-100] considering default value as well
		if (temperature == null)
			defaultReq.setTemperature(null);
		else
			defaultReq.setTemperature(temperature / 50);
	}

	/**
	 * This service will try to calculate this so to allow the longest possible
	 * output, if it is null.
	 */
	@Override
	public Integer getMaxNewTokens() {
		return defaultReq.getMaxTokens();
	}

	/**
	 * This service will try to calculate this so to allow the longest possible
	 * output, if it is null.
	 */
	@Override
	public void setMaxNewTokens(Integer maxNewTokens) {
		defaultReq.setMaxTokens(maxNewTokens);
	}

	@Override
	public int getN() {
		if (defaultReq.getN() == null)
			return 1;
		return defaultReq.getN();
	}

	@Override
	public void setN(int n) {
		defaultReq.setN(n);
	}

	/**
	 * Continues current chat, with the provided message.
	 * 
	 * The exchange is added to the conversation history.
	 */
	public TextResponse chat(String msg, ChatCompletionsRequest req) {

		// Update history
		history.add(new ChatMessage(ChatMessage.Role.USER, msg));

		try {

			ChatCompletionsChoice choice = chatCompletion(trimChat(history, ModelUtil.getTokenCounter(req.getModel())),
					req);
			TextResponse result = TextResponse.fromGptApi(choice.getMessage().getContent(), choice.getFinishReason());
			history.add(choice.getMessage());
			return result;

		} catch (Exception e) {
			// remove last message from history
			history.remove(history.size() - 1);

			throw e;
		} finally {
			// Make sure history is of desired length
			while (history.size() > getMaxHistoryLength())
				history.remove(0);
		}
	}

	/**
	 * Completes text (executes given prompt).
	 * 
	 * Notice this does not consider or affects chat history but agent personality
	 * is used, if provided.
	 */
	@Override
	public TextResponse complete(String prompt) {
		return complete(prompt, defaultReq);
	}

	/**
	 * Completes text (executes given prompt).
	 * 
	 * Notice this does not consider or affects chat history but agent personality
	 * is used, if provided.
	 */
	public TextResponse complete(String prompt, ChatCompletionsRequest req) {
		List<ChatMessage> msg = new ArrayList<>();
		if (getPersonality() != null)
			msg.add(new ChatMessage(ChatMessage.Role.SYSTEM, getPersonality()));
		msg.add(new ChatMessage(ChatMessage.Role.USER, prompt));

		return complete(msg, req);
	}

	/**
	 * Completes given conversation, using this service as a completion service.
	 * 
	 * Notice this does not consider or affects chat history. In addition, agent
	 * personality is NOT considered, but can be injected as first message in the
	 * list, for service that support this.
	 */
	@Override
	public TextResponse complete(List<ChatMessage> messages) {
		return complete(messages, defaultReq);
	}

	/**
	 * Completes given conversation.
	 * 
	 * Notice this does not consider or affects chat history. In addition, agent
	 * personality is NOT considered, but can be injected as first message in the
	 * list.
	 */
	public TextResponse complete(List<ChatMessage> messages, ChatCompletionsRequest req) {
		ChatCompletionsChoice choice = chatCompletion(messages, req);
		return TextResponse.fromGptApi(choice.getMessage().getContent(), choice.getFinishReason());
	}

	/**
	 * Completes given conversation.
	 * 
	 * Notice this does not consider or affects chat history. In addition, agent
	 * personality is NOT considered, but can be injected as first message in the
	 * list.
	 */
	private ChatCompletionsChoice chatCompletion(List<ChatMessage> messages, ChatCompletionsRequest req) {

		String model = req.getModel();
		TokenCounter counter = ModelUtil.getTokenCounter(model);

		req.setMessages(messages);

		// Adjust token limit if needed
		boolean autofit = (req.getMaxTokens() == null) && (ModelUtil.getContextSize(model) != -1);
		try {
			if (autofit) {
				int tok = counter.count(messages);
				req.setMaxTokens(ModelUtil.getContextSize(model) - tok - 10);
			}

			ChatCompletionsResponse resp = endpoint.getClient().createChatCompletion(req);
			return resp.getChoices().get(0);

		} finally {

			// for next call, or maxTokens will remain fixed
			if (autofit)
				req.setMaxTokens(null);
		}
	}
}