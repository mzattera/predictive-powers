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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mzattera.predictivepowers.openai.client.OpenAiException;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsChoice;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsResponse;
import io.github.mzattera.predictivepowers.openai.client.chat.Function;
import io.github.mzattera.predictivepowers.openai.client.chat.OpenAiTool;
import io.github.mzattera.predictivepowers.openai.client.chat.ToolCall;
import io.github.mzattera.predictivepowers.openai.client.chat.ToolCallResult;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatMessage.Role;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiTokenizer;
import io.github.mzattera.predictivepowers.services.AbstractChatService;
import io.github.mzattera.predictivepowers.services.ChatMessage;
import io.github.mzattera.predictivepowers.services.ModelService.Tokenizer;
import io.github.mzattera.predictivepowers.services.TextCompletion.FinishReason;
import lombok.Getter;
import lombok.NonNull;

/**
 * OpenAI based chat service.
 * 
 * The service supports bout function and tool calls transparently (it will
 * handle either, based on the model is used). The service has a list of default
 * tools that will be used in each interaction with the service and that can be
 * overridden for each single service call.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class OpenAiChatService extends AbstractChatService {

	private final static Logger LOG = LoggerFactory.getLogger(OpenAiChatService.class);

	// TODO add "slot filling" capabilities: fill a slot in the prompt based on
	// values from a Map this is done partially

	public static final String DEFAULT_MODEL = "gpt-4";

	public OpenAiChatService(OpenAiEndpoint ep) {
		this(ep, ChatCompletionsRequest.builder().model(DEFAULT_MODEL).build());
	}

	public OpenAiChatService(OpenAiEndpoint ep, ChatCompletionsRequest defaultReq) {
		this.endpoint = ep;
		this.defaultReq = defaultReq;
		modelService = endpoint.getModelService();
		String model = defaultReq.getModel();
		int maxReplyTk = Math.min(modelService.getContextSize(model) / 4, modelService.getMaxNewTokens(model));
		setMaxConversationTokens(modelService.getContextSize(model) - maxReplyTk);
	}

	@Getter
	@NonNull
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

	private final OpenAiModelService modelService;

	private final List<OpenAiChatMessage> history = new ArrayList<>();

	@Override
	public List<ChatMessage> getHistory() {
		return Collections.unmodifiableList(
				history.stream().map(chatMessage -> (ChatMessage) chatMessage).collect(Collectors.toList()));
	}

	/** This is used for testing purposes only */
	List<OpenAiChatMessage> getModifiableHistory() {
		return history;
	}

	@Override
	public void clearConversation() {
		history.clear();
	}

	/**
	 * Set the tools to be used in all subsequent request. This sets tools or
	 * functions fields in defaultReq properly, taking automatically in
	 * consideration whether the model support functions or tool calls.
	 * 
	 * This is easier than setting tools or functions fields in defaultReq directly.
	 * single or parallel (tools) function calls.
	 * 
	 * @param tools List of tools to be used in all subsequent calls of this
	 *              service.
	 * 
	 * @throws IllegalArgumentException if the model does not support function
	 *                                  calls.
	 */
	public void setDefaulTools(List<OpenAiTool> tools) {

		if ((tools == null) || (tools.size() == 0)) { // No tools / functions used
			defaultReq.setTools(null);
			defaultReq.setFunctions(null);
			return;
		}

		switch (((OpenAiModelService) modelService).getSupportedCall(getModel())) {
		case FUNCTIONS:
			List<Function> f = new ArrayList<>(tools.size());
			for (OpenAiTool t : tools) {
				if (t.getType() != OpenAiTool.Type.FUNCTION) // paranoid, but will support future tools
					throw new IllegalArgumentException("Current model does only support old funtion calling API.");
				f.add(t.getFunction());
			}
			defaultReq.setFunctions(f);
			defaultReq.setTools(null);
			break;
		case TOOLS:
			defaultReq.setTools(tools);
			defaultReq.setFunctions(null);
			break;
		default:
			throw new IllegalArgumentException("Current model does not support function calling.");
		}
	}

	/**
	 * Gets the tools to used in all subsequent calls to this service
	 * 
	 * @return Content of tools or functions fields in defaultReq, taking
	 *         automatically in consideration whether the model support functions.
	 * 
	 *         This is easier than accessing tools or functions fields in defaultReq
	 *         directly.
	 * 
	 * @throws IllegalArgumentException if the model does not support function
	 *                                  calls.
	 */
	public List<OpenAiTool> getDefaulTools() {

		switch (modelService.getSupportedCall(getModel())) {
		case FUNCTIONS:
			if (defaultReq.getFunctions() == null)
				return null;
			List<OpenAiTool> result = new ArrayList<>(defaultReq.getFunctions().size());
			for (Function t : defaultReq.getFunctions()) {
				result.add(new OpenAiTool(t));
			}
			return result;
		case TOOLS:
			if (defaultReq.getTools() == null)
				return null;
			return defaultReq.getTools();
		default:
			throw new IllegalArgumentException("Current model does not support function calling.");
		}
	}

	@Override
	public Integer getTopK() {
		return null;
	}

	@Override
	public void setTopK(Integer topK) {
		if (topK != null)
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
		return chat(msg, defaultReq);
	}

	/**
	 * Continues current chat, with the provided message.
	 * 
	 * The exchange is added to the conversation history.
	 */
	public OpenAiTextCompletion chat(String msg, ChatCompletionsRequest req) {
		return chat(new OpenAiChatMessage(Role.USER, msg), req);
	}

	@Override
	public OpenAiTextCompletion chat(ChatMessage msg) {
		return chat(msg, defaultReq);
	}

	/**
	 * Continues current chat, with the provided message.
	 * 
	 * The exchange is added to the conversation history.
	 */
	public OpenAiTextCompletion chat(ChatMessage msg, ChatCompletionsRequest req) {

		OpenAiChatMessage m;
		try {
			m = (OpenAiChatMessage) msg;
		} catch (ClassCastException e) { // Paranoid, in reality should never happen if used properly
			m = new OpenAiChatMessage(msg);
		}

		List<OpenAiChatMessage> conversation = new ArrayList<>(history);
		conversation.add(m);
		trimConversation(conversation);

		OpenAiTextCompletion result = chatCompletion(conversation, req);

		history.add(m);
		history.add(result.getMessage());

		// Make sure history is of desired length
		while (history.size() > getMaxHistoryLength())
			history.remove(0);

		return result;
	}

	/**
	 * Continues current chat, by returning results from tool calls to the OpenAI
	 * API. This is invoked after the model generated tool call(s), proper tools
	 * were invoked and now their results are ready to be returned to the model.
	 * Note that the service treats (old) function call API and (new) tool call API
	 * the same, translating function calls into tool calls transparently.
	 * 
	 * @param results the list of results from various call.
	 */
	public OpenAiTextCompletion chat(List<ToolCallResult> results) {
		return chat(results, defaultReq);
	}

	/**
	 * Continues current chat, by returning results from tool calls to the OpenAI
	 * API. This is invoked after the model generated tool call(s), proper tools
	 * were invoked and now their results are ready to be returned to the model.
	 * Note that the service treats (old) function call API and (new) tool call API
	 * the same, translating function calls into tool calls transparently.
	 * 
	 * @param results the list of results from various call.
	 */
	public OpenAiTextCompletion chat(List<ToolCallResult> results, ChatCompletionsRequest req) {

		List<OpenAiChatMessage> conversation = new ArrayList<>(history);

		OpenAiTextCompletion completion;

		// Transparently handles function and tool calls
		switch (((OpenAiModelService) modelService).getSupportedCall(req.getModel())) {
		case FUNCTIONS:
			if (results.size() != 1)
				throw new IllegalArgumentException("Current model supports only single function calls.");

			OpenAiChatMessage msg = new OpenAiChatMessage(Role.FUNCTION, results.get(0));
			conversation.add(msg);
			trimConversation(conversation);
			completion = chatCompletion(conversation, req);

			history.add(msg);
			history.add(completion.getMessage());

			break;
		case TOOLS:

			List<OpenAiChatMessage> msgs = new ArrayList<>();
			for (ToolCallResult result : results) {
				msgs.add(new OpenAiChatMessage(Role.TOOL, result));
			}
			conversation.addAll(msgs);
			trimConversation(conversation);
			completion = chatCompletion(conversation, req);

			history.addAll(msgs);
			history.add(completion.getMessage());

			break;
		default:
			throw new IllegalArgumentException("Current model does not support function calling.");
		}

		// Make sure history is of desired length
		while (history.size() > getMaxHistoryLength())
			history.remove(0);

		return OpenAiTextCompletion.builder() //
				.message(completion.getMessage()) //
				.finishReason(completion.getFinishReason()) //
				.toolCalls(completion.getToolCalls()).build();
	}

	@Override
	public OpenAiTextCompletion complete(String prompt) {
		return complete(prompt, defaultReq);
	}

	/**
	 * Completes text (executes given prompt).
	 * 
	 * Notice this does not consider or affects chat history but agent personality
	 * is used, if provided.
	 */
	public OpenAiTextCompletion complete(String prompt, ChatCompletionsRequest req) {
		return complete(new OpenAiChatMessage(Role.USER, prompt), req);
	}

	@Override
	public OpenAiTextCompletion complete(ChatMessage prompt) {
		return complete(prompt, defaultReq);
	}

	/**
	 * Completes text outside a conversation (executes given prompt).
	 * 
	 * Notice this does not consider or affects chat history but agent personality
	 * is used, if provided.
	 */
	public OpenAiTextCompletion complete(ChatMessage prompt, ChatCompletionsRequest req) {
		List<OpenAiChatMessage> msgs = new ArrayList<>();

		OpenAiChatMessage m;
		try {
			m = (OpenAiChatMessage) prompt;
		} catch (ClassCastException e) { // Paranoid, in reality should never happen if used properly
			m = new OpenAiChatMessage(prompt);
		}
		msgs.add(m);
		trimConversation(msgs);

		return complete(msgs, req);
	}

	/**
	 * Completes text outside a conversation (executes given prompt ignoring and
	 * without affecting conversation history).
	 * 
	 * Notice the list of messages is supposed to be passed as-is to the chat API,
	 * without modifications.
	 */
	public OpenAiTextCompletion complete(List<OpenAiChatMessage> messages) {
		return complete(messages, defaultReq);
	}

	/**
	 * Completes given conversation.
	 * 
	 * Notice this does not consider or affects chat history. In addition, agent
	 * personality is NOT considered, but can be injected as first message in the
	 * list.
	 */
	public OpenAiTextCompletion complete(List<OpenAiChatMessage> messages, ChatCompletionsRequest req) {
		return chatCompletion(messages, req);
	}

	/**
	 * Completes given conversation.
	 * 
	 * Notice this does not consider or affects chat history. In addition, agent
	 * personality is NOT considered, but can be injected as first message in the
	 * list.
	 * 
	 * @param tools List of tools that can be called (this can be empty to prevent
	 *              tool calls, or null to use the list of default tools).
	 */
	private OpenAiTextCompletion chatCompletion(List<OpenAiChatMessage> messages, ChatCompletionsRequest req) {

		String model = req.getModel();

		req.setMessages(messages);

		boolean autofit = (req.getMaxTokens() == null) && (modelService.getContextSize(model, -1) != -1);

		try {
			if (autofit) {
				// Automatically set token limit, if needed
				OpenAiTokenizer counter = (OpenAiTokenizer) modelService.getTokenizer(model);
				int tok = counter.count(req); // Notice we must count function definitions too
				int size = modelService.getContextSize(model) - tok - 5;
				if (size <= 0)
					throw new IllegalArgumentException(
							"Your prompt exceeds context size: " + modelService.getContextSize(model));
				req.setMaxTokens(Math.min(size, modelService.getMaxNewTokens(model)));
			}

			ChatCompletionsResponse resp = null;
			try {
				resp = endpoint.getClient().createChatCompletion(req);
			} catch (OpenAiException e) {
				if (e.isContextLengthExceeded()) { // Automatically recover if request is too long
					int optimal = e.getMaxContextLength() - e.getPromptLength() - 1;
					if (optimal > 0) {
						LOG.warn("Reducing context length for OpenAI chat service from " + req.getMaxTokens() + " to "
								+ optimal);
						req.setMaxTokens(optimal);
						resp = endpoint.getClient().createChatCompletion(req);
						// TODO re-set old value?
					} else
						throw e; // Context too small anyway
				} else
					throw e; // Not a context issue
			}

			ChatCompletionsChoice choice = resp.getChoices().get(0);
			if (choice.getMessage().getFunctionCall() != null) {

				// If the model returned a function call, transparently translate it into a tool
				// call
				List<ToolCall> fc = new ArrayList<>();
				fc.add(new ToolCall(choice.getMessage().getFunctionCall()));
				return OpenAiTextCompletion.builder() //
						.finishReason(FinishReason.fromGptApi(choice.getFinishReason())) //
						.message(choice.getMessage()) //
						.toolCalls(fc).build();
			}
			return OpenAiTextCompletion.builder() //
					.finishReason(FinishReason.fromGptApi(choice.getFinishReason())) //
					.message(choice.getMessage()) //
					.toolCalls(choice.getMessage().getToolCalls()).build();

		} finally {

			// for next call, or maxTokens will remain fixed
			if (autofit)
				req.setMaxTokens(null);
		}
	}

	/**
	 * Trims given list of messages (typically a conversation history), so it fits
	 * the limits set in this instance (that is, maximum conversation steps and
	 * tokens).
	 * 
	 * Notice the personality is always added to the trimmed list (if set).
	 * 
	 * @throws IllegalArgumentException if no message can be added because of
	 *                                  context size limitations.
	 */
	private void trimConversation(List<OpenAiChatMessage> messages) {

		// Remove tool call results left on top without corresponding calls, or this
		// will cause HTTP 400 error
		// TODO make this more efficient?
		while (messages.size() > 0) {
			OpenAiChatMessage m = messages.get(0);
			if (m.getRole() == Role.TOOL)
				messages.remove(0);
			else
				break;
		}
		if (messages.size() == 0)
			throw new IllegalArgumentException("Conversation history contains only tool call results");

		// TODO URGENT: Better counting of tokens

		if (getPersonality() != null) {

			// must add a system message on top with personality
			OpenAiChatMessage sys = new OpenAiChatMessage(Role.SYSTEM, getPersonality());
			int count = modelService.getTokenizer(getModel()).count(sys);
			if (count >= getMaxConversationTokens())
				throw new IllegalArgumentException("Context to small: bot personality alone is " + count
						+ " tokens and getMaxConversationTokens() returns " + getMaxConversationTokens());
			trimConversation(messages, getMaxConversationSteps(), getMaxConversationTokens() - count);
			if (messages.size() == 0)
				throw new IllegalArgumentException("Context to small to fit last conversation message");
			messages.add(0, sys);
		} else {
			trimConversation(messages, getMaxConversationSteps(), getMaxConversationTokens());
			if (messages.size() == 0)
				throw new IllegalArgumentException("Context to small to fit last conversation message");
		}
	}

	private void trimConversation(List<OpenAiChatMessage> messages, int maxConversationSteps,
			int maxConversationTokens) {

		Tokenizer counter = modelService.getTokenizer(getModel());
		int tok = 0;
		int steps = 0;

		for (int i = messages.size() - 1; i >= 0; --i) {
			if (steps >= maxConversationSteps)
				break;

			// TODO urgent: it is not the correct way to count
			tok += counter.count(messages.get(i));

			if (tok > maxConversationTokens) {
				break;
			}

			++steps;
		}

		if (steps == 0)
			messages.clear();
		else if (steps < messages.size())
			messages.subList(0, messages.size()-steps).clear();
	}
}