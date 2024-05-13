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
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mzattera.predictivepowers.openai.client.AzureOpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.OpenAiException;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsChoice;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsResponse;
import io.github.mzattera.predictivepowers.openai.client.chat.Function;
import io.github.mzattera.predictivepowers.openai.client.chat.FunctionCall;
import io.github.mzattera.predictivepowers.openai.client.chat.OpenAiTool;
import io.github.mzattera.predictivepowers.openai.client.chat.OpenAiToolCall;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatMessage.Role;
import io.github.mzattera.predictivepowers.services.AbstractAgent;
import io.github.mzattera.predictivepowers.services.ChatService;
import io.github.mzattera.predictivepowers.services.Tool;
import io.github.mzattera.predictivepowers.services.ToolInitializationException;
import io.github.mzattera.predictivepowers.services.messages.Base64FilePart;
import io.github.mzattera.predictivepowers.services.messages.ChatCompletion;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage.Author;
import io.github.mzattera.predictivepowers.services.messages.FilePart;
import io.github.mzattera.predictivepowers.services.messages.FilePart.ContentType;
import io.github.mzattera.predictivepowers.services.messages.FinishReason;
import io.github.mzattera.predictivepowers.services.messages.MessagePart;
import io.github.mzattera.predictivepowers.services.messages.TextPart;
import io.github.mzattera.predictivepowers.services.messages.ToolCall;
import io.github.mzattera.predictivepowers.services.messages.ToolCallResult;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

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
public class OpenAiChatService extends AbstractAgent implements ChatService {

	// TODO add "slot filling" capabilities: fill a slot in the prompt based on
	// values from a Map this is done partially

	private final static Logger LOG = LoggerFactory.getLogger(OpenAiChatService.class);

	public static final String DEFAULT_MODEL = "gpt-4-turbo";
	
	@Getter
	@Setter
	private int maxHistoryLength = 1000;

	@Getter
	@Setter
	private String personality = null;

	@Getter
	private int maxConversationSteps = Integer.MAX_VALUE;

	@Override
	public void setMaxConversationSteps(int l) {
		if (l < 1)
			throw new IllegalArgumentException("Must keep at least 1 message.");
		maxConversationSteps = l;
	}

	@Getter
	private int maxConversationTokens = Integer.MAX_VALUE;

	@Override
	public void setMaxConversationTokens(int n) {
		if (n < 1)
			throw new IllegalArgumentException("Must keep at least 1 token.");
		maxConversationTokens = n;
	}

	public OpenAiChatService(@NonNull OpenAiEndpoint ep) {
		this(ep, DEFAULT_MODEL);
	}

	public OpenAiChatService(@NonNull OpenAiEndpoint ep, @NonNull String model) {
		this(ep, ChatCompletionsRequest.builder().model(model).build());
	}

