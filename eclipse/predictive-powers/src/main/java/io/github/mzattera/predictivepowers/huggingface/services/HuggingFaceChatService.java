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
package io.github.mzattera.predictivepowers.huggingface.services;

import java.util.ArrayList;
import java.util.List;

import io.github.mzattera.predictivepowers.huggingface.client.Options;
import io.github.mzattera.predictivepowers.huggingface.client.nlp.ConversationalRequest;
import io.github.mzattera.predictivepowers.huggingface.client.nlp.ConversationalResponse;
import io.github.mzattera.predictivepowers.huggingface.endpoint.HuggingFaceEndpoint;
import io.github.mzattera.predictivepowers.services.AbstractChatService;
import io.github.mzattera.predictivepowers.services.ChatCompletion;
import io.github.mzattera.predictivepowers.services.ChatMessage;
import io.github.mzattera.predictivepowers.services.ChatMessage.Author;
import io.github.mzattera.predictivepowers.services.TextCompletion.FinishReason;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Hugging Face based chat service.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class HuggingFaceChatService extends AbstractChatService {

//	public static final String DEFAULT_MODEL = "microsoft/DialoGPT-medium";
	public static final String DEFAULT_MODEL = "facebook/blenderbot-400M-distill";

	public HuggingFaceChatService(HuggingFaceEndpoint ep) {
		this(ep, ConversationalRequest.builder().options(Options.builder().waitForModel(true).build()).build());
	}

	public HuggingFaceChatService(HuggingFaceEndpoint ep, ConversationalRequest defaultReq) {
		super(ep.getModelService());
		this.endpoint = ep;
		this.defaultReq = defaultReq;
	}

	@NonNull
	@Getter
	protected final HuggingFaceEndpoint endpoint;

	/**
	 * This request, with its parameters, is used as default setting for each call.
	 * 
	 * You can change any parameter to change these defaults (e.g. the model used)
	 * and the change will apply to all subsequent calls.
	 * 
	 * Getters and setters that change servie parameters, also change this object
	 * fields.
	 */
	@Getter
	@NonNull
	private final ConversationalRequest defaultReq;

	@Getter
	@Setter
	private String model = DEFAULT_MODEL;

	@Override
	public Integer getTopK() {
		return defaultReq.getParameters().getTopK();
	}

	@Override
	public void setTopK(Integer topK) {
		defaultReq.getParameters().setTopK(topK);
	}

	@Override
	public Double getTopP() {
		return defaultReq.getParameters().getTopP();
	}

	@Override
	public void setTopP(Double topP) {
		defaultReq.getParameters().setTopP(topP);
	}

	@Override
	public Double getTemperature() {
		return defaultReq.getParameters().getTemperature();
	}

	@Override
	public void setTemperature(Double temperature) {
		defaultReq.getParameters().setTemperature(temperature);
	}

	@Override
	public Integer getMaxNewTokens() {
		return defaultReq.getParameters().getMaxLength();
	}

	@Override
	public void setMaxNewTokens(Integer maxNewTokens) {
		defaultReq.getParameters().setMaxLength(maxNewTokens);
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
	public ChatCompletion chat(String msg, ConversationalRequest req) {

		ChatCompletion resp = chatCompletion(msg, trimConversation(getHistory()), req);
		getHistory().add(new ChatMessage(Author.USER, msg));
		getHistory().add(new ChatMessage(Author.BOT, resp.getText()));

		// Make sure history is of desired length
		while (history.size() > getMaxHistoryLength())
			history.remove(0);

		return resp;
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
	public ChatCompletion chat(ChatMessage msg, ConversationalRequest req) {
		return chat(msg.getContent(), req);
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
	public ChatCompletion complete(String prompt, ConversationalRequest req) {
		return chatCompletion(prompt, new ArrayList<>(), req);
	}

	@Override
	public ChatCompletion complete(ChatMessage prompt) {
		return complete(prompt.getContent(), defaultReq);
	}

	/**
	 * Completes text (executes given prompt).
	 * 
	 * Notice this does not consider or affects chat history but agent personality
	 * is used, if provided.
	 */
	public ChatCompletion complete(ChatMessage prompt, ConversationalRequest req) {
		return complete(prompt.getContent(), req);
	}

	@Override
	public ChatCompletion complete(List<ChatMessage> messages) {
		return complete(messages, defaultReq);
	}

	/**
	 * Completes given conversation.
	 * 
	 * Notice this does not consider or affects chat history.
	 */
	public ChatCompletion complete(List<ChatMessage> messages, ConversationalRequest req) {

		// We assume last message in the list is current user utterance to be answered

		if (messages.size() == 0)
			return ChatCompletion.builder() //
					.message(ChatMessage.builder().content("").author(Author.BOT).build()) //
					.finishReason(FinishReason.COMPLETED) //
					.build();

		// We split first, it is more complicated but in this way we ensure we support
		// cases with multiple user messages in sequence, including at the end of
		// conversation
		List<String>[] history = buildInputs(messages);
		String msg = history[0].remove(history[0].size() - 1);

		return chatCompletion(msg, history, req);
	}

	/**
	 * Completes given conversation.
	 * 
	 * Notice this does not consider or affects chat history. In addition, agent
	 * personality is NOT considered.
	 * 
	 * @param msg      Last user message, to be replied to.
	 * @param messages Past conversation so far.
	 */
	protected ChatCompletion chatCompletion(String msg, List<ChatMessage> messages, ConversationalRequest req) {
		return chatCompletion(msg, buildInputs(messages), req);
	}

	/**
	 * Completes given conversation.
	 * 
	 * Notice this does not consider or affects chat history. In addition, agent
	 * personality is NOT considered.
	 * 
	 * @param msg     Last user message, to be replied to.
	 * @param history Two Lists with user utterances and corresponding bot replies,
	 *                respectively.
	 */
	protected ChatCompletion chatCompletion(String msg, List<String>[] history, ConversationalRequest req) {

		if (history[0].size() != history[1].size())
			throw new IllegalArgumentException("Conversation history must have a reply for each user message");
		req.getInputs().setPastUserInputs(history[0]);
		req.getInputs().setGeneratedResponses(history[1]);
		req.getInputs().setText(msg);

		ConversationalResponse resp = endpoint.getClient().conversational(getModel(), req);

		return ChatCompletion.builder() //
				.message(ChatMessage.builder().content(resp.getGeneratedText()).author(Author.BOT).build()) //
				.finishReason(FinishReason.COMPLETED) //
				.build();
	}

	/**
	 * Splits given conversation into two Lists with user utterances and
	 * corresponding bot replies, respectively, as required by Hugging Face API.
	 * 
	 * Notice it also enforce respect of parameters such as max conversation length
	 */
	protected List<String>[] buildInputs(List<ChatMessage> msgs) {

		@SuppressWarnings("unchecked")
		List<String>[] result = new ArrayList[2];
		result[0] = new ArrayList<String>(msgs.size());
		result[1] = new ArrayList<String>(msgs.size());

		if (msgs.size() == 0)
			return result;

		// Get to first user message, in case history has beenshortened
		int i = 0;
		for (; (i < msgs.size()) && (msgs.get(i).getAuthor() != Author.USER); ++i)
			;

		Author lastAuthor = Author.USER;
		StringBuilder sb = new StringBuilder();
		for (; i < msgs.size(); ++i) {
			ChatMessage m = msgs.get(i);

			if (m.getAuthor() != lastAuthor) { // must save conversation we accumulated so far
				switch (m.getAuthor()) {
				case USER:
					result[1].add(sb.toString());
					break;
				case BOT:
					result[0].add(sb.toString());
					break;
				default:
					throw new IllegalArgumentException("Role not supported");
				}

				lastAuthor = m.getAuthor();
				sb.setLength(0);
			}

			// save this message in the buffer
			if (sb.length() > 0)
				sb.append('\n');
			sb.append(m.getContent() == null ? "" : m.getContent());

		} // for each message

		// Last message
		if (sb.length() > 0) { // must save conversation we accumulated so far
			switch (lastAuthor) {
			case USER:
				result[0].add(sb.toString());
				break;
			case BOT:
				result[1].add(sb.toString());
				break;
			default:
				throw new IllegalArgumentException("Role not supported");
			}
		}

		return result;
	}
}