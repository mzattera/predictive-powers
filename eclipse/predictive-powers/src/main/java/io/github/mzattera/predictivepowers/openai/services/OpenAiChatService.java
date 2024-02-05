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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mzattera.predictivepowers.openai.client.OpenAiException;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsChoice;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsResponse;
import io.github.mzattera.predictivepowers.openai.client.chat.Function;
import io.github.mzattera.predictivepowers.openai.client.chat.FunctionCall;
import io.github.mzattera.predictivepowers.openai.client.chat.OpenAiTool;
import io.github.mzattera.predictivepowers.openai.client.chat.OpenAiToolCall;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatMessage.Role;
import io.github.mzattera.predictivepowers.services.AbstractChatService;
import io.github.mzattera.predictivepowers.services.Agent;
import io.github.mzattera.predictivepowers.services.Tool;
import io.github.mzattera.predictivepowers.services.ToolInitializationException;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage;
import io.github.mzattera.predictivepowers.services.messages.FinishReason;
import io.github.mzattera.predictivepowers.services.messages.ToolCallResult;
import lombok.Getter;
import lombok.NonNull;

/**
 * OpenAI chat service.
 * 
 * The service supports both function and tool calls transparently (it will
 * handle either, based on which model is used). As for agents, a list of tools
 * that will be used in each interaction with the service can be provided.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class OpenAiChatService extends AbstractChatService implements Agent {

	// TODO URGENT Add methods ot handle files in messages here (for vision as URL)

	// TODO add "slot filling" capabilities: fill a slot in the prompt based on
	// values from a Map this is done partially

	private final static Logger LOG = LoggerFactory.getLogger(OpenAiChatService.class);

	public static final String DEFAULT_MODEL = "gpt-4";

	public OpenAiChatService(OpenAiEndpoint ep) {
		this(ep, ChatCompletionsRequest.builder().model(DEFAULT_MODEL).build());
	}

	public OpenAiChatService(OpenAiEndpoint ep, ChatCompletionsRequest defaultReq) {
		this.endpoint = ep;
		this.defaultReq = defaultReq;
		modelService = endpoint.getModelService();
		String model = defaultReq.getModel();

		// With GPT you pay max tokens, even if they are not generated, so we put a
		// reasonable limit here
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

	@Override
	public String getId() {
		return "OpenAI-chat-API-model-" + getModel();
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
	public List<OpenAiChatMessage> getHistory() {
		return Collections.unmodifiableList(history);
	}

	/** For testing purposes only. This is not meant to be used. */
	List<OpenAiChatMessage> getModifiableHistory() {
		return history;
	}

	@Override
	public void clearConversation() {
		history.clear();
	}

	// TODO URGENT add tests to check all the methods to manipulate tools

	private final Map<String, OpenAiTool> tools = new HashMap<>();

	public List<OpenAiTool> getTools() {
		return Collections.unmodifiableList(new ArrayList<>(tools.values()));
	}

	public void setTools(Collection<? extends Tool> list) throws ToolInitializationException {

		tools.clear();
		if (list != null) {
			try {
				addTools(list);
			} catch (ToolInitializationException e) {
				tools.clear();
				throw e;
			}
		}

		setDefaultTools();
	}

	public void addTool(@NonNull Tool tool) throws ToolInitializationException {

		// TODO? Re-enable? it has been disable for easier tests
//		if (tools.containsKey(tool.getId()))
//			throw new ToolInitializationException("Duplicated tool: " + tool.getId());

		tool.init(this);
		try {
			tools.put(tool.getId(), (OpenAiTool) tool);
		} catch (ClassCastException e) {
			// Wrap the Tool into an OpenAiTool, so we can use any tool with function calls
			tools.put(tool.getId(), new OpenAiTool(tool));
		}
		setDefaultTools();
	}

	@Override
	public void addTools(Collection<? extends Tool> tools) throws ToolInitializationException {

		if (tools == null)
			return;
		for (Tool tool : tools)
			addTool(tool);
	}

	@Override
	public OpenAiTool removeTool(@NonNull String id) {

		OpenAiTool result = tools.remove(id);
		if (result != null)
			setDefaultTools();
		return result;
	}

	@Override
	public OpenAiTool removeTool(Tool tool) {
		return removeTool(tool.getId());
	}

	@Override
	public void clearTools() {
		tools.clear();
		setDefaultTools();
	}

	/**
	 * Set the tools to be used in all subsequent request. This sets tools or
	 * functions fields in defaultReq properly, taking automatically in
	 * consideration whether the model support functions or tool calls.
	 * 
	 * This is easier than setting tools or functions fields in defaultReq directly.
	 * 
	 * @throws UnsupportedOperationException if the model does not support function
	 *                                       calls.
	 */
	private void setDefaultTools() {

		if (tools.size() == 0) { // No tools / functions used
			defaultReq.setTools(null);
			defaultReq.setFunctions(null);
			return;
		}

		switch (modelService.getSupportedCall(getModel())) {
		case FUNCTIONS:
			List<Function> f = new ArrayList<>(tools.size());
			for (OpenAiTool t : tools.values()) {
				if (t.getType() != OpenAiTool.Type.FUNCTION) // paranoid, but will support future tools
					throw new UnsupportedOperationException("Current model supports only old funtion calling API.");
				f.add(t.getFunction());
			}
			defaultReq.setFunctions(f);
			defaultReq.setTools(null);
			break;
		case TOOLS:
			defaultReq.setTools(new ArrayList<>(tools.values()));
			defaultReq.setFunctions(null);
			break;
		default:
			throw new UnsupportedOperationException("Current model does not support function calling.");
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

	/**
	 * This method counts number of tokens that are consumed at each request to
	 * provide bot instructions (personality) and list tools it can use. This is the
	 * minimum size each request to the API will take. In addition, each request
	 * will consume the tokens needed to encode messages, which include tool calls
	 * and their corresponding replies.
	 * 
	 * @return Number of tokens in the request including bot personality and tools
	 *         (functions) descriptions, but excluding any other message.
	 */
	public int getBaseTokens() {
		List<OpenAiChatMessage> old = defaultReq.getMessages();

		List<OpenAiChatMessage> msgs = new ArrayList<>();
		if (getPersonality() != null) {
			msgs.add(new OpenAiChatMessage(Role.SYSTEM, getPersonality()));
		}
		defaultReq.setMessages(msgs);
		int result = modelService.getTokenizer(getModel()).count(defaultReq);
		defaultReq.setMessages(old);

		return result;
	}

	@Override
	public OpenAiChatCompletion chat(String msg) {
		return chat(msg, defaultReq);
	}

	/**
	 * Continues current chat, with the provided message.
	 * 
	 * The exchange is added to the conversation history.
	 */
	public OpenAiChatCompletion chat(String msg, ChatCompletionsRequest req) {
		return chat(new OpenAiChatMessage(Role.USER, msg), req);
	}

	@Override
	public OpenAiChatCompletion chat(ChatMessage msg) {
		return chat(msg, defaultReq);
	}

	/**
	 * Continues current chat, with the provided message.
	 * 
	 * The exchange is added to the conversation history.
	 */
	public OpenAiChatCompletion chat(ChatMessage msg, ChatCompletionsRequest req) {

		OpenAiChatMessage m;
		try {
			m = (OpenAiChatMessage) msg;
		} catch (ClassCastException e) { // Paranoid, in reality should never happen if used properly
			m = new OpenAiChatMessage(msg);
		}

		List<OpenAiChatMessage> conversation = new ArrayList<>(history);
		conversation.add(m);
		trimConversation(conversation);

		OpenAiChatCompletion result = chatCompletion(conversation, req);

		history.add(m);
		history.add((OpenAiChatMessage) result.getMessage());

		// Make sure history is of desired length
		int toTrim = history.size() - getMaxHistoryLength();
		if (toTrim > 0)
			history.subList(0, toTrim).clear();

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
	public OpenAiChatCompletion chat(List<? extends ToolCallResult> results) {
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
	public OpenAiChatCompletion chat(List<? extends ToolCallResult> results, ChatCompletionsRequest req) {

		List<OpenAiChatMessage> conversation = new ArrayList<>(history);

		OpenAiChatCompletion completion;

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
			history.add((OpenAiChatMessage) completion.getMessage());

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
			history.add((OpenAiChatMessage) completion.getMessage());

			break;
		default:
			throw new IllegalArgumentException("Current model does not support function calling.");
		}

		// Make sure history is of desired length
		int toTrim = history.size() - getMaxHistoryLength();
		if (toTrim > 0)
			history.subList(toTrim, history.size()).clear();

		return completion;
	}

	@Override
	public OpenAiChatCompletion complete(String prompt) {
		return complete(prompt, defaultReq);
	}

	/**
	 * Completes text (executes given prompt).
	 * 
	 * Notice this does not consider or affects chat history but agent personality
	 * is used, if provided.
	 */
	public OpenAiChatCompletion complete(String prompt, ChatCompletionsRequest req) {
		return complete(new OpenAiChatMessage(Role.USER, prompt), req);
	}

	@Override
	public OpenAiChatCompletion complete(ChatMessage prompt) {
		return complete(prompt, defaultReq);
	}

	/**
	 * Completes text outside a conversation (executes given prompt).
	 * 
	 * Notice this does not consider or affects chat history but agent personality
	 * is used, if provided.
	 */
	public OpenAiChatCompletion complete(ChatMessage prompt, ChatCompletionsRequest req) {
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
	 * without modifications. This means bot personality is NOT added on top of the
	 * list of messages, nor the list is checked or trimmed for excessive length.
	 */
	public OpenAiChatCompletion complete(List<OpenAiChatMessage> messages) {
		return complete(messages, defaultReq);
	}

	/**
	 * Completes given conversation.
	 * 
	 * Notice the list of messages is supposed to be passed as-is to the chat API,
	 * without modifications. This means bot personality is NOT added on top of the
	 * list of messages, nor the list is checked or trimmed for excessive length.
	 */
	public OpenAiChatCompletion complete(List<OpenAiChatMessage> messages, ChatCompletionsRequest req) {
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
	private OpenAiChatCompletion chatCompletion(List<OpenAiChatMessage> messages, ChatCompletionsRequest req) {

		validate(messages);

		String model = req.getModel();

		req.setMessages(messages);

		boolean autofit = (req.getMaxTokens() == null) && (modelService.getContextSize(model, -1) != -1);

		try {
			if (autofit) {
				// Automatically set token limit, if needed
				int tok = modelService.getTokenizer(model).count(req);
				int size = modelService.getContextSize(model) - tok - 5; // Paranoid :) count is now exact
				if (size <= 0)
					throw new IllegalArgumentException("Requests size (" + tok + ") exceeds model context size ("
							+ modelService.getContextSize(model) + ")");
				req.setMaxTokens(Math.min(size, modelService.getMaxNewTokens(model)));
			}

			ChatCompletionsResponse resp = null;
			try {
				resp = endpoint.getClient().createChatCompletion(req);
			} catch (OpenAiException e) {
				if (e.isContextLengthExceeded()) { // Automatically recover if request is too long
					int optimal = e.getMaxContextLength() - e.getPromptLength() - 1;
					if (optimal > 0) {
						// TODO URGENT Add a test case
						LOG.warn("Reducing context length for OpenAI chat service from " + req.getMaxTokens() + " to "
								+ optimal);
						int old = req.getMaxTokens();
						req.setMaxTokens(optimal);
						try {
							resp = endpoint.getClient().createChatCompletion(req);
						} finally {
							req.setMaxTokens(old);
						}
					} else
						throw e; // Context too small anyway
				} else
					throw e; // Not a context issue
			}

			ChatCompletionsChoice choice = resp.getChoices().get(0);
			if (choice.getMessage().getFunctionCall() != null) {

				// If the model returned a function call, transparently translate it into a tool
				// call
				List<OpenAiToolCall> calls = new ArrayList<>();
				FunctionCall funCall = choice.getMessage().getFunctionCall();
				OpenAiToolCall toolCall = new OpenAiToolCall(funCall);
				toolCall.setTool(tools.get(funCall.getName()));
				calls.add(toolCall);
				return new OpenAiChatCompletion(FinishReason.fromGptApi(choice.getFinishReason()), choice.getMessage(),
						calls);
			}
			if (choice.getMessage().getToolCalls() != null) {

				// Augment tool calls with corresponding OpenAITool
				for (OpenAiToolCall toolCall : choice.getMessage().getToolCalls()) {
					toolCall.setTool(tools.get(toolCall.getFunction().getName()));
				}
			}
			return new OpenAiChatCompletion(FinishReason.fromGptApi(choice.getFinishReason()), choice.getMessage(),
					choice.getMessage().getToolCalls());

		} finally {

			// for next call, or maxTokens will remain fixed
			if (autofit)
				req.setMaxTokens(null);
		}
	}

	/**
	 * Checks that
	 * 
	 * @param messages
	 */
	private void validate(List<OpenAiChatMessage> messages) {
		// TODO Auto-generated method stub

	}

	/**
	 * Trims given list of messages (typically a conversation history), so it fits
	 * the limits set in this instance (that is, maximum conversation steps and
	 * tokens).
	 * 
	 * Notice the personality is always and automatically added to the trimmed list
	 * (if set).
	 * 
	 * @throws IllegalArgumentException if no message can be added because of
	 *                                  context size limitations.
	 */
	private void trimConversation(List<OpenAiChatMessage> messages) {

		// Remove tool call results left on top without corresponding calls, or this
		// will cause HTTP 400 error
		// TODO urgent test if this fails with FUNCTION too
		// TODO URGENT add a test case
		int firstNonToolIndex = 0;
		for (OpenAiChatMessage m : messages) {
			if (m.getRole() == Role.TOOL) {
				firstNonToolIndex++;
			} else {
				break;
			}
		}
		if (firstNonToolIndex > 0) {
			messages.subList(0, firstNonToolIndex).clear();
			if (messages.size() == 0)
				throw new IllegalArgumentException(
						"Messages contain only tool call results without corresponding calls");
		}

		// Trims down the list of messages accordingly to given limits.
		OpenAiTokenizer counter = modelService.getTokenizer(getModel());
		int steps = 0;
		for (int i = messages.size() - 1; i >= 0; --i) {
			if (steps >= getMaxConversationSteps())
				break;

			int tok = counter.count(messages.subList(i, messages.size()));
			if (tok > getMaxConversationTokens()) {
				break;
			}

			++steps;
		}
		if (steps == 0)
			throw new IllegalArgumentException("Context to small to fit a single message");
		else if (steps < messages.size())
			messages.subList(0, messages.size() - steps).clear();

		if (getPersonality() != null)
			// must add a system message on top with personality
			messages.add(0, new OpenAiChatMessage(Role.SYSTEM, getPersonality()));
	}
}