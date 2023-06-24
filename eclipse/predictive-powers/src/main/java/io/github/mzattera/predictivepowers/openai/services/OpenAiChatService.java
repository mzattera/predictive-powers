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

import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsChoice;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsResponse;
import io.github.mzattera.predictivepowers.openai.client.chat.Function;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiTokenizer;
import io.github.mzattera.predictivepowers.services.AbstractChatService;
import io.github.mzattera.predictivepowers.services.ChatMessage;
import io.github.mzattera.predictivepowers.services.ChatMessage.Role;
import io.github.mzattera.predictivepowers.services.TextCompletion;
import lombok.Getter;
import lombok.NonNull;

/**
 * Hugging Face based chat service.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class OpenAiChatService extends AbstractChatService {

	// TODO add "slot filling" capabilities: fill a slot in the prompt based on
	// values from a Map

	// TODO switch back to 3.5 after On June 27th, 2023
//	public static final String DEFAULT_MODEL = "gpt-3.5-turbo";
	public static final String DEFAULT_MODEL = "gpt-3.5-turbo-0613";

	public OpenAiChatService(OpenAiEndpoint ep) {
		this(ep, ChatCompletionsRequest.builder().model(DEFAULT_MODEL).build());
	}

	public OpenAiChatService(OpenAiEndpoint ep, ChatCompletionsRequest defaultReq) {
		super(ep.getModelService());
		this.endpoint = ep;
		this.defaultReq = defaultReq;
		setMaxConversationTokens(Math.max(ep.getModelService().getContextSize(defaultReq.getModel()), 2046) * 3 / 4);
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

	@Override
	public Integer getMaxNewTokens() {
		return defaultReq.getMaxTokens();
	}

	@Override
	public void setMaxNewTokens(Integer maxNewTokens) {
		defaultReq.setMaxTokens(maxNewTokens);
	}

	@Override
	public OpenAiTextCompletion chat(String msg) {
		return chat(msg, defaultReq, null);
	}

	/**
	 * Continues current chat, with the provided message.
	 * 
	 * The exchange is added to the conversation history.
	 */
	public OpenAiTextCompletion chat(String msg, ChatCompletionsRequest req) {
		return chat(msg, req, null);
	}

	/**
	 * Continues current chat, with the provided message.
	 * 
	 * The exchange is added to the conversation history.
	 * 
	 * @param functions List of functions that can be called (possibly empty or
	 *                  null, to prevent function calls).
	 */
	public OpenAiTextCompletion chat(String msg, List<Function> functions) {
		return chat(msg, defaultReq, functions);
	}

	/**
	 * Continues current chat, with the provided message.
	 * 
	 * The exchange is added to the conversation history.
	 * 
	 * @param functions List of functions that can be called (possibly empty or
	 *                  null, to prevent function calls).
	 */
	public OpenAiTextCompletion chat(String msg, ChatCompletionsRequest req, List<Function> functions) {
		return chat(new ChatMessage(Role.USER, msg), req, functions);
	}

	@Override
	public OpenAiTextCompletion chat(ChatMessage msg) {
		return chat(msg, defaultReq, null);
	}

	/**
	 * Continues current chat, with the provided message.
	 * 
	 * The exchange is added to the conversation history.
	 */
	public OpenAiTextCompletion chat(ChatMessage msg, ChatCompletionsRequest req) {
		return chat(msg, defaultReq, null);
	}

	/**
	 * Continues current chat, with the provided message.
	 * 
	 * The exchange is added to the conversation history.
	 * 
	 * @param functions List of functions that can be called (possibly empty or
	 *                  null, to prevent function calls).
	 */
	public OpenAiTextCompletion chat(ChatMessage msg, List<Function> functions) {
		return chat(msg, defaultReq, functions);
	}

	/**
	 * Continues current chat, with the provided message.
	 * 
	 * The exchange is added to the conversation history.
	 * 
	 * @param functions List of functions that can be called (possibly empty or
	 *                  null, to prevent function calls).
	 */
	public OpenAiTextCompletion chat(ChatMessage msg, ChatCompletionsRequest req, List<Function> functions) {

		List<ChatMessage> conversation = trimChat(history, true);
		conversation.add(msg);

		ChatCompletionsChoice choice = chatCompletion(conversation, req, functions);
		OpenAiTextCompletion result = new OpenAiTextCompletion(choice.getMessage().getContent(),
				choice.getFinishReason(), choice.getMessage().getFunctionCall());

		history.add(msg);
		history.add(choice.getMessage());

		// Make sure history is of desired length
		while (history.size() > getMaxHistoryLength())
			history.remove(0);

		return result;
	}

	@Override
	public OpenAiTextCompletion complete(String prompt) {
		return complete(prompt, defaultReq, null);
	}

	/**
	 * Completes text (executes given prompt).
	 * 
	 * Notice this does not consider or affects chat history but agent personality
	 * is used, if provided.
	 */
	public OpenAiTextCompletion complete(String prompt, ChatCompletionsRequest req) {
		return complete(prompt, req, null);
	}

	/**
	 * Completes text (executes given prompt) allowing function calls.
	 * 
	 * Notice this does not consider or affects chat history but agent personality
	 * is used, if provided.
	 * 
	 * @param functions List of functions that can be called (possibly empty or
	 *                  null, to prevent function calls).
	 */
	public OpenAiTextCompletion complete(String prompt, List<Function> functions) {
		return complete(prompt, defaultReq, functions);
	}

	/**
	 * Completes text (executes given prompt).
	 * 
	 * Notice this does not consider or affects chat history but agent personality
	 * is used, if provided.
	 * 
	 * @param functions List of functions that can be called (possibly empty or
	 *                  null, to prevent function calls).
	 */
	public OpenAiTextCompletion complete(String prompt, ChatCompletionsRequest req, List<Function> functions) {
		return complete(new ChatMessage(ChatMessage.Role.USER, prompt), req, functions);
	}

	@Override
	public TextCompletion complete(ChatMessage prompt) {
		return complete(prompt, defaultReq, null);
	}

	/**
	 * Completes text outside a conversation (executes given prompt).
	 * 
	 * Notice this does not consider or affects chat history but agent personality
	 * is used, if provided.
	 */
	public OpenAiTextCompletion complete(ChatMessage prompt, ChatCompletionsRequest req) {
		return complete(prompt, req, null);
	}

	/**
	 * Completes text outside a conversation (executes given prompt).
	 * 
	 * Notice this does not consider or affects chat history but agent personality
	 * is used, if provided.
	 * 
	 * @param functions List of functions that can be called (possibly empty or
	 *                  null, to prevent function calls).
	 */
	public OpenAiTextCompletion complete(ChatMessage prompt, List<Function> functions) {
		return complete(prompt, defaultReq, functions);
	}

	/**
	 * Completes text outside a conversation (executes given prompt).
	 * 
	 * Notice this does not consider or affects chat history but agent personality
	 * is used, if provided.
	 * 
	 * 
	 * 
	 * @param functions List of functions that can be called (possibly empty or
	 *                  null, to prevent function calls).
	 */
	public OpenAiTextCompletion complete(ChatMessage prompt, ChatCompletionsRequest req, List<Function> functions) {
		List<ChatMessage> msgs = new ArrayList<>();
		if (getPersonality() != null)
			msgs.add(new ChatMessage(ChatMessage.Role.SYSTEM, getPersonality()));
		msgs.add(prompt);

		return complete(msgs, req, functions);
	}

	@Override
	public OpenAiTextCompletion complete(List<ChatMessage> messages) {
		return complete(messages, defaultReq, null);
	}

	/**
	 * Completes given conversation.
	 * 
	 * Notice this does not consider or affects chat history. In addition, agent
	 * personality is NOT considered, but can be injected as first message in the
	 * list.
	 */
	public OpenAiTextCompletion complete(List<ChatMessage> messages, ChatCompletionsRequest req) {
		return complete(messages, req, null);
	}

	/**
	 * Completes given conversation.
	 * 
	 * Notice this does not consider or affects chat history. In addition, agent
	 * personality is NOT considered, but can be injected as first message in the
	 * list.
	 * 
	 * @param functions List of functions that can be called (possibly empty or
	 *                  null, to prevent function calls).
	 */
	public OpenAiTextCompletion complete(List<ChatMessage> messages, List<Function> functions) {
		return complete(messages, defaultReq, null);
	}

	/**
	 * Completes given conversation.
	 * 
	 * Notice this does not consider or affects chat history. In addition, agent
	 * personality is NOT considered, but can be injected as first message in the
	 * list.
	 * 
	 * @param functions List of functions that can be called (possibly empty or
	 *                  null, to prevent function calls).
	 */
	public OpenAiTextCompletion complete(List<ChatMessage> messages, ChatCompletionsRequest req,
			List<Function> functions) {
		ChatCompletionsChoice choice = chatCompletion(messages, req, functions);
		return new OpenAiTextCompletion(choice.getMessage().getContent(), choice.getFinishReason(),
				choice.getMessage().getFunctionCall());
	}

	/**
	 * Completes given conversation.
	 * 
	 * Notice this does not consider or affects chat history. In addition, agent
	 * personality is NOT considered, but can be injected as first message in the
	 * list.
	 * 
	 * @param functions List of functions that can be called (possibly empty or
	 *                  null, to prevent function calls).
	 */
	protected ChatCompletionsChoice chatCompletion(List<ChatMessage> messages, ChatCompletionsRequest req,
			List<Function> functions) {

		String model = req.getModel();
		OpenAiTokenizer counter = (OpenAiTokenizer) modelService.getTokenizer(model);

		req.setMessages(messages);
		if ((functions != null) && (functions.size() > 0)) { // seems to cause an error if you set it otherwise
			req.setFunctions(functions);
		}

		// Adjust token limit if needed
		boolean autofit = (req.getMaxTokens() == null) && (modelService.getContextSize(model, -1) != -1);
		try {
			if (autofit) {
				int tok = counter.count(req); // Notice we must count function definitions too
				req.setMaxTokens(modelService.getContextSize(model) - tok - 5);
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