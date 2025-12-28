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
package io.github.mzattera.predictivepowers.openai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.openai.core.JsonMissing;
import com.openai.core.JsonValue;
import com.openai.errors.OpenAIServiceException;
import com.openai.models.FunctionDefinition;
import com.openai.models.ResponseFormatJsonObject;
import com.openai.models.ResponseFormatJsonSchema;
import com.openai.models.chat.completions.ChatCompletion.Choice;
import com.openai.models.chat.completions.ChatCompletionAudio;
import com.openai.models.chat.completions.ChatCompletionContentPart;
import com.openai.models.chat.completions.ChatCompletionContentPartImage;
import com.openai.models.chat.completions.ChatCompletionContentPartInputAudio;
import com.openai.models.chat.completions.ChatCompletionContentPartText;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionCreateParams.Builder;
import com.openai.models.chat.completions.ChatCompletionCreateParams.Function;
import com.openai.models.chat.completions.ChatCompletionCreateParams.ResponseFormat;
import com.openai.models.chat.completions.ChatCompletionDeveloperMessageParam;
import com.openai.models.chat.completions.ChatCompletionFunctionMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessage;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;
import com.openai.models.chat.completions.ChatCompletionTool;
import com.openai.models.chat.completions.ChatCompletionToolMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;

import io.github.mzattera.predictivepowers.EndpointException;
import io.github.mzattera.predictivepowers.openai.OpenAiModelService.OpenAiModelMetaData.CallType;
import io.github.mzattera.predictivepowers.services.AbstractAgent;
import io.github.mzattera.predictivepowers.services.Capability.ToolAddedEvent;
import io.github.mzattera.predictivepowers.services.Capability.ToolRemovedEvent;
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
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * OpenAI chat service.
 * 
 * This is currently implemented using Chat Completions API.
 * 
 * The service supports both function and tool calls transparently (it will
 * handle either, based on which model is used).
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class OpenAiChatService extends AbstractAgent {

	// TODO add "slot filling" capabilities: fill a slot in the prompt based on
	// values from a Map this is done partially

	private final static Logger LOG = LoggerFactory.getLogger(OpenAiChatService.class);

	public static final String DEFAULT_MODEL = "gpt-4o";

	private final OpenAiModelService modelService;

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

	protected OpenAiChatService(@NonNull OpenAiEndpoint ep) {
		this(UUID.randomUUID().toString(), ep, DEFAULT_MODEL);
	}

	protected OpenAiChatService(@NonNull OpenAiEndpoint ep, @NonNull String model) {
		this(UUID.randomUUID().toString(), ep, model);
	}

	protected OpenAiChatService(@NonNull String id, @NonNull OpenAiEndpoint ep) {
		this(id, ep, DEFAULT_MODEL);
	}

	protected OpenAiChatService(@NonNull String id, @NonNull OpenAiEndpoint ep, @NonNull String model) {
		this.id = id;
		this.endpoint = ep;
		this.defaultRequest = ChatCompletionCreateParams.builder() //
				.model(model) //
				.messages(new ArrayList<>()).build();
		modelService = endpoint.getModelService();
	}

	@Getter
	@NonNull
	protected final OpenAiEndpoint endpoint;

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
			throw new UnsupportedOperationException();
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
			b.addMessage(ChatCompletionMessageParam.ofDeveloper( //
					ChatCompletionDeveloperMessageParam.builder() //
							.content(personality).build() //
			));

		return modelService.getTokenizer(getModel()).count(b.build());
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
		}

		ChatCompletionCreateParams.ResponseFormat format = null;
		if (modelService.supportsStructuredOutput(getModel(), false)) { // guard
			format = ResponseFormat.ofJsonSchema( //
					ResponseFormatJsonSchema.builder() //
							.jsonSchema( //
									ResponseFormatJsonSchema.JsonSchema.builder() //
											.name(responseFormat.getTitle() == null ? "NoName"
													: responseFormat.getTitle().replaceAll("[^a-zA-Z0-9_-]", "")) //
											.description(responseFormat.getDescription() == null ? "No description"
													: responseFormat.getDescription()) //
											.schema(JsonValue.from(responseFormat.asMap(true))) //
											.build() //
							).build());

			System.out.println("*** " + getModel() + " -> structured");

		} else {
			format = ResponseFormat.ofJsonObject(ResponseFormatJsonObject.builder().build());

			System.out.println("*** " + getModel() + " -> JSON");
		}

		defaultRequest = defaultRequest.toBuilder().responseFormat(format).build();
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
	private String name = "OpenAI Chat " + getId();

	@Getter
	@Setter
	private String description = "An agent instance using OpenAI Chat Completions API";

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
	@SuppressWarnings({ "unchecked", "deprecation" })
	private void setDefaultTools() {

		Builder b = defaultRequest.toBuilder();
		if (toolMap.size() == 0) { // No tools / functions used
			b.functions(JsonMissing.of());
			b.tools(JsonMissing.of());
			defaultRequest = b.build();
			return;
		}

		// Fallback in case we do not have model meta
		CallType type = modelService.getSupportedCallType(getModel(), CallType.TOOLS);
		boolean strict = modelService.supportsStrictModeToolCall(getModel(), false);

		List<Tool> tools = new ArrayList<>(toolMap.values());
		switch (type) {
		case FUNCTIONS:
			List<Function> funcs = new ArrayList<>(tools.size());
			for (Tool t : tools) {
				Function f = Function.builder() //
						.name(t.getId()) //
						.description(t.getDescription()) //
						.parameters(OpenAiUtil.toFunctionParameters(t.getParameters(), false)).build();
				funcs.add(f);
			}
			b.functions(funcs);
			b.tools(JsonMissing.of());
			break;
		case TOOLS:
			List<ChatCompletionTool> tls = new ArrayList<>(tools.size());
			for (Tool t : tools) {
				ChatCompletionTool f = ChatCompletionTool.builder() //
						.function( //
								FunctionDefinition.builder() //
										.name(t.getId()) //
										.description(t.getDescription()) //
										.strict(strict)
										.parameters(OpenAiUtil.toFunctionParameters(t.getParameters(), strict)).build() //
						).build();
				tls.add(f);
			}
			b.functions(JsonMissing.of());
			b.tools(tls);
			break;
		default:
			throw new UnsupportedOperationException("Current model does not support function calling.");
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
	private Pair<FinishReason, ChatCompletionMessage> chatCompletion(List<ChatCompletionMessageParam> messages)
			{

		// This ensures we can track last call messages from defaultRequest, for testing
		// reasons
		defaultRequest = defaultRequest.toBuilder().messages(messages).build();
		ChatCompletionCreateParams req = defaultRequest;

		com.openai.models.chat.completions.ChatCompletion resp = null;
		while (resp == null) {
			try {
				resp = endpoint.getClient().chat().completions().create(req);
			} catch (OpenAIServiceException e) {

				// Check for policy violations
				if (e.getMessage().contains("violating our usage policy")) {
					return new ImmutablePair<>(FinishReason.INAPPROPRIATE,
							ChatCompletionMessage.builder().content(e.getMessage()).build());
				}

				// Automatically recover if request is too long
				// This makes sense as req is modified only for this call (it is immutable).
				OpenAiUtil.OpenAiExceptionData d = OpenAiUtil.getExceptionData(e);
				int contextSize = modelService.getContextSize(getModel(), d.getContextSize());
				if ((contextSize > 0) && (d.getPromptLength() > 0)) {
					int optimal = contextSize - d.getPromptLength() - 1;
					if (optimal > 0) {
						LOG.warn("Reducing reply length for OpenAI completion service from "
								+ req.maxCompletionTokens().orElse(-1L) + " to " + optimal);
						req = req.toBuilder().maxCompletionTokens(optimal).build();
					} else
						throw OpenAiUtil.toEndpointException(e); // Context too small anyway
				} else
					throw OpenAiUtil.toEndpointException(e); // Not a context length issue
			}
		} // Until call succeeds

		Choice choice = resp.choices().get(0);
		return new ImmutablePair<>(OpenAiUtil.fromOpenAiApi(choice.finishReason()), choice.message());
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
	private void trimConversation(List<ChatCompletionMessageParam> messages) {

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
		OpenAiTokenizer counter = modelService.getTokenizer(getModel());
		for (int i = messages.size() - 1; i >= 0; --i) {
			if (steps >= maxConversationSteps)
				break;

			if (isMaxConversationTokensSet()) { // We do not invoke the tokenizer if we do not have to
				int tok = counter.count(messages.subList(i, messages.size()));
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
			messages.add(0, ChatCompletionMessageParam.ofDeveloper( //
					ChatCompletionDeveloperMessageParam.builder() //
							.content(personality).build() //
			));
	}

	/**
	 * Turns an ChatCompletionMessageParam returned by API into a ChatMessage.
	 */
	@SuppressWarnings("deprecation")
	private ChatMessage fromOpenAiMessage(ChatCompletionMessage msg) throws JsonProcessingException {

		if (msg.functionCall().isPresent()) {

			// The model returned a function call, transparently translate it into a message
			// with a single tool call
			List<ToolCall> calls = new ArrayList<>();
			ChatCompletionMessage.FunctionCall funCall = msg.functionCall().get();
			ToolCall toolCall;
			toolCall = ToolCall.builder() //
					.id(funCall.name()) //
					.tool(toolMap.get(funCall.name())) //
					.arguments(funCall.arguments()) //
					.build();
			calls.add(toolCall);
			return new ChatMessage(Author.BOT, calls);
		}
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
		if (msg.audio().isPresent()) { // Agent returned audio response, let's add it as a new part
			ChatCompletionAudio audio = msg.audio().get();
			parts.add(new Base64FilePart(audio.data(), audio.id(), "audio"));
			String transcript = audio.transcript();
			if ((transcript != null) && !transcript.isBlank()) {
				parts.add(new TextPart(transcript));
			}
		}

		ChatMessage result = new ChatMessage(Author.BOT, parts);
		if (msg.refusal().isPresent()) {
			result.setRefusal(msg.refusal().get());
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
	@SuppressWarnings("deprecation")
	private List<ChatCompletionMessageParam> fromChatMessage(ChatMessage msg) throws IOException {

		if (msg.hasToolCalls())
			throw new IllegalArgumentException("Only API can generate tool/function calls.");

		List<ChatCompletionMessageParam> result = new ArrayList<>();

		if (msg.hasToolCallResults()) {

			// Transparently handles function and tool calls

			List<ToolCallResult> results = msg.getToolCallResults();
			if (results.size() != msg.getParts().size())
				throw new IllegalArgumentException(
						"Tool/function call results cannot contain other parts in the message.");

			switch (modelService.getSupportedCallType(getModel())) {
			case FUNCTIONS:
				if (results.size() != 1)
					throw new IllegalArgumentException("Model supports only single function calls.");

				result.add(ChatCompletionMessageParam.ofFunction( //
						ChatCompletionFunctionMessageParam.builder() //
								.content(results.get(0).getResult().toString()) //
								.name(results.get(0).getToolId()).build() //
				));
				break;
			case TOOLS:
				for (ToolCallResult r : results) {
					result.add(ChatCompletionMessageParam.ofTool( //
							ChatCompletionToolMessageParam.builder() //
									.content(r.getResult().toString()) //
									.toolCallId(r.getToolCallId()).build() //
					));
				}
				break;
			default:
				throw new IllegalArgumentException("Model " + getModel() + " does not support function calling.");
			}

			return result;
		}

		// Check remaining parts are images or text.
		// API will fail if the model does not support them eventually (e.g. using an
		// image for a model that is not a vision model).

		List<ChatCompletionContentPart> newParts = new ArrayList<>(msg.getParts().size());
		for (MessagePart part : msg.getParts()) {

			if (part instanceof TextPart) {
				newParts.add(ChatCompletionContentPart.ofText( //
						ChatCompletionContentPartText.builder().text(part.getContent()).build() //
				));

			} else if (part instanceof FilePart) {
				FilePart file = (FilePart) part;

				switch (file.getContentType()) {
				case IMAGE:
					// We ensure the image is Base64 encoded unless it is a remote file that we do
					// not touch (for performance reasons)
					// TODO: Scale down to the biggest supported image?
					if (file.getUrl() != null) {
						newParts.add(ChatCompletionContentPart.ofImageUrl( //
								ChatCompletionContentPartImage.builder() //
										.imageUrl( //
												ChatCompletionContentPartImage.ImageUrl.builder()
														.url(file.getUrl().toString()).build() //
										).build() //
						));
					} else { // No image URL available
						if (!(file instanceof Base64FilePart))
							file = new Base64FilePart(file);

						// TODO: Scale down to the biggest supported image? Now it depends on model.
						newParts.add(ChatCompletionContentPart.ofImageUrl( //
								ChatCompletionContentPartImage.builder() //
										.imageUrl( //
												ChatCompletionContentPartImage.ImageUrl.builder()
														.url("data:" + file.getMimeType() + ";base64,"
																+ ((Base64FilePart) file).getEncodedContent())
														.build() //
										).build() //
						));
					}
					break;
				case AUDIO:
					ChatCompletionContentPartInputAudio.InputAudio.Format frmt;
					switch (file.getFormat().toLowerCase()) {
					case "wav":
						frmt = ChatCompletionContentPartInputAudio.InputAudio.Format.WAV;
						break;
					case "mp3":
						frmt = ChatCompletionContentPartInputAudio.InputAudio.Format.MP3;
						break;
					default:
						// TODO Maybe convert ?!
						throw new IllegalArgumentException("Unsupported audio format: " + file.getFormat());
					}

					if (!(file instanceof Base64FilePart))
						file = new Base64FilePart(file);

					newParts.add(ChatCompletionContentPart.ofInputAudio( //
							ChatCompletionContentPartInputAudio.builder() //
									.inputAudio( //
											ChatCompletionContentPartInputAudio.InputAudio.builder() //
													.data(((Base64FilePart) file).getEncodedContent()) //
													.format(frmt).build() //
									).build() //
					));
					break;
				default:
					newParts.add(ChatCompletionContentPart.ofFile(toFile(file)));
					break;
				}
			} else
				throw new IllegalArgumentException("Unsupported message part: " + part.getClass().getName());
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
			result.add(ChatCompletionMessageParam.ofDeveloper( //
					ChatCompletionDeveloperMessageParam.builder().contentOfArrayOfContentParts(txtParts).build() //
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

	private static ChatCompletionContentPart.File toFile(FilePart file) throws IOException {
		if (file instanceof OpenAiFilePart) {
			return ChatCompletionContentPart.File.builder() //
					.file(ChatCompletionContentPart.File.FileObject.builder() //
							.filename(file.getName()) //
							.fileId(((OpenAiFilePart) file).getFileId()).build() //
					).build();
		} else {
			if (!(file instanceof Base64FilePart))
				file = new Base64FilePart(file);

			return ChatCompletionContentPart.File.builder() //
					.file(ChatCompletionContentPart.File.FileObject.builder() //
							.filename(file.getName()) //
							.fileData("data:" + file.getMimeType() + ";base64,"
									+ ((Base64FilePart) file).getEncodedContent())
							.build() //
					).build();
		}
	}

	@Override
	public void close() {
		super.close();
		modelService.close();
	}
}