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
package io.github.mzattera.predictivepowers.anthropic.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mzattera.predictivepowers.anthropic.client.AnthropicEndpoint;
import io.github.mzattera.predictivepowers.anthropic.client.messages.Message;
import io.github.mzattera.predictivepowers.anthropic.client.messages.Message.Role;
import io.github.mzattera.predictivepowers.anthropic.client.messages.MessagesRequest;
import io.github.mzattera.predictivepowers.anthropic.client.messages.MessagesResponse;
import io.github.mzattera.predictivepowers.anthropic.services.AnthropicModelService.AnthropicTokenizer;
import io.github.mzattera.predictivepowers.services.AbstractChatService;
import io.github.mzattera.predictivepowers.services.messages.Base64FilePart;
import io.github.mzattera.predictivepowers.services.messages.ChatCompletion;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage.Author;
import io.github.mzattera.predictivepowers.services.messages.FilePart;
import io.github.mzattera.predictivepowers.services.messages.FilePart.ContentType;
import io.github.mzattera.predictivepowers.services.messages.FinishReason;
import io.github.mzattera.predictivepowers.services.messages.MessagePart;
import io.github.mzattera.predictivepowers.services.messages.TextPart;
import lombok.Getter;
import lombok.NonNull;

/**
 * ANTHROP\C chat service.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class AnthropicChatService extends AbstractChatService {

	// TODO add "slot filling" capabilities: fill a slot in the prompt based on
	// values from a Map this is done partially

	private final static Logger LOG = LoggerFactory.getLogger(AnthropicChatService.class);

	public static final String DEFAULT_MODEL = "claude-3-opus-20240229";

	private final AnthropicModelService modelService;

	public AnthropicChatService(@NonNull AnthropicEndpoint ep) {
		this(ep, DEFAULT_MODEL);
	}

	public AnthropicChatService(@NonNull AnthropicEndpoint ep, @NonNull String model) {
		this(ep, MessagesRequest.builder().model(model).build());
	}

	public AnthropicChatService(@NonNull AnthropicEndpoint ep, @NonNull MessagesRequest defaultReq) {
		this.endpoint = ep;
		this.defaultReq = defaultReq;
		this.modelService = ep.getModelService();
		setMaxConversationSteps(Integer.MAX_VALUE);
		setMaxConversationTokens(Math.min(10_000, modelService.getContextSize(defaultReq.getModel())));
		setMaxNewTokens(modelService.getMaxNewTokens(defaultReq.getModel()));
	}

	@Getter
	@NonNull
	protected final AnthropicEndpoint endpoint;

	/**
	 * This request, with its parameters, is used as default setting for each call.
	 * 
	 * You can change any parameter to change these defaults (e.g. the model used)
	 * and the change will apply to all subsequent calls.
	 */
	@Getter
	@NonNull
	private final MessagesRequest defaultReq;

	@Override
	public String getPersonality() {
		return defaultReq.getSystem();
	}

	@Override
	public void setPersonality(String personality) {
		defaultReq.setSystem(personality);
	}

	@Override
	public String getModel() {
		return defaultReq.getModel();
	}

	@Override
	public void setModel(@NonNull String model) {
		defaultReq.setModel(model);
	}

	@Override
	public Integer getTopK() {
		return defaultReq.getTopK();
	}

	@Override
	public void setTopK(Integer topK) {
		defaultReq.setTopK(topK);
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
		// Must scale from [0-1] to [0-100] considering default value as well
		if (defaultReq.getTemperature() == null)
			return null;
		return defaultReq.getTemperature() * 100.0;
	}

	@Override
	public void setTemperature(Double temperature) {
		// Must scale from [0-1] to [0-100] considering default value as well
		if (temperature == null)
			defaultReq.setTemperature(null);
		else
			defaultReq.setTemperature(temperature / 100.0d);
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
	 * provide bot instructions (personality). This is the minimum size each request
	 * to the API will take.
	 */
	public int getBaseTokens() {
		// Instructions tokens
		String cmd = getPersonality();
		if (cmd == null)
			return 0;
		return modelService.getTokenizer(getModel()).count(cmd);
	}

	private final List<Message> history = new ArrayList<>();

	@Override
	public List<? extends ChatMessage> getHistory() {
		return history.stream().map(this::fromMessage).collect(Collectors.toList());
	}

	@Override
	public void clearConversation() {
		history.clear();
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
	public ChatCompletion chat(String msg, MessagesRequest req) {
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
	public ChatCompletion chat(ChatMessage msg, MessagesRequest req) {

		Message m = fromChatMessage(msg);

		List<Message> conversation = new ArrayList<>(history);
		conversation.add(m);
		trimConversation(conversation);

		Pair<FinishReason, Message> result = chatCompletion(conversation, req);

		history.add(m);
		history.add(result.getRight());

		// Make sure history is of desired length
		int toTrim = history.size() - getMaxHistoryLength();

		if (toTrim > 0) {
			if (toTrim >= history.size())
				history.clear();
			else
				history.subList(0, toTrim).clear();
		}

		return new ChatCompletion(result.getLeft(), fromMessage(result.getRight()));
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
	public ChatCompletion complete(String prompt, MessagesRequest req) {
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
	public ChatCompletion complete(ChatMessage prompt, MessagesRequest req) {
		List<Message> msgs = new ArrayList<>();
		msgs.add(fromChatMessage(prompt));

		return complete(msgs, req);
	}

	/**
	 * Completes text outside a conversation (executes given prompt ignoring and
	 * without affecting conversation history).
	 * 
	 * Notice this does not consider or affects chat history but agent personality
	 * is used, if provided.
	 */
	public ChatCompletion complete(List<Message> messages) {
		return complete(messages, defaultReq);
	}

	/**
	 * Completes given conversation.
	 * 
	 * Notice this does not consider or affects chat history but agent personality
	 * is used, if provided.
	 */
	public ChatCompletion complete(List<Message> messages, MessagesRequest req) {
		Pair<FinishReason, Message> result = chatCompletion(messages, req);
		return new ChatCompletion(result.getLeft(), fromMessage(result.getRight()));
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
	private Pair<FinishReason, Message> chatCompletion(List<Message> messages, MessagesRequest req) {
		req.setMessages(messages);
		MessagesResponse resp = endpoint.getClient().createMessage(req);
		return new ImmutablePair<>(FinishReason.fromAnthropicApi(resp.getStopReason()), new Message(resp));
	}

	/**
	 * Trims given list of messages (typically a conversation history), so it fits
	 * the limits set in this instance (that is, maximum conversation steps and
	 * tokens).
	 *
	 * * @throws IllegalArgumentException if no message can be added because of
	 * context size limitations.
	 */
	private void trimConversation(List<Message> messages) {

		// TODO URGENT Test this

		// First trim based on history length
		int toTrim = messages.size() - getMaxConversationSteps();
		if (toTrim > 0) {
			messages.subList(0, toTrim).clear();
		}

		// Then trim the remaining messages based on number of tokens
		AnthropicTokenizer counter = modelService.getTokenizer(getModel());
		int tok = 0;
		for (toTrim = messages.size() - 1; toTrim >= 0; --toTrim) {
			int t = counter.count(messages.get(toTrim));
			if ((t + tok) > getMaxConversationTokens())
				break;
			tok += t;
		}
		++toTrim;

		// Makes sure first message of the chat is always a user message
		while ((toTrim < messages.size()) && (messages.get(toTrim).getRole() != Role.USER)) {
			++toTrim;
		}

		if (toTrim > 0) {
			if (toTrim >= messages.size())
				throw new IllegalArgumentException("Not enought space for conversation");
			else
				messages.subList(0, toTrim).clear();
		}
	}

	/**
	 * Turns a Message into a ChatMessage.
	 * 
	 * @param msg
	 * @return
	 */
	private ChatMessage fromMessage(Message msg) {
		return new ChatMessage(Author.BOT, msg.getContent());
	}

	/**
	 * This converts a generic ChatMessaege into an Message that is used for
	 * Anthrop/c API. This is meant for abstraction and easier interoperability of
	 * agents.
	 * 
	 * @throws IllegalArgumentException if the message is not in a format supported
	 *                                  directly by OpenAI API.
	 */
	private Message fromChatMessage(ChatMessage msg) {

		// TODO add test to check this

		Message result = new Message(msg.getAuthor());

		for (MessagePart part : msg.getParts()) {

			if (part instanceof TextPart) {
				result.getContent().add(part);

			} else if (part instanceof FilePart) {

				FilePart file = (FilePart) part;
				if (file.getContentType() != ContentType.IMAGE)
					throw new IllegalArgumentException("Only files with coontent type = IMAGE are supported.");

				// Parts with images are converted and scaled into base64
				try {
					result.getContent().add(Base64FilePart.forAnthropic(file));
				} catch (IOException e) {
					throw new IllegalArgumentException("Error accessing image: " + file, e);
				}

			} else {
				throw new IllegalArgumentException("Unsupported message part");
			}
		}

		return result;
	}
}