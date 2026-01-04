/*
 * Copyright 2023-2025 Massimiliano "Maxi" Zattera
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
package io.github.mzattera.predictivepowers.ollama;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.openai.models.chat.completions.ChatCompletionMessageParam;

import io.github.mzattera.ollama.client.model.AssistantMessage;
import io.github.mzattera.ollama.client.model.ChatRequest;
import io.github.mzattera.ollama.client.model.ChatResponse;
import io.github.mzattera.ollama.client.model.Function;
import io.github.mzattera.ollama.client.model.FunctionTool;
import io.github.mzattera.ollama.client.model.Message;
import io.github.mzattera.ollama.client.model.Message.RoleEnum;
import io.github.mzattera.ollama.client.model.RequestOptions;
import io.github.mzattera.ollama.client.model.SystemMessage;
import io.github.mzattera.ollama.client.model.ThinkSetting;
import io.github.mzattera.ollama.client.model.ToolMessage;
import io.github.mzattera.ollama.client.model.UserMessage;
import io.github.mzattera.predictivepowers.EndpointException;
import io.github.mzattera.predictivepowers.services.AbstractAgent;
import io.github.mzattera.predictivepowers.services.Capability.ToolAddedEvent;
import io.github.mzattera.predictivepowers.services.Capability.ToolRemovedEvent;
import io.github.mzattera.predictivepowers.services.ModelService.Tokenizer;
import io.github.mzattera.predictivepowers.services.Tool;
import io.github.mzattera.predictivepowers.services.messages.Base64FilePart;
import io.github.mzattera.predictivepowers.services.messages.ChatCompletion;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage.Author;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage.ChatMessageBuilder;
import io.github.mzattera.predictivepowers.services.messages.FilePart;
import io.github.mzattera.predictivepowers.services.messages.FilePart.ContentType;
import io.github.mzattera.predictivepowers.services.messages.FinishReason;
import io.github.mzattera.predictivepowers.services.messages.JsonSchema;
import io.github.mzattera.predictivepowers.services.messages.MessagePart;
import io.github.mzattera.predictivepowers.services.messages.TextPart;
import io.github.mzattera.predictivepowers.services.messages.ToolCall;
import io.github.mzattera.predictivepowers.services.messages.ToolCallResult;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Ollama chat service.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class OllamaChatService extends AbstractAgent {

	// TODO URGENT we do not have such a6 thing...either we take the first or
	// pretend user specifies a model
	public static final String DEFAULT_MODEL = "qwen3:8b";

	private final OllamaModelService modelService;

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
	// If you change this, change isMaxConversationTokensSet()
	private int maxConversationTokens = Integer.MAX_VALUE;

	@Override
	public void setMaxConversationTokens(int n) {
		if (n < 1)
			throw new IllegalArgumentException("Must keep at least 1 token.");
		maxConversationTokens = n;
	}

	private boolean isMaxConversationTokensSet() {
		return (maxConversationTokens != Integer.MAX_VALUE);
	}

	protected OllamaChatService(@NonNull OllamaEndpoint ep) {
		this(UUID.randomUUID().toString(), ep, DEFAULT_MODEL);
	}

	protected OllamaChatService(@NonNull OllamaEndpoint ep, @NonNull String model) {
		this(UUID.randomUUID().toString(), ep, model);
	}

	protected OllamaChatService(@NonNull String id, @NonNull OllamaEndpoint ep) {
		this(id, ep, DEFAULT_MODEL);
	}

	protected OllamaChatService(@NonNull String id, @NonNull OllamaEndpoint ep, @NonNull String model) {
		this.id = id;
		this.endpoint = ep;

		RequestOptions options = new RequestOptions();
//		RequestOptions options = new RequestOptions() //
//				.numCtx(16000) //
//				.numPredict(8000);
		this.defaultRequest = new ChatRequest() //
				.model(model) //
				.think(new ThinkSetting(false)) //
				.options(options);
		this.modelService = endpoint.getModelService();
	}

	@Getter
	@NonNull
	protected final OllamaEndpoint endpoint;

	@Override
	public String getModel() {
		return defaultRequest.getModel();
	}

	@Override
	public void setModel(@NonNull String model) {
		defaultRequest.setModel(model);
	}

	/**
	 * This request, with its parameters, is used as default setting for each call.
	 * 
	 * You can change any parameter to change these defaults (e.g. the model used)
	 * and the change will apply to all subsequent calls.
	 */
	@Getter
	@Setter
	@NonNull
	private ChatRequest defaultRequest;

	@Override
	public Integer getTopK() {
		RequestOptions opt = defaultRequest.getOptions();
		if (opt != null)
			return opt.getTopK();
		return null;
	}

	@Override
	public void setTopK(Integer topK) {
		RequestOptions opt = defaultRequest.getOptions();
		if (opt == null) {
			opt = new RequestOptions();
			defaultRequest.setOptions(opt);
		}
		opt.setTopK(topK);
	}

	@Override
	public Double getTopP() {
		RequestOptions opt = defaultRequest.getOptions();
		if (opt != null)
			return opt.getTopP() == null ? null : opt.getTopP().doubleValue();
		return null;
	}

	@Override
	public void setTopP(Double topP) {
		RequestOptions opt = defaultRequest.getOptions();
		if (opt == null) {
			opt = new RequestOptions();
			defaultRequest.setOptions(opt);
		}
		opt.setTopP(topP == null ? null : topP.floatValue());
	}

	@Override
	public Double getTemperature() {
		// Must scale from [0-1] to [0-100] considering default value as well
		RequestOptions opt = defaultRequest.getOptions();
		if (opt != null)
			return opt.getTemperature() == null ? null : Double.valueOf(opt.getTemperature().floatValue() * 100f);
		return null;
	}

	@Override
	public void setTemperature(Double temperature) {
		// Must scale from [0-1] to [0-100] considering default value as well
		RequestOptions opt = defaultRequest.getOptions();
		if (opt == null) {
			opt = new RequestOptions();
			defaultRequest.setOptions(opt);
		}
		opt.setTemperature(temperature == null ? null : temperature.floatValue() / 100f);
	}

	@Override
	public Integer getMaxNewTokens() {
		RequestOptions opt = defaultRequest.getOptions();
		if (opt != null)
			return opt.getNumPredict() == null ? null : opt.getNumPredict();
		return null;
	}

	@Override
	public void setMaxNewTokens(Integer maxNewTokens) {
		RequestOptions opt = defaultRequest.getOptions();
		if (opt == null) {
			opt = new RequestOptions();
			defaultRequest.setOptions(opt);
		}
		opt.setNumPredict(maxNewTokens);
	}

	@Override
	public int getBaseTokens() {
		if (personality != null)
			return modelService.getTokenizer(getModel()).count(personality);
		return 0;
	}

	@Getter
	private JsonSchema responseFormat;

	@Override
	public void setResponseFormat(JsonSchema jsonSchema) {

		try {
			defaultRequest.setFormat(JsonSchema.JSON_MAPPER.readValue(jsonSchema.asJsonSchema(), Object.class));
		} catch (JsonProcessingException e) {
			throw new EndpointException(e);
		}
		responseFormat = jsonSchema;
	}

	private List<Message> history = new ArrayList<>();

	/**
	 * For testing purposes only. Have you peek to history.
	 */
	List<Message> getUnmodifiableHistory() {
		return List.copyOf(history);
	}

	/**
	 * For testing purposes only. Adds a fake user message to history.
	 */
	void addMessageToHistory(String msg) {
		addMessageToHistory(new UserMessage().content(msg));
	}

	/**
	 * For testing purposes only. Adds a fake user message to history.
	 */
	void addMessageToHistory(Message msg) {
		history.add(msg);
	}

	@Override
	public void clearConversation() {
		history.clear();
	}

	@Getter
	private final String id;

	@Getter
	@Setter
	private String name = "Ollama Chat " + getId();

	@Getter
	@Setter
	private String description = "An agent instance using Ollama Chat API";

	/**
	 * This is called when a new tool is made available.
	 */
	@Override
	public void onToolAdded(@NonNull ToolAddedEvent evt) {
		super.onToolAdded(evt);
		setDefaultTools();
	}

	/**
	 * This is called when a tool is removed.
	 */
	@Override
	public void onToolRemoved(@NonNull ToolRemovedEvent evt) {
		super.onToolRemoved(evt);
		setDefaultTools();
	}

	private void setDefaultTools() {

		List<io.github.mzattera.ollama.client.model.Tool> tools = new ArrayList<>(toolMap.size());
		for (Tool t : toolMap.values()) {

			io.github.mzattera.ollama.client.model.Tool hfTool = new FunctionTool() //
					.function(new Function().name(t.getId()) //
							.description(t.getDescription()) //
							.parameters(new JsonSchema(t.getParameters()).asMap(false)));
			tools.add(hfTool);
		}
		defaultRequest.setTools(tools);
	}

	@Override
	public ChatCompletion chat(ChatMessage msg) {
		try {
			return chat(fromChatMessage(msg));
		} catch (Exception e) {
			throw OllamaUtil.toEndpointException(e);
		}
	}

	/**
	 * Chats using a list of {@link ChatCompletionMessageParam}s, so individual
	 * message parameters can be set.
	 * 
	 * Notice that this list will be processed accordingly to conversation limits
	 * before being sent and that personality, if any, will be added on top.
	 */
	public ChatCompletion chat(List<Message> msg) throws EndpointException {

		try {
			// Add messages to conversation and trims it
			List<Message> conversation = new ArrayList<>(history);
			conversation.addAll(msg);
			trimConversation(conversation);

			// Create response
			Pair<FinishReason, AssistantMessage> result = chatCompletion(conversation);

			// Add messages and response to history
			history.addAll(msg);
			history.add(result.getRight());

			// Make sure history is of desired length
			List<Message> tmp = new ArrayList<>(history);
			int toTrim = tmp.size() - maxHistoryLength;
			if (toTrim > 0) {
				tmp.subList(0, toTrim).clear();
				history = tmp;
			}

			return new ChatCompletion(result.getLeft(), fromOllamaMessage(result.getRight()));
		} catch (Exception e) {
			throw OllamaUtil.toEndpointException(e);
		}
	}

	@Override
	public ChatCompletion complete(ChatMessage prompt) throws EndpointException {
		try {
			return complete(fromChatMessage(prompt));
		} catch (Exception e) {
			throw OllamaUtil.toEndpointException(e);
		}
	}

	/**
	 * Chats using a list of {@link Message}s, so individual message parameters can
	 * be set. This method ignores and does not affect conversation history, still,
	 * this list will be processed accordingly to conversation limits before being
	 * sent and that personality, if any, will be added on top.
	 */
	public ChatCompletion complete(List<Message> messages) throws EndpointException {
		try {
			List<Message> conversation = new ArrayList<>(messages);
			trimConversation(conversation);

			Pair<FinishReason, AssistantMessage> result = chatCompletion(conversation);

			return new ChatCompletion(result.getLeft(), fromOllamaMessage(result.getRight()));
		} catch (Exception e) {
			throw OllamaUtil.toEndpointException(e);
		}
	}

	/**
	 * Completes given conversation.
	 * 
	 * Notice this does not consider or affects chat history. In addition, agent
	 * personality is NOT considered.
	 */
	private Pair<FinishReason, AssistantMessage> chatCompletion(List<Message> messages) {

		defaultRequest.setMessages(messages);
		ChatResponse response = endpoint.getClient().chat(defaultRequest);

		return new ImmutablePair<>(OllamaUtil.fromOllamaFinishReason(response),
				(AssistantMessage) response.getMessage());
	}

	/**
	 * Trims given list of messages (typically a conversation history), so it fits
	 * the limits set in this instance (that is, maximum conversation steps and
	 * tokens).
	 * 
	 * Notice the personality is always and automatically added to the trimmed list
	 * (if set).
	 * 
	 * @throws JsonProcessingException
	 * 
	 * @throws IllegalArgumentException if no message can be added because of
	 *                                  context size limitations.
	 */
	private void trimConversation(List<Message> messages) throws JsonProcessingException {

		// Remove tool call results left on top without corresponding calls
		int firstNonToolIndex = 0;
		for (Message m : messages) {
			if (m.getRole() == RoleEnum.TOOL) {
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
		int steps = 0;
		Tokenizer counter = modelService.getTokenizer(getModel(), OllamaModelService.FALLBACK_TOKENIZER);
		for (int i = messages.size() - 1; i >= 0; --i) {
			if (steps >= maxConversationSteps)
				break;

			if (isMaxConversationTokensSet()) { // We do not invoke the tokenizer if we do not have to
				int tok = counter.countAsJson(messages.subList(i, messages.size()));
				if (tok > maxConversationTokens) {
					break;
				}
			}
			++steps;
		}
		if (steps == 0)
			throw new IllegalArgumentException("Context to small to fit a single message");
		else if (steps < messages.size())
			messages.subList(0, messages.size() - steps).clear();

		if (personality != null)
			// must add a system message on top with personality
			messages.add(0, new SystemMessage().content(personality));
	}

	/**
	 * Turns a Message returned by API into a ChatMessage.
	 * 
	 * @param msg
	 * @return
	 */
	private ChatMessage fromOllamaMessage(AssistantMessage msg) {

		if ((msg.getToolCalls() != null) && (msg.getToolCalls().size() > 0)) {

			// The model returned a set of tool calls, transparently translate that into a
			// message with a multiple tool calls
			List<ToolCall> calls = new ArrayList<>();
			for (io.github.mzattera.ollama.client.model.ToolCall call : msg.getToolCalls()) {
				ToolCall toolCall;
				toolCall = ToolCall.builder() //
						.id(call.getId()) //
						.tool(toolMap.get(call.getFunction().getName())) //
						.arguments(call.getFunction().getArguments()) //
						.build();
				calls.add(toolCall);
			}
			return new ChatMessage(Author.BOT, calls);
		}

		List<MessagePart> parts = new ArrayList<>();
		if (msg.getContent() != null) {
			// Normal (text) message
			parts.add(new TextPart(msg.getContent()));
		}
		if (msg.getImages() != null) {
			// Images returned (should never be the case at the moment)
			for (int i = 0; i < msg.getImages().size(); ++i) {
				parts.add(new Base64FilePart(msg.getImages().get(i), "Image_" + i));
			}
		}

		ChatMessageBuilder b = ChatMessage.builder().author(Author.BOT).parts(parts);
		if (msg.getThinking() != null)
			b.reasoning(msg.getThinking());

		return b.build();
	}

	/**
	 * This converts a generic ChatMessaege provided by user into an Message that is
	 * used for the Ollama API. This is meant for abstraction and easier
	 * interoperability of agents.
	 * 
	 * @throws IOException
	 */
	private List<Message> fromChatMessage(ChatMessage msg) throws IOException {

		if (msg.hasToolCalls())
			throw new IllegalArgumentException("Only API can generate tool/function calls.");

		List<Message> result = new ArrayList<>();

		if (msg.hasToolCallResults()) {

			List<ToolCallResult> results = msg.getToolCallResults();
			if (results.size() != msg.getParts().size())
				throw new IllegalArgumentException(
						"Tool/function call results cannot contain other parts in the message.");

			for (ToolCallResult r : results) {
				result.add(new ToolMessage().toolCallId(r.getToolCallId()).content(r.getContent()));
			}
			return result;
		}

		// Check remaining parts are images or text.
		// API will fail if the model does not support them eventually (e.g. using an
		// image for a model that is not a vision model).

		StringBuilder content = new StringBuilder();
		List<String> b64Images = new ArrayList<>();

		for (MessagePart part : msg.getParts()) {

			if (part instanceof TextPart) {
				if (content.length() > 0)
					content.append("\n\n");
				content.append(part.getContent());
			} else if (part instanceof FilePart) {
				FilePart file = (FilePart) part;
				if (file.getContentType() != ContentType.IMAGE)
					throw new IllegalArgumentException("Only image files are supported");

				if (!(file instanceof Base64FilePart))
					file = new Base64FilePart(file);
				b64Images.add(((Base64FilePart) file).getEncodedContent());
			}
		}

		switch (msg.getAuthor()) {
		case USER:
			UserMessage uMsg = new UserMessage().content(content.toString()).images(b64Images);
			result.add(uMsg);
			return result;
		case DEVELOPER:
			SystemMessage sMsg = new SystemMessage().content(content.toString()).images(b64Images);
			result.add(sMsg);
			return result;
		default:
			throw new IllegalArgumentException("Unrecognnised author: " + msg.getAuthor());
		}
	}

	@Override
	public void close() {
		super.close();
		modelService.close();
	}
}