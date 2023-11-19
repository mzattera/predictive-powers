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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mzattera.predictivepowers.openai.client.OpenAiException;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsChoice;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsResponse;
import io.github.mzattera.predictivepowers.openai.client.chat.Function;
import io.github.mzattera.predictivepowers.openai.client.chat.Tool;
import io.github.mzattera.predictivepowers.openai.client.chat.ToolCall;
import io.github.mzattera.predictivepowers.openai.client.chat.ToolCallResult;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiTokenizer;
import io.github.mzattera.predictivepowers.services.AbstractChatService;
import io.github.mzattera.predictivepowers.services.ChatMessage;
import io.github.mzattera.predictivepowers.services.ChatMessage.Role;
import io.github.mzattera.predictivepowers.services.TextCompletion.FinishReason;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

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
		super(ep.getModelService());
		this.endpoint = ep;
		this.defaultReq = defaultReq;
		setMaxConversationTokens(Math.max(modelService.getContextSize(defaultReq.getModel()), 2046) * 3 / 4);
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
	 * This is the list of default tools that the service will use at any call.
	 * Leave this null or empty to prevent function calls.
	 */
	@Getter
	@Setter
	@NonNull
	private List<Tool> defaulTools = new ArrayList<>();

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
	 * @param tools List of tools that can be called (this can be empty to prevent
	 *              tool calls, or null to use the list of default tools).
	 */
	public OpenAiTextCompletion chat(String msg, List<Tool> tools) {
		return chat(msg, defaultReq, tools);
	}

	/**
	 * Continues current chat, with the provided message.
	 * 
	 * The exchange is added to the conversation history.
	 * 
	 * @param tools List of tools that can be called (this can be empty to prevent
	 *              tool calls, or null to use the list of default tools).
	 */
	public OpenAiTextCompletion chat(String msg, ChatCompletionsRequest req, List<Tool> tools) {
		return chat(new ChatMessage(Role.USER, msg), req, tools);
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
		return chat(msg, req, null);
	}

	/**
	 * Continues current chat, with the provided message.
	 * 
	 * The exchange is added to the conversation history.
	 * 
	 * @param tools List of tools that can be called (this can be empty to prevent
	 *              tool calls, or null to use the list of default tools).
	 */
	public OpenAiTextCompletion chat(ChatMessage msg, List<Tool> tools) {
		return chat(msg, defaultReq, tools);
	}

	/**
	 * Continues current chat, with the provided message.
	 * 
	 * The exchange is added to the conversation history.
	 * 
	 * @param tools List of tools that can be called (this can be empty to prevent
	 *              tool calls, or null to use the list of default tools).
	 */
	public OpenAiTextCompletion chat(ChatMessage msg, ChatCompletionsRequest req, List<Tool> tools) {

		List<ChatMessage> conversation = trimChat(history, true);
		conversation.add(msg);

		OpenAiTextCompletion result = chatCompletion(conversation, req, tools);

		history.add(msg);
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
		return chat(results, defaultReq, null);
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
		return chat(results, req, null);
	}

	/**
	 * Continues current chat, by returning results from tool calls to the OpenAI
	 * API. This is invoked after the model generated tool call(s), proper tools
	 * were invoked and now their results are ready to be returned to the model.
	 * Note that the service treats (old) function call API and (new) tool call API
	 * the same, translating function calls into tool calls transparently.
	 * 
	 * @param results the list of results from various call.
	 * @param tools   List of tools that can be called (this can be empty to prevent
	 *                tool calls, or null to use the list of default tools).
	 */
	public OpenAiTextCompletion chat(List<ToolCallResult> results, List<Tool> tools) {
		return chat(results, defaultReq, null);
	}

	/**
	 * Continues current chat, by returning results from tool calls to the OpenAI
	 * API. This is invoked after the model generated tool call(s), proper tools
	 * were invoked and now their results are ready to be returned to the model.
	 * Note that the service treats (old) function call API and (new) tool call API
	 * the same, translating function calls into tool calls transparently.
	 * 
	 * @param results the list of results from various call.
	 * @param tools   List of tools that can be called (this can be empty to prevent
	 *                tool calls, or null to use the list of default tools).
	 */
	public OpenAiTextCompletion chat(List<ToolCallResult> results, ChatCompletionsRequest req, List<Tool> tools) {

		List<ChatMessage> conversation = trimChat(history, true);

		OpenAiTextCompletion completion;

		// Transparently handles function and tool calls
		switch (((OpenAiModelService) modelService).getSupportedCall(req.getModel())) {
		case FUNCTIONS:
			if (results.size() != 1)
				throw new IllegalArgumentException("Current model supports only single funtion calls.");

			ToolCallResult callResult = results.get(0);
			OpenAiChatMessage msg = OpenAiChatMessage.builder() //
					.role(Role.FUNCTION) //
					.content(callResult.getResult()) //
					.name(callResult.getName()).build();
			conversation.add(msg);
			completion = chatCompletion(conversation, req, tools);

			history.add(msg);
			history.add(completion.getMessage());

			break;
		case TOOLS:

			List<OpenAiChatMessage> msgs = new ArrayList<>();
			for (ToolCallResult result : results) {
				msgs.add(OpenAiChatMessage.builder() //
						.role(Role.TOOL) //
						.toolCallId(result.getToolCallId()) //
						.content(result.getResult()) //
						.name(result.getName()).build() //
				);
			}
			conversation.addAll(msgs);
			completion = chatCompletion(conversation, req, tools);

			history.addAll(msgs);
			history.add(completion.getMessage());

			break;
		default:
			throw new IllegalArgumentException("Current model does not support funtion calling.");
		}

		// Make sure history is of desired length
		while (history.size() > getMaxHistoryLength())
			history.remove(0);

		return OpenAiTextCompletion.builder() //
				.text(completion.getMessage().getContent()) //
				.finishReason(completion.getFinishReason()) //
				.toolCalls(completion.getToolCalls()).build();
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
	 * @param tools List of tools that can be called (this can be empty to prevent
	 *              tool calls, or null to use the list of default tools).
	 */
	public OpenAiTextCompletion complete(String prompt, List<Tool> tools) {
		return complete(prompt, defaultReq, tools);
	}

	/**
	 * Completes text (executes given prompt).
	 * 
	 * Notice this does not consider or affects chat history but agent personality
	 * is used, if provided.
	 * 
	 * @param tools List of tools that can be called (this can be empty to prevent
	 *              tool calls, or null to use the list of default tools).
	 */
	public OpenAiTextCompletion complete(String prompt, ChatCompletionsRequest req, List<Tool> tools) {
		return complete(new ChatMessage(ChatMessage.Role.USER, prompt), req, tools);
	}

	@Override
	public OpenAiTextCompletion complete(ChatMessage prompt) {
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
	 * @param tools List of tools that can be called (this can be empty to prevent
	 *              tool calls, or null to use the list of default tools).
	 */
	public OpenAiTextCompletion complete(ChatMessage prompt, List<Tool> tools) {
		return complete(prompt, defaultReq, tools);
	}

	/**
	 * Completes text outside a conversation (executes given prompt).
	 * 
	 * Notice this does not consider or affects chat history but agent personality
	 * is used, if provided.
	 * 
	 * @param tools List of tools that can be called (this can be empty to prevent
	 *              tool calls, or null to use the list of default tools).
	 */
	public OpenAiTextCompletion complete(ChatMessage prompt, ChatCompletionsRequest req, List<Tool> tools) {
		List<ChatMessage> msgs = new ArrayList<>();
		if (getPersonality() != null)
			msgs.add(new ChatMessage(ChatMessage.Role.SYSTEM, getPersonality()));
		msgs.add(prompt);

		return complete(msgs, req, tools);
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
	 * @param tools List of tools that can be called (this can be empty to prevent
	 *              tool calls, or null to use the list of default tools).
	 */
	public OpenAiTextCompletion complete(List<ChatMessage> messages, List<Tool> tools) {
		return complete(messages, defaultReq, tools);
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
	public OpenAiTextCompletion complete(List<ChatMessage> messages, ChatCompletionsRequest req, List<Tool> tools) {
		return chatCompletion(messages, req, tools);
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
	protected OpenAiTextCompletion chatCompletion(List<ChatMessage> messages, ChatCompletionsRequest req,
			List<Tool> tools) {

		String model = req.getModel();
		OpenAiModelService modelSvc = (OpenAiModelService) modelService;

		req.setMessages(messages);

		// Sets he tools/functions to use.
		// It uses proper structure transparently, transparently based on the model
		if (tools == null)
			tools = defaulTools;
		if ((tools != null) && (tools.size() > 0)) { // seems to cause an error if you set it otherwise
			switch (modelSvc.getSupportedCall(model)) {
			case FUNCTIONS:
				List<Function> f = new ArrayList<>(tools.size());
				for (Tool t : tools) {
					if (t.getType() != Tool.Type.FUNCTION) // paranoid, but will support future tools
						throw new IllegalArgumentException("Current model does only support old funtion calling API.");
					f.add(t.getFunction());
				}
				req.setFunctions(f);
				break;
			case TOOLS:
				req.setTools(tools);
				break;
			default:
				throw new IllegalArgumentException("Current model does not support funtion calling.");
			}
		}

		boolean autofit = (req.getMaxTokens() == null) && (modelSvc.getContextSize(model, -1) != -1);

		try {
			if (autofit) {
				// Automatically set token limit, if needed
				OpenAiTokenizer counter = modelSvc.getTokenizer(model);
				int tok = counter.count(req); // Notice we must count function definitions too
				int size = modelSvc.getContextSize(model) - tok - 5;
				if (size <= 0)
					throw new IllegalArgumentException(
							"Your prompt exceeds context size: " + modelSvc.getContextSize(model));
				req.setMaxTokens(Math.min(size, modelSvc.getMaxNewTokens(model)));
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
						.text(choice.getMessage().getContent()) //
						.message(choice.getMessage()) //
						.toolCalls(fc).build();
			}
			return OpenAiTextCompletion.builder() //
					.finishReason(FinishReason.fromGptApi(choice.getFinishReason())) //
					.text(choice.getMessage().getContent()) //
					.message(choice.getMessage()) //
					.toolCalls(choice.getMessage().getToolCalls()).build();

		} finally {

			// for next call, or maxTokens will remain fixed
			if (autofit)
				req.setMaxTokens(null);
		}
	}
}