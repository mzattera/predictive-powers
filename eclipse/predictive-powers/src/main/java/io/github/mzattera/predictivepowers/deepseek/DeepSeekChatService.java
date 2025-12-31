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
package io.github.mzattera.predictivepowers.deepseek;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.openai.core.JsonMissing;
import com.openai.models.FunctionDefinition;
import com.openai.models.ResponseFormatJsonObject;
import com.openai.models.chat.completions.ChatCompletion.Choice;
import com.openai.models.chat.completions.ChatCompletionContentPart;
import com.openai.models.chat.completions.ChatCompletionContentPartText;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionCreateParams.Builder;
import com.openai.models.chat.completions.ChatCompletionCreateParams.ResponseFormat;
import com.openai.models.chat.completions.ChatCompletionMessage;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionTool;
import com.openai.models.chat.completions.ChatCompletionToolMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;

import io.github.mzattera.predictivepowers.EndpointException;
import io.github.mzattera.predictivepowers.openai.OpenAiUtil;
import io.github.mzattera.predictivepowers.services.AbstractAgent;
import io.github.mzattera.predictivepowers.services.Capability.ToolAddedEvent;
import io.github.mzattera.predictivepowers.services.Capability.ToolRemovedEvent;
import io.github.mzattera.predictivepowers.services.ModelService.Tokenizer;
import io.github.mzattera.predictivepowers.services.Tool;
import io.github.mzattera.predictivepowers.services.messages.ChatCompletion;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage.Author;
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
 * DeepSeek chat service using their chat completion API.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class DeepSeekChatService extends AbstractAgent {

	// TODO add "slot filling" capabilities: fill a slot in the prompt based on
	// values from a Map this is done partially

	private final static Logger LOG = LoggerFactory.getLogger(DeepSeekChatService.class);

	public static final String DEFAULT_MODEL = "deepseek-chat";

	private final DeepSeekModelService modelService;

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

	protected DeepSeekChatService(@NonNull DeepSeekEndpoint ep) {
		this(UUID.randomUUID().toString(), ep, DEFAULT_MODEL);
	}

	protected DeepSeekChatService(@NonNull DeepSeekEndpoint ep, @NonNull String model) {
		this(UUID.randomUUID().toString(), ep, model);
	}

	protected DeepSeekChatService(@NonNull String id, @NonNull DeepSeekEndpoint ep) {
		this(id, ep, DEFAULT_MODEL);
	}

	protected DeepSeekChatService(@NonNull String id, @NonNull DeepSeekEndpoint ep, @NonNull String model) {
		this.id = id;
		this.endpoint = ep;
		this.defaultRequest = ChatCompletionCreateParams.builder() //
				.model(model) //
				.messages(new ArrayList<>()).build();
		modelService = endpoint.getModelService();
	}

	@Getter
	@NonNull
	protected final DeepSeekEndpoint endpoint;

	@Override
	public String getModel() {
		return defaultRequest.model().asString();
	}

	@Override
	public void setModel(@NonNull String model) {
		defaultRequest = defaultRequest.toBuilder().model(model).build();
		setDefaultTools(); // Changing model might change the type of tool calls that are supported
		updateResponseFormat(); // Changing model might change the support for structured output
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
	private ChatCompletionCreateParams defaultRequest;

	@Override
	public Integer getTopK() {
		return null;
	}

	@Override
	public void setTopK(Integer topK) {
		if (topK != null)
			throw new EndpointException(new UnsupportedOperationException());
	}

	@Override
	public Double getTopP() {
		return defaultRequest.topP().orElse(null);
	}

	@Override
	public void setTopP(Double topP) {
		defaultRequest = defaultRequest.toBuilder().topP(topP).build();
	}

	@Override
	public Double getTemperature() {
		// Must scale from [0-2] to [0-100] considering default value as well
		return defaultRequest.temperature().isEmpty() ? null : (defaultRequest.temperature().get() * 50);
	}

	@Override
	public void setTemperature(Double temperature) {
		// Must scale from [0-2] to [0-100] considering default value as well
		if (temperature == null)
			defaultRequest = defaultRequest.toBuilder().temperature((Double) null).build();
		else
			defaultRequest = defaultRequest.toBuilder().temperature(temperature / 50).build();
	}

	@Override
	public Integer getMaxNewTokens() {
		return defaultRequest.maxCompletionTokens().isEmpty() ? null
				: defaultRequest.maxCompletionTokens().get().intValue();
	}

	@Override
	public void setMaxNewTokens(Integer maxNewTokens) {
		if (maxNewTokens == null)
			defaultRequest = defaultRequest.toBuilder().maxCompletionTokens((Long) null).build();
		else
			defaultRequest = defaultRequest.toBuilder().maxCompletionTokens(maxNewTokens.intValue()).build();
	}

	@Override
	public int getBaseTokens() {
		Builder b = defaultRequest.toBuilder().messages(new ArrayList<>());
		if (personality != null)
			// must add a system message on top with personality
			b.addMessage(ChatCompletionMessageParam.ofSystem( //
					ChatCompletionSystemMessageParam.builder() //
							.content(personality).build() //
			));

		try {
			return modelService.getTokenizer(getModel(), DeepSeekModelService.FALLBACK_TOKENIZER)
					.countAsJson(defaultRequest);
		} catch (JsonProcessingException e) {
			LOG.warn("Error serializing request: " + defaultRequest);
			return 0;
		}
	}

	@Getter
	private JsonSchema responseFormat;

	@Override
	public void setResponseFormat(JsonSchema jsonSchema) {
		this.responseFormat = jsonSchema;
		updateResponseFormat();
	}

	@SuppressWarnings("unchecked")
	private void updateResponseFormat() {

		if (responseFormat == null) {
			defaultRequest = defaultRequest.toBuilder().responseFormat(JsonMissing.of()).build();
			return;
		} else { // Only supports setting this flag; still, you shoudl provide an example of the
					// output format in the prompt
			defaultRequest = defaultRequest.toBuilder()
					.responseFormat(ResponseFormat.ofJsonObject(ResponseFormatJsonObject.builder().build())).build();
		}
	}

	// Because of how SDK is built, we cannot have a List<> containing both request
	// and response messages
	// so we must save it like this, pretending we have a conversation going on
	// inside this Builder
	@SuppressWarnings("unchecked")
	private final Builder history = ChatCompletionCreateParams.builder().model(JsonMissing.of()) //
			.messages(new ArrayList<>());

	/**
	 * For testing purposes only. Have you peek to history.
	 */
	List<ChatCompletionMessageParam> getUnmodifiableHistory() {
		return history.build().messages();
	}

	/**
	 * For testing purposes only. Adds a fake user message to history.
	 */
	void addMessageToHistory(String msg) {
		addMessageToHistory(
				ChatCompletionMessageParam.ofUser(ChatCompletionUserMessageParam.builder().content(msg).build()));
	}

	/**
	 * For testing purposes only. Adds a fake user message to history.
	 */
	void addMessageToHistory(ChatCompletionMessageParam msg) {
		history.addMessage(msg);
	}

	@Override
	public void clearConversation() {
		history.messages(new ArrayList<>());
	}

	@Getter
	private final String id;

	@Getter
	@Setter
	private String name = "DeepSeek Chat " + getId();

	@Getter
	@Setter
	private String description = "An agent instance using DeepSeek Chat Completions API";

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

	/**
	 * Set the tools to be used in all subsequent request. This sets tools or
	 * functions fields in defaultRequest properly, taking automatically in
	 * consideration whether the model support functions or tool calls.
	 * 
	 * @throws UnsupportedOperationException if the model does not support function
	 *                                       calls.
	 */
	@SuppressWarnings({ "unchecked" })
	private void setDefaultTools() {

		Builder b = defaultRequest.toBuilder();
		if (toolMap.size() == 0) { // No tools / functions used
			b.tools(JsonMissing.of());
		} else {
			List<Tool> tools = new ArrayList<>(toolMap.values());
			List<ChatCompletionTool> tls = new ArrayList<>(tools.size());
			for (Tool t : tools) {
				ChatCompletionTool f = ChatCompletionTool.builder() //
						.function( //
								FunctionDefinition.builder() //
										.name(t.getId()) //
										.description(t.getDescription()) //
										.strict(false)
										.parameters(OpenAiUtil.toFunctionParameters(t.getParameters(), false)).build() //
						).build();
				tls.add(f);
			}
			b.tools(tls);
		}

		defaultRequest = b.build();
	}

	@Override
	public ChatCompletion chat(ChatMessage msg) throws EndpointException {
		try {
			return chat(fromChatMessage(msg));
		} catch (Exception e) {
			throw OpenAiUtil.toEndpointException(e);
		}
	}

	/**
	 * Chats using a list of {@link ChatCompletionMessageParam}s, so individual
	 * message parameters can be set.
	 * 
	 * Notice that this list will be processed accordingly to conversation limits
	 * before being sent and that personality, if any, will be added on top.
	 */
	public ChatCompletion chat(List<ChatCompletionMessageParam> msg) throws EndpointException {

		try {
			// Add messages to conversation and trims it
			List<ChatCompletionMessageParam> conversation = new ArrayList<>(history.build().messages());
			conversation.addAll(msg);
			trimConversation(conversation);

			// Create response
			Pair<FinishReason, ChatCompletionMessage> result = chatCompletion(conversation);

			// Add messages and response to history
			msg.stream().forEach(history::addMessage);
			history.addMessage(result.getRight());

			// Make sure history is of desired length
			List<ChatCompletionMessageParam> tmp = new ArrayList<>(history.build().messages());
			int toTrim = tmp.size() - maxHistoryLength;
			if (toTrim > 0) {
				tmp.subList(0, toTrim).clear();
				history.messages(tmp);
			}

			return buildCompletion(result);

		} catch (Exception e) {
			throw OpenAiUtil.toEndpointException(e);
		}
	}

	@Override
	public ChatCompletion complete(ChatMessage prompt) throws EndpointException {
		try {
			return complete(fromChatMessage(prompt));
		} catch (Exception e) {
			throw OpenAiUtil.toEndpointException(e);
		}
	}

	/**
	 * Chats using a list of {@link ChatCompletionMessageParam}s, so individual
	 * message parameters can be set. This method ignores and does not affect
	 * conversation history, still, this list will be processed accordingly to
	 * conversation limits before being sent and that personality, if any, will be
	 * added on top.
	 */
	public ChatCompletion complete(List<ChatCompletionMessageParam> messages) throws EndpointException {

		try {
			List<ChatCompletionMessageParam> conversation = new ArrayList<>(messages);
			trimConversation(conversation);

			Pair<FinishReason, ChatCompletionMessage> result = chatCompletion(conversation);

			return buildCompletion(result);
		} catch (Exception e) {
			throw OpenAiUtil.toEndpointException(e);
		}
	}

	/**
	 * Completes given conversation.
	 * 
	 * Notice this does not consider or affects chat history. In addition, agent
	 * personality is NOT considered, but can be injected as first message in the
	 * list.
	 */
	private Pair<FinishReason, ChatCompletionMessage> chatCompletion(List<ChatCompletionMessageParam> messages) {

		// This ensures we can track last call messages from defaultRequest, for testing
		// reasons
		defaultRequest = defaultRequest.toBuilder().messages(messages).build();
		ChatCompletionCreateParams req = defaultRequest;

		com.openai.models.chat.completions.ChatCompletion resp = endpoint.getClient().chat().completions().create(req);
		Choice choice = resp.choices().get(0);
		return new ImmutablePair<>(OpenAiUtil.fromOpenAiApi(choice.finishReason().asString()), choice.message());
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
	private void trimConversation(List<ChatCompletionMessageParam> messages) throws JsonProcessingException {

		// Remove tool call results left on top without corresponding calls, or this
		// will cause HTTP 400 error for tools (it does not create issues for functions)
		int firstNonToolIndex = 0;
		for (ChatCompletionMessageParam m : messages) {
			if (m.isTool()) {
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
		Tokenizer counter = modelService.getTokenizer(getModel(), DeepSeekModelService.FALLBACK_TOKENIZER);
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
			messages.add(0, ChatCompletionMessageParam.ofSystem( //
					ChatCompletionSystemMessageParam.builder() //
							.content(personality).build() //
			));
	}

	/**
	 * Turns an ChatCompletionMessageParam returned by API into a ChatMessage.
	 */
	@SuppressWarnings("unchecked")
	private ChatMessage fromOpenAiMessage(ChatCompletionMessage msg) throws JsonProcessingException {

		if (msg.toolCalls().isPresent()) {

			// The model returned a set of tool calls, transparently translate that into a
			// message with a multiple tool calls
			List<ToolCall> calls = new ArrayList<>();
			for (ChatCompletionMessageToolCall call : msg.toolCalls().get()) {
				ToolCall toolCall;
				toolCall = ToolCall.builder() //
						.id(call.id()) //
						.tool(toolMap.get(call.function().name())) //
						.arguments(call.function().arguments()) //
						.build();
				calls.add(toolCall);
			}
			return new ChatMessage(Author.BOT, calls);
		}

		List<MessagePart> parts = new ArrayList<>();
		if (msg.content().isPresent()) {
			// Normal (text) message
			parts.add(new TextPart(msg.content().get()));
		}
		ChatMessage result = new ChatMessage(Author.BOT, parts);
		if (msg.refusal().isPresent()) {
			result.setRefusal(msg.refusal().get());
		}
		if (msg._additionalProperties().containsKey("reasoning_content")) {
			Optional<String> reasoning = msg._additionalProperties().get("reasoning_content").asString();
			if (reasoning.isPresent())
				result.setReasoning(reasoning.get());
		}
		return result;
	}

	/**
	 * This converts a generic ChatMessaege provided by user into an
	 * ChatCompletionMessageParam that is used for the OpenAi API. This is meant for
	 * abstraction and easier interoperability of agents.
	 * 
	 * @throws IOException
	 * 
	 * @throws IllegalArgumentException if the message is not in a format supported
	 *                                  directly by OpenAI API.
	 */
	private List<ChatCompletionMessageParam> fromChatMessage(ChatMessage msg) throws IOException {

		if (msg.hasToolCalls())
			throw new IllegalArgumentException("Only API can generate tool/function calls.");

		List<ChatCompletionMessageParam> result = new ArrayList<>();

		if (msg.hasToolCallResults()) {
			List<ToolCallResult> results = msg.getToolCallResults();
			if (results.size() != msg.getParts().size())
				throw new IllegalArgumentException(
						"Tool/function call results cannot contain other parts in the message.");

			for (ToolCallResult r : results) {
				result.add(ChatCompletionMessageParam.ofTool( //
						ChatCompletionToolMessageParam.builder() //
								.content(r.getResult().toString()) //
								.toolCallId(r.getToolCallId()).build() //
				));
			}

			return result;
		}

		List<ChatCompletionContentPart> newParts = new ArrayList<>(msg.getParts().size());
		for (MessagePart part : msg.getParts()) {
			if (part instanceof TextPart) {
				newParts.add(ChatCompletionContentPart.ofText( //
						ChatCompletionContentPartText.builder().text(part.getContent()).build() //
				));

			} else {
				throw new IllegalArgumentException("Unsupported message part: " + part.getClass().getName());
			}
		} // for each part

		// Now builds the message out of its parts
		switch (msg.getAuthor()) {
		case USER:
			result.add(ChatCompletionMessageParam.ofUser( //
					ChatCompletionUserMessageParam.builder().contentOfArrayOfContentParts(newParts).build() //
			));
			break;
		case DEVELOPER:
			List<ChatCompletionContentPartText> txtParts = newParts.stream() //
					.filter(p -> p.isText()) //
					.map(p -> p.text().get()) //
					.collect(Collectors.toList());
			if (txtParts.size() != newParts.size())
				throw new IllegalArgumentException("DEVELOPER messages can only contain text");
			result.add(ChatCompletionMessageParam.ofSystem( //
					ChatCompletionSystemMessageParam.builder().contentOfArrayOfContentParts(txtParts).build() //
			));
			break;
		default:
			throw new IllegalArgumentException("Message author not supported: " + msg.getAuthor());
		}

		return result;
	}

	private ChatCompletion buildCompletion(Pair<FinishReason, ChatCompletionMessage> result)
			throws JsonProcessingException {
		ChatMessage botMsg = fromOpenAiMessage(result.getRight());
		if (botMsg.getRefusal() != null)
			return new ChatCompletion(FinishReason.INAPPROPRIATE, botMsg);
		else
			return new ChatCompletion(result.getLeft(), botMsg);
	}

	@Override
	public void close() {
		super.close();
		modelService.close();
	}
}