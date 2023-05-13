package io.github.mzattera.predictivepowers.services;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsChoice;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsResponse;
import io.github.mzattera.predictivepowers.openai.util.ModelUtil;
import io.github.mzattera.predictivepowers.openai.util.TokenUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * This class manages a chat with an agent.
 * 
 * Chat history is kept in memory and updated as chat progresses. At the same
 * time, methods to use chat service as a simple completion service are
 * provided.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class ChatService {

	// TODO add "slot filling" capabilities: fill a slot in the prompt based on
	// values from a Map

	public ChatService(OpenAiEndpoint ep) {
		this(ep, new ChatCompletionsRequest());
		defaultReq.setModel("gpt-3.5-turbo");
		maxConversationTokens = Math.max(ModelUtil.getContextSize(defaultReq.getModel()), 2046) * 3 / 4;
	}

	public ChatService(OpenAiEndpoint ep, ChatCompletionsRequest defaultReq) {
		this.ep = ep;
		this.defaultReq = defaultReq;
		maxConversationTokens = Math.max(ModelUtil.getContextSize(defaultReq.getModel()), 2046) * 3 / 4;
	}

	@NonNull
	protected final OpenAiEndpoint ep;

	/**
	 * This request, with its parameters, is used as default setting for each call.
	 * 
	 * You can change any parameter to change these defaults (e.g. the model used)
	 * and the change will apply to all subsequent calls.
	 */
	@Getter
	@NonNull
	protected final ChatCompletionsRequest defaultReq;

	/**
	 * These are the messages exchanged in the current chat.
	 * 
	 * They can be manipulated in order to alter chat flow.
	 * 
	 * Notice this will grow up to {@link #maxHistoryLength}, unless cleared.
	 * However, only latest messages are considered when calling the API (see
	 * {@link maxConversationLength}).
	 */
	@Getter
	protected final List<ChatMessage> history = new ArrayList<>();

	@Getter
	@Setter
	private int maxHistoryLength = 100;

	/** Personality of the agent. If null, agent has NO personality. */
	@Getter
	@Setter
	private String personality = null;

	/**
	 * Maximum number of steps in the conversation to consider when interacting with
	 * chat service.
	 * 
	 * Notice this does NOT limit length of conversation history (see
	 * {@link #maxHistoryLength}).
	 */
	@Getter
	private int maxConversationSteps = 14;

	public void setMaxConversationSteps(int l) {
		if (l < 1)
			throw new IllegalArgumentException("Must keep at least 1 message.");
		maxConversationSteps = l;
	}

	/**
	 * Maximum number of tokens to keep in conversation steps when interacting with
	 * chat service.
	 * 
	 * The higher this parameter, the smaller the allowed size of the reply.
	 * 
	 * As there is no Java code available for an exact calculation, this is
	 * approximated.
	 */
	@Getter
	private int maxConversationTokens;

	public void setMaxConversationTokens(int n) {
		if (n < 1)
			throw new IllegalArgumentException("Must keep at least 1 token.");
		maxConversationTokens = n;
	}

	/**
	 * Starts a new chat, clearing current conversation.
	 */
	public void clearConversation() {
		history.clear();
	}

	/**
	 * Continues current chat, with the provided message.
	 * 
	 * The exchange is added to the conversation history.
	 */
	public TextResponse chat(String msg) {
		return chat(msg, defaultReq);
	}

	/**
	 * Continues current chat, with the provided message.
	 * 
	 * The exchange is added to the conversation history.
	 */
	public TextResponse chat(String msg, ChatCompletionsRequest req) {

		// Update history
		history.add(new ChatMessage("user", msg));

		try {

			ChatCompletionsChoice choice = chatCompletion(trimChat(history), req);
			TextResponse result = TextResponse.fromGptApi(choice.getMessage().getContent(), choice.getFinishReason());
			history.add(choice.getMessage());
			return result;

		} catch (Exception e) {
			// remove last message from history
			history.remove(history.size() - 1);

			throw e;
		} finally {
			// Make sure history is of desired length
			while (history.size() > maxHistoryLength)
				history.remove(0);
		}
	}

	/**
	 * Trims given conversation history, so it fits the limits set in this instance.
	 * 
	 * @return A new conversation, including agent personality and as many messages
	 *         as can fit, given current settings.
	 */
	private List<ChatMessage> trimChat(List<ChatMessage> messages) {
		List<ChatMessage> result = new ArrayList<>(messages.size());

		int numTokens = 3; // these are in the chat response, always
		if (getPersonality() != null) {
			ChatMessage m = new ChatMessage("system", getPersonality());
			result.add(m);
			numTokens = TokenUtil.count(m);
		}

		int numMsg = 0;
		for (int i = messages.size() - 1; i >= 0; --i) {
			if (numMsg >= maxConversationSteps)
				break;

			ChatMessage msg = messages.get(i);
			int t = TokenUtil.count(msg);
			if ((numMsg > 0) && ((numTokens + t) > maxConversationTokens))
				break;

			if (getPersonality() != null) {
				// Skip "system" message when inserting
				result.add(1, msg);
			} else {
				result.add(0, msg);
			}
			++numMsg;
			numTokens += t;
		}

		return result;
	}

	/**
	 * Completes text (executes given prompt). The agent personality is considered,
	 * if provided.
	 */
	public TextResponse complete(String prompt) {
		return complete(prompt, defaultReq);
	}

	/**
	 * Completes text (executes given prompt).
	 * 
	 * Notice this does not consider or affects chat history but agent personality
	 * is used, if provided.
	 */
	public TextResponse complete(String prompt, ChatCompletionsRequest req) {
		List<ChatMessage> msg = new ArrayList<>();
		if (personality != null)
			msg.add(new ChatMessage("system", personality));
		msg.add(new ChatMessage("user", prompt));

		return complete(msg, req);
	}

	/**
	 * Completes given conversation.
	 * 
	 * Notice this does not consider or affects chat history but agent personality
	 * is used, if provided.
	 */
	public TextResponse complete(List<ChatMessage> messages) {
		return complete(messages, defaultReq);
	}

	/**
	 * Completes given conversation.
	 * 
	 * Notice this does not consider or affects chat history. In addition, agent
	 * personality is NOT considered, but can be injected as first message in the
	 * list.
	 */
	public TextResponse complete(List<ChatMessage> messages, ChatCompletionsRequest req) {
		ChatCompletionsChoice choice = chatCompletion(messages, req);
		return TextResponse.fromGptApi(choice.getMessage().getContent(), choice.getFinishReason());
	}

	/**
	 * Completes given conversation.
	 * 
	 * Notice this does not consider or affects chat history. In addition, agent
	 * personality is NOT considered, but can be injected as first message in the
	 * list.
	 */
	private ChatCompletionsChoice chatCompletion(List<ChatMessage> messages, ChatCompletionsRequest req) {

		req.setMessages(messages);

		// Adjust token limit if needed
		boolean autofit = (req.getMaxTokens() == null) && (ModelUtil.getContextSize(req.getModel()) != -1);
		try {
			if (autofit) {
				int tok = TokenUtil.count(messages);
				req.setMaxTokens(ModelUtil.getContextSize(req.getModel()) - tok - 10);
			}
			
			// TODO: catch exception if maxToken is too high, parse prompt token length and
			// resubmit, optionally
			ChatCompletionsResponse resp = ep.getClient().createChatCompletion(req);
			return resp.getChoices().get(0);

		} finally {

			// for next call, or maxTokens will remain fixed
			if (autofit)
				req.setMaxTokens(null);
		}
	}
}