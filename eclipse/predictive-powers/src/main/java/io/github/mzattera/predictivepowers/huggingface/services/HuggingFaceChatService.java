/*
 * Copyright 2023-2024 Massimiliano "Maxi" Zattera
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

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.openai.models.chat.completions.ChatCompletionMessageParam;

import io.github.mzattera.hfinferenceapi.client.model.AssistantMessage;
import io.github.mzattera.hfinferenceapi.client.model.ChatCompletionRequest;
import io.github.mzattera.hfinferenceapi.client.model.ChatCompletionResponse;
import io.github.mzattera.hfinferenceapi.client.model.Choice;
import io.github.mzattera.hfinferenceapi.client.model.DeveloperMessage;
import io.github.mzattera.hfinferenceapi.client.model.FileContentPart;
import io.github.mzattera.hfinferenceapi.client.model.FileContentPartFile;
import io.github.mzattera.hfinferenceapi.client.model.Function;
import io.github.mzattera.hfinferenceapi.client.model.FunctionTool;
import io.github.mzattera.hfinferenceapi.client.model.FunctionToolCall;
import io.github.mzattera.hfinferenceapi.client.model.ImageContentPart;
import io.github.mzattera.hfinferenceapi.client.model.ImageContentPartUrl;
import io.github.mzattera.hfinferenceapi.client.model.JsonSchemaObject;
import io.github.mzattera.hfinferenceapi.client.model.JsonSchemaResponseFormat;
import io.github.mzattera.hfinferenceapi.client.model.Message;
import io.github.mzattera.hfinferenceapi.client.model.Message.RoleEnum;
import io.github.mzattera.hfinferenceapi.client.model.MessageContent;
import io.github.mzattera.hfinferenceapi.client.model.MessageContentPart;
import io.github.mzattera.hfinferenceapi.client.model.TextContentPart;
import io.github.mzattera.hfinferenceapi.client.model.ToolMessage;
import io.github.mzattera.hfinferenceapi.client.model.UserMessage;
import io.github.mzattera.predictivepowers.EndpointException;
import io.github.mzattera.predictivepowers.huggingface.util.HuggingFaceUtil;
import io.github.mzattera.predictivepowers.services.AbstractAgent;
import io.github.mzattera.predictivepowers.services.Capability.ToolAddedEvent;
import io.github.mzattera.predictivepowers.services.Capability.ToolRemovedEvent;
import io.github.mzattera.predictivepowers.services.ModelService.Tokenizer;
import io.github.mzattera.predictivepowers.services.Tool;
import io.github.mzattera.predictivepowers.services.messages.Base64FilePart;
import io.github.mzattera.predictivepowers.services.messages.ChatCompletion;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage.Author;
import io.github.mzattera.predictivepowers.services.messages.FilePart;
import io.github.mzattera.predictivepowers.services.messages.FinishReason;
import io.github.mzattera.predictivepowers.services.messages.JsonSchema;
import io.github.mzattera.predictivepowers.services.messages.MessagePart;
import io.github.mzattera.predictivepowers.services.messages.TextPart;
import io.github.mzattera.predictivepowers.services.messages.ToolCall;
import io.github.mzattera.predictivepowers.services.messages.ToolCallResult;
import io.github.mzattera.predictivepowers.util.CharTokenizer;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Hugging Face chat service.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class HuggingFaceChatService extends AbstractAgent {

//	private final static Logger LOG = LoggerFactory.getLogger(HuggingFaceChatService.class);

	public static final String DEFAULT_MODEL = "openai/gpt-oss-120b";

	private final HuggingFaceModelService modelService;

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

	public HuggingFaceChatService(@NonNull HuggingFaceEndpoint ep) {
		this(UUID.randomUUID().toString(), ep, DEFAULT_MODEL);
	}

	public HuggingFaceChatService(@NonNull HuggingFaceEndpoint ep, @NonNull String model) {
		this(UUID.randomUUID().toString(), ep, model);
	}

	public HuggingFaceChatService(@NonNull String id, @NonNull HuggingFaceEndpoint ep) {
		this(id, ep, DEFAULT_MODEL);
	}

	public HuggingFaceChatService(@NonNull String id, @NonNull HuggingFaceEndpoint ep, @NonNull String model) {
		this.id = id;
		this.endpoint = ep;
		this.defaultRequest = new ChatCompletionRequest().model(model).messages(new ArrayList<>());
		modelService = endpoint.getModelService();
	}

	@Getter
	@NonNull
	protected final HuggingFaceEndpoint endpoint;

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
	private ChatCompletionRequest defaultRequest;

	@Override
	public Integer getTopK() {
		return null;
	}

	@Override
	public void setTopK(Integer topK) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Double getTopP() {
		return defaultRequest.getTopP().doubleValue();
	}

	@Override
	public void setTopP(Double topP) {
		if (topP == null)
			defaultRequest.setTopP(null);
		else
			defaultRequest.setTopP(BigDecimal.valueOf(topP));
	}

	@Override
	public Double getTemperature() {
		// Must scale from [0-2] to [0-100] considering default value as well
		return defaultRequest.getTemperature() == null ? null : (defaultRequest.getTemperature().doubleValue() * 50);
	}

	@Override
	public void setTemperature(Double temperature) {
		// Must scale from [0-2] to [0-100] considering default value as well
		if (temperature == null)
			defaultRequest.setTemperature(null);
		else
			defaultRequest.setTemperature(BigDecimal.valueOf(temperature / 50));
	}

	@Override
	public Integer getMaxNewTokens() {
		return defaultRequest.getMaxTokens();
	}

	@Override
	public void setMaxNewTokens(Integer maxNewTokens) {
		defaultRequest.setMaxTokens(maxNewTokens);
	}

	@Override
	public int getBaseTokens() {
		defaultRequest.setMessages(new ArrayList<>());
		if (personality != null)
			// must add a system message on top with personality
			defaultRequest.addMessagesItem(new DeveloperMessage().content(new MessageContent(personality)));

		String json;
		try {
			json = JsonSchema.JSON_MAPPER.writer().writeValueAsString(defaultRequest);
		} catch (JsonProcessingException e) {
			throw HuggingFaceUtil.toEndpointException(e);
		}
		return modelService.getTokenizer(getModel(), CharTokenizer.getInstance()).count(json);
	}

	@Getter
	private JsonSchema responseFormat;

	@Override
	public void setResponseFormat(JsonSchema jsonSchema) {
		this.responseFormat = jsonSchema;

		if (responseFormat == null) {
			defaultRequest.setResponseFormat(null);
			return;
		}

		// Else set the schema properly
		JsonSchemaObject obj = new JsonSchemaObject()
				.name(responseFormat.getTitle() == null ? "NoName"
						: responseFormat.getTitle().replaceAll("[^a-zA-Z0-9_-]", ""))
				.description(
						responseFormat.getDescription() == null ? "No description" : responseFormat.getDescription())
				.schema(responseFormat.asMap(true));
		defaultRequest.setResponseFormat(new JsonSchemaResponseFormat().jsonSchema(obj));
	}

	@Getter
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
		addMessageToHistory(new UserMessage().content(new MessageContent(msg)));
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
	private String name = "Hugging Face Chat " + getId();

	@Getter
	@Setter
	private String description = "An agent instance using Hugging Face Chat Completions API";

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
	 * functions fields in defaultRequest properly.
	 */
	private void setDefaultTools() {

		List<io.github.mzattera.hfinferenceapi.client.model.Tool> tls = new ArrayList<>(toolMap.size());
		for (Tool t : toolMap.values()) {

			io.github.mzattera.hfinferenceapi.client.model.Tool hfTool = new FunctionTool()
					.function(new Function().name(t.getId()) //
							.description(t.getDescription()) //
							.strict(false).parameters(new JsonSchema(t.getParameters()).asMap(false)));
			tls.add(hfTool);
		}
		defaultRequest.setTools(tls);
	}

	@Override
	public ChatCompletion chat(ChatMessage msg) {
		try {
			return chat(fromChatMessage(msg));
		} catch (IOException e) {
			throw new EndpointException(e);
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
			Pair<FinishReason, Message> result = chatCompletion(conversation);

			// Add messages and response to history
			msg.stream().forEach(history::add);
			history.add(result.getRight());

			// Make sure history is of desired length
			List<Message> tmp = new ArrayList<>(history);
			int toTrim = tmp.size() - maxHistoryLength;
			if (toTrim > 0) {
				tmp.subList(0, toTrim).clear();
				history = tmp;
			}

			return new ChatCompletion(result.getLeft(), fromHuggingFaceMessage((AssistantMessage) result.getRight()));
		} catch (Exception e) {
			throw HuggingFaceUtil.toEndpointException(e);
		}
	}

	@Override
	public ChatCompletion complete(ChatMessage prompt) throws EndpointException {
		try {
			return complete(fromChatMessage(prompt));
		} catch (Exception e) {
			throw HuggingFaceUtil.toEndpointException(e);
		}
	}

	/**
	 * Chats using a list of {@link ChatCompletionMessageParam}s, so individual
	 * message parameters can be set. This method ignores and does not affect
	 * conversation history, still, this list will be processed accordingly to
	 * conversation limits before being sent and that personality, if any, will be
	 * added on top.
	 */
	public ChatCompletion complete(List<Message> messages) throws EndpointException {
		try {
			List<Message> conversation = new ArrayList<>(messages);
			trimConversation(conversation);

			Pair<FinishReason, Message> result = chatCompletion(conversation);
			return new ChatCompletion(result.getLeft(), fromHuggingFaceMessage((AssistantMessage) result.getRight()));
		} catch (Exception e) {
			throw HuggingFaceUtil.toEndpointException(e);
		}
	}

	/**
	 * Completes given conversation.
	 * 
	 * Notice this does not consider or affects chat history. In addition, agent
	 * personality is NOT considered, but can be injected as first message in the
	 * list.
	 */
	private Pair<FinishReason, Message> chatCompletion(
			List<io.github.mzattera.hfinferenceapi.client.model.Message> messages) throws EndpointException {

		try {
			defaultRequest.setMessages(messages);
			ChatCompletionResponse resp = endpoint.getClient().chatCompletion(defaultRequest);

			Choice choice = resp.getChoices().get(0);
			return new ImmutablePair<>(HuggingFaceUtil.fromHuggingFaceApi(choice.getFinishReason()),
					choice.getMessage());
		} catch (Exception e) {
			throw HuggingFaceUtil.toEndpointException(e);
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
	 * @throws JsonProcessingException
	 * 
	 * @throws IllegalArgumentException if no message can be added because of
	 *                                  context size limitations.
	 */
	private void trimConversation(List<Message> messages) throws JsonProcessingException {

		// Remove tool call results left on top without corresponding calls, or this
		// will cause HTTP 400 error for tools (it does not create issues for functions)
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
		Tokenizer counter = modelService.getTokenizer(getModel(), CharTokenizer.getInstance());
		for (int i = messages.size() - 1; i >= 0; --i) {
			if (steps >= maxConversationSteps)
				break;

			if (isMaxConversationTokensSet()) { // We do not invoke the tokenizer if we do not have to
				int tok = counter.count(
						JsonSchema.JSON_MAPPER.writer().writeValueAsString(messages.subList(i, messages.size())));
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
			messages.add(0, new DeveloperMessage().content(new MessageContent(personality)));
	}

	/**
	 * Turns an Message returned by API into a ChatMessage.
	 */
	private ChatMessage fromHuggingFaceMessage(AssistantMessage msg) throws JsonProcessingException {

		List<MessagePart> parts = new ArrayList<>();

		if (msg.getContent() != null) {
			parts.add(new TextPart(msg.getContent()));
		}

		String refusal = msg.getRefusal();
		if ((refusal != null) && !refusal.isBlank()) {
			parts.add(new TextPart("**The model generated a refusal**\n\n" + refusal));
		}

		List<io.github.mzattera.hfinferenceapi.client.model.ToolCall> toolCalls = msg.getToolCalls();
		if ((toolCalls != null) && (toolCalls.size() > 0)) { // Message has tool calls
			for (FunctionToolCall call : toolCalls.stream().map(c -> (FunctionToolCall) c)
					.collect(Collectors.toList())) {
				ToolCall toolCall;
				toolCall = ToolCall.builder() //
						.id(call.getId()) //
						.tool(toolMap.get(call.getFunction().getName())) //
						.arguments(call.getFunction().getArguments()) //
						.build();
				parts.add(toolCall);
			}
		}

		return new ChatMessage(Author.BOT, parts);
	}

	/**
	 * This converts a generic ChatMessaege provided by user into an Message that is
	 * used for the HuggingFace API. This is meant for abstraction and easier
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
				result.add(new ToolMessage().content(new MessageContent(r.getResult().toString()))
						.toolCallId(r.getToolCallId()));
			}

			return result;
		}

		// Check remaining parts are images or text.

		List<MessageContentPart> newParts = new ArrayList<>(msg.getParts().size());
		for (MessagePart part : msg.getParts()) {

			if (part instanceof TextPart) {
				newParts.add(new TextContentPart().text(part.getContent()));

			} else if (part instanceof FilePart) {
				FilePart file = (FilePart) part;

				switch (file.getContentType()) {
				case IMAGE:
					if (file.getUrl() != null) {
						newParts.add(new ImageContentPart()
								.imageUrl(new ImageContentPartUrl().url(file.getUrl().toString())));
					} else { // No image URL available - copying what OpenAI API does here, not sure it works
								// like this
						if (!(file instanceof Base64FilePart))
							file = new Base64FilePart(file);

						newParts.add(new ImageContentPart().imageUrl( //
								new ImageContentPartUrl() //
										.url("data:" + file.getMimeType() + ";base64,"
												+ ((Base64FilePart) file).getEncodedContent())));
					}
					break;
				default: // Generic file
					if (!(file instanceof Base64FilePart))
						file = new Base64FilePart(file);

					newParts.add(new FileContentPart()._file(new FileContentPartFile().filename(file.getName()) //
							.fileData("data:" + file.getMimeType() + ";base64,"
									+ ((Base64FilePart) file).getEncodedContent())));
					break;
				}
			} else
				throw new IllegalArgumentException("Unsupported message part: " + part.getClass().getName());
		} // for each part

		// Now builds the message out of its parts
		switch (msg.getAuthor()) {
		case USER:
			result.add(new UserMessage().content(new MessageContent(newParts)));
			break;
		case DEVELOPER:
			result.add(new DeveloperMessage().content(new MessageContent(newParts)));
			break;
		default:
			throw new IllegalArgumentException("Message author not supported: " + msg.getAuthor());
		}

		return result;
	}

	@Override
	public void close() {
		super.close();
		modelService.close();
	}
}