	public OpenAiChatService(OpenAiEndpoint ep, ChatCompletionsRequest defaultReq) {
		this.endpoint = ep;
		this.defaultReq = defaultReq;
		modelService = endpoint.getModelService();
		register();

		// With GPT you pay max tokens, even if they are not generated, so we put a
		// reasonable limit here
		String model = defaultReq.getModel();
		int maxReplyTk = Math.min(modelService.getContextSize(model) / 5, modelService.getMaxNewTokens(model));
		setMaxNewTokens(maxReplyTk);
		maxConversationTokens = modelService.getContextSize(model) - maxReplyTk;
		defaultReq.setN(1); // paranoid
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
		register();
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

	/**
	 * Register the deploy ID if we are running in MS Azure See
	 * {@link AzureOpenAiModelService}.
	 */
	private void register() {
		if (endpoint instanceof AzureOpenAiEndpoint) {
			String model = getModel();
			if (modelService.get(model) == null) {
				// Do a "fake" call to read base model ID (see AzureOpenAiModelService JavaDoc).
				ChatCompletionsRequest req = ChatCompletionsRequest.builder().model(model).maxTokens(1).build();
				req.getMessages().add(new OpenAiChatMessage(Role.USER, "Hi"));
				ChatCompletionsResponse resp = endpoint.getClient().createChatCompletion(req);
				((AzureOpenAiModelService) modelService).map(model, resp.getModel());
			}
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
	public int getBaseTokens() {
		List<OpenAiChatMessage> old = defaultReq.getMessages();

		List<OpenAiChatMessage> msgs = new ArrayList<>();
		if (personality != null) {
			msgs.add(new OpenAiChatMessage(Role.SYSTEM, personality));
		}
		defaultReq.setMessages(msgs);
		int result = modelService.getTokenizer(getModel()).count(defaultReq);
		defaultReq.setMessages(old);

		return result;
	}

	private final List<OpenAiChatMessage> history = new ArrayList<>();

	@Override
	public List<? extends ChatMessage> getHistory() {
		return history.stream().map(this::fromOpenAiMessage).collect(Collectors.toList());
	}

	// TODO URGENT, probably add method setHistory(List<ChatMessage> to interface)
	/** For testing purposes only. This is not meant to be used. */
	List<OpenAiChatMessage> getModifiableHistory() {
		return history;
	}

	@Override
	public void clearConversation() {
		history.clear();
	}

	@Getter
	private final String id = UUID.randomUUID().toString();

	@Getter
	@Setter
	private String name = "OpenAI Chat API Assistant";

	@Getter
	@Setter
	private String description = "";

	@Override
	protected void putTool(@NonNull Tool tool) throws ToolInitializationException {
		OpenAiTool t = (tool instanceof OpenAiTool) ? (OpenAiTool) tool : new OpenAiTool(tool);
		super.putTool(t);
		setDefaultTools();
	}

	@Override
	protected boolean removeTool(@NonNull String toolId) {
		if (super.removeTool(toolId)) {
			setDefaultTools();
			return true;
		}
		return false;
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

		if (toolMap.size() == 0) { // No tools / functions used
			defaultReq.setTools(null);
			defaultReq.setFunctions(null);
			return;
		}

		List<OpenAiTool> tools = toolMap.values().stream() //
				.map(tool -> (OpenAiTool) tool) //
				.collect(Collectors.toList());

		switch (modelService.getSupportedCallType(getModel())) {
		case FUNCTIONS:
			List<Function> f = new ArrayList<>(toolMap.size());
			for (OpenAiTool t : tools) {
				if (t.getType() != OpenAiTool.Type.FUNCTION) // paranoid, but will support future tools
					throw new UnsupportedOperationException("Current model supports only old funtion calling API.");
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
			throw new UnsupportedOperationException("Current model does not support function calling.");
		}
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
	public ChatCompletion chat(String msg, ChatCompletionsRequest req) {
		return chat(new ChatMessage(msg), req);
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
	public ChatCompletion chat(ChatMessage msg, ChatCompletionsRequest req) {

		List<OpenAiChatMessage> m = fromChatMessage(msg);

		List<OpenAiChatMessage> conversation = new ArrayList<>(history);
		conversation.addAll(m);
		trimConversation(conversation);

		Pair<FinishReason, OpenAiChatMessage> result = chatCompletion(conversation, req);

		history.addAll(m);
		history.add(result.getRight());

		// Make sure history is of desired length
		int toTrim = history.size() - maxHistoryLength;
		if (toTrim > 0)
			history.subList(0, toTrim).clear();

		return new ChatCompletion(result.getLeft(), fromOpenAiMessage(result.getRight()));
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
	public ChatCompletion complete(String prompt, ChatCompletionsRequest req) {
		return complete(new ChatMessage(prompt), req);
	}

	@Override
	public ChatCompletion complete(ChatMessage prompt) {
		return complete(prompt, defaultReq);
	}

	/**
	 * Completes text outside a conversation (executes given prompt).
	 * 
	 * Notice this does not consider or affects chat history but agent personality
	 * is used, if provided.
	 */
	public ChatCompletion complete(ChatMessage prompt, ChatCompletionsRequest req) {
		List<OpenAiChatMessage> msgs = new ArrayList<>(fromChatMessage(prompt));
		trimConversation(msgs); // Adds personality as well

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
	public ChatCompletion complete(List<OpenAiChatMessage> messages) {
		return complete(messages, defaultReq);
	}

	/**
	 * Completes given conversation.
	 * 
	 * Notice the list of messages is supposed to be passed as-is to the chat API,
	 * without modifications. This means bot personality is NOT added on top of the
	 * list of messages, nor the list is checked or trimmed for excessive length.
	 */
	public ChatCompletion complete(List<OpenAiChatMessage> messages, ChatCompletionsRequest req) {
		Pair<FinishReason, OpenAiChatMessage> result = chatCompletion(messages, req);
		return new ChatCompletion(result.getLeft(), fromOpenAiMessage(result.getRight()));
	}

	/**
	 * Completes given conversation.
	 * 
	 * Notice this does not consider or affects chat history. In addition, agent
	 * personality is NOT considered, but can be injected as first message in the
	 * list.
	 * 
	 * @param toolMap List of tools that can be called (this can be empty to prevent
	 *                tool calls, or null to use the list of default tools).
	 */
	private Pair<FinishReason, OpenAiChatMessage> chatCompletion(List<OpenAiChatMessage> messages,
			ChatCompletionsRequest req) {

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
						// TODO Add a test case
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
			return new ImmutablePair<>(FinishReason.fromOpenAiApi(choice.getFinishReason()), choice.getMessage());

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
	 * Notice the personality is always and automatically added to the trimmed list
	 * (if set).
	 * 
	 * @throws IllegalArgumentException if no message can be added because of
	 *                                  context size limitations.
	 */
	private void trimConversation(List<OpenAiChatMessage> messages) {

		// Remove tool call results left on top without corresponding calls, or this
		// will cause HTTP 400 error for tools (it does not create issues for functions)
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
			if (steps >= maxConversationSteps)
				break;

			int tok = counter.count(messages.subList(i, messages.size()));
			if (tok > maxConversationTokens) {
				break;
			}

			++steps;
		}
		if (steps == 0)
			throw new IllegalArgumentException("Context to small to fit a single message");
		else if (steps < messages.size())
			messages.subList(0, messages.size() - steps).clear();

		if (personality != null)
			// must add a system message on top with personality
			messages.add(0, new OpenAiChatMessage(Role.SYSTEM, personality));
	}

	/**
	 * Turns an OpenAiChatMessage into a ChatMessage.
	 * 
	 * @param msg
	 * @return
	 */
	private ChatMessage fromOpenAiMessage(OpenAiChatMessage msg) {
		if (msg.getFunctionCall() != null) {

			// The model returned a function call, transparently translate it into a message
			// with a single tool call
			List<ToolCall> calls = new ArrayList<>();
			FunctionCall funCall = msg.getFunctionCall();
			ToolCall toolCall = ToolCall.builder() //
					.id(funCall.getName()) //
					.tool(toolMap.get(funCall.getName())) //
					.arguments(funCall.getArguments()) //
					.build();
			calls.add(toolCall);
			return new ChatMessage(Author.BOT, calls);
		}
		if (msg.getToolCalls() != null) {

			// The model returned a set of tool calls, transparently translate that into a
			// message with a multiple tool calls
			List<ToolCall> calls = new ArrayList<>();
			for (OpenAiToolCall call : msg.getToolCalls()) {
				ToolCall toolCall = ToolCall.builder() //
						.id(call.getId()) //
						.tool(toolMap.get(call.getFunction().getName())) //
						.arguments(call.getFunction().getArguments()) //
						.build();
				calls.add(toolCall);
			}
			return new ChatMessage(Author.BOT, calls);
		}

		// Normal (text) message
		return new ChatMessage(Author.BOT, msg.getContentParts());
	}

	/**
	 * This converts a generic ChatMessaege into an OpenAIChatMessage that is used
	 * for the OpenAi API. This is meant for abstraction and easier interoperability
	 * of agents.
	 * 
	 * @throws IllegalArgumentException if the message is not in a format supported
	 *                                  directly by OpenAI API.
	 */
	private List<OpenAiChatMessage> fromChatMessage(ChatMessage msg) {

		// TODO add test to check this

		if (msg.hasToolCalls())
			throw new IllegalArgumentException("Only API can generate tool/function calls.");

		List<OpenAiChatMessage> result = new ArrayList<>();

		if (msg.hasToolCallResults()) {

			// Transparently handles function and tool calls

			List<? extends ToolCallResult> results = msg.getToolCallResults();
			if (results.size() != msg.getParts().size())
				throw new IllegalArgumentException(
						"Tool/function call results cannot contain other parts in the message.");

			switch (modelService.getSupportedCallType(getModel())) {
			case FUNCTIONS:
				if (results.size() != 1)
					throw new IllegalArgumentException("Model supports only single function calls.");

				result.add(new OpenAiChatMessage(Role.FUNCTION, results.get(0)));
				break;
			case TOOLS:
				for (ToolCallResult r : results) {
					result.add(new OpenAiChatMessage(Role.TOOL, r));
				}
				break;
			default:
				throw new IllegalArgumentException("Model does not support function calling.");
			}
			return result;
		}

		// Check remaining parts are images or text.
		// API will fail if the model does not support them eventually (e.g. using an
		// image for a model that is not a vision model).

		List<MessagePart> newParts = new ArrayList<>(msg.getParts().size());
		for (MessagePart part : msg.getParts()) {

			if (part instanceof TextPart) {
				newParts.add(part);

			} else if (part instanceof FilePart) {

				FilePart file = (FilePart) part;
				if (file.getContentType() != ContentType.IMAGE)
					throw new IllegalArgumentException("Only files with coontent type = IMAGE are supported.");

				// We ensure the image is Base64 encoded and pre-scaled,
				// unless it is a remote file that we do not touch (for performance reasons)
				if (!file.isLocalFile()) {
					newParts.add(part);
				} else {
					try {
						newParts.add(Base64FilePart.forOpenAi(file));
					} catch (Exception e) {
						newParts.add(part);
					}
				}
			} else
				throw new IllegalArgumentException(
						"Unsupported part in message (only text and images are supported): " + part);
		} // for each part

		result.add(new OpenAiChatMessage(msg.getAuthor(), newParts));
		return result;
	}
}