package io.github.mzattera.predictivepowers.service;

import java.util.ArrayList;
import java.util.List;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.TokenCalculator;
import io.github.mzattera.predictivepowers.openai.client.Models;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionChoice;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsResponse;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatMessage;
import lombok.Getter;

/**
 * This class manages a chat with an agent through the /chat/completions API.
 * 
 * Chat history is kept in memory and updated as chat progresses. At the same
 * time, methods to use chat service as a simple completion service are
 * provided.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class ChatService extends CompletionService {

	// TODO: revert back to using completion not chat completion

	// TODO add "slot filling" capabilities: fill a slot in the prompt based on
	// values from a Map

	/**
	 * These are the messages exchanged in the current chat.
	 * 
	 * They can be manipulated in order to alter chat flow.
	 * 
	 * Notice this will grow indefinitely, unless cleared. Only latest messages
	 * however are considered when calling the API (see
	 * {@link maxConversationLength}).
	 */
	// TODO Size limit?
	@Getter
	private final List<ChatMessage> history = new ArrayList<>();

	/**
	 * Maximum number of steps in the conversation to consider when interacting with
	 * chat service.
	 * 
	 * Notice this does NOT limit length of conversation history, which is
	 * unlimited, but it limits the number of turns considered when calling the API,
	 * thus limiting the agent memory. This is needed to avoid exceeding prompt
	 * length when calling the API.
	 */
	@Getter
	private int maxConversationSteps = 14;

	// TODO add max history length limits as well

	public void setMaxConversationSteps(int l) {
		if (l < 1)
			throw new IllegalArgumentException("Must keep at least 1 message.");
		maxConversationSteps = l;
	}

	/**
	 * Maximum number of token to keep in the conversation when interacting with
	 * chat service.
	 * 
	 * Notice this does NOT limit length of conversation history, which is
	 * unlimited, but it limits number of turns considered when calling the API,
	 * thus limiting the agent memory. THis is needed to avoid exceeding prompt
	 * length when calling the API.
	 * 
	 * As there is no Java code available for an exact calculation, this is
	 * approximated.
	 */
	@Getter
	private int maxConversationTokens = 3 * 1024;

	public void setMaxConversationTokens(int n) {
		if (n < 1)
			throw new IllegalArgumentException("Must keep at least 1 token.");
		maxConversationTokens = n;
	}

	public ChatService(OpenAiEndpoint ep, ChatCompletionsRequest defaultReq) {
		super(ep, defaultReq);
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
		return chat(msg, getDefaultReq());
	}

	/**
	 * Continues current chat, with the provided message.
	 * 
	 * The exchange is added to the conversation history.
	 */
	public TextResponse chat(String msg, ChatCompletionsRequest req) {

		history.add(new ChatMessage("user", msg));
		List<ChatMessage> messages = trimChat(history);
		req.setMessages(messages);

		// Adjust token limit if needed
		if ((req.getMaxTokens() == null) && (Models.getContextSize(req.getModel()) != -1)) {
			int tok = 0;
			for (ChatMessage m : messages)
				tok += TokenCalculator.count(m);
			req.setMaxTokens(Models.getContextSize(req.getModel()) - tok);
		}

		// TODO is this error handling good? It should in principle as if we cannot get
		// this text, something went wrong and we should react.
		// TODO BETTER USE finish reason.
		ChatCompletionsResponse resp = ep.getClient().createChatCompletion(req);
		ChatCompletionChoice choice = resp.getChoices()[0];
		ChatMessage message = choice.getMessage();
		history.add(message);

		return TextResponse.fromGptApi(message.getContent(), choice.getFinishReason());
	}

	/**
	 * Trims given conversation history, so it fits the limits set in the class.
	 * 
	 * @return A new conversation, including agent personality and as many messages
	 *         as can fit, civen current limits.
	 */
	private List<ChatMessage> trimChat(List<ChatMessage> messages) {
		List<ChatMessage> result = new ArrayList<>(messages.size());

		int numTokens = 0;
		if (getPersonality() != null) {
			ChatMessage m = new ChatMessage("system", getPersonality());
			result.add(m);
			numTokens = TokenCalculator.count(m);
		}

		int numMsg = 0;
		for (int i = messages.size() - 1; i >= 0; --i) {
			if (numMsg >= maxConversationSteps)
				break;

			ChatMessage msg = messages.get(i);
			int t = TokenCalculator.count(msg);
			if ((numMsg > 0) && ((numTokens + t) > maxConversationTokens))
				break;

			result.add(1, msg);
			++numMsg;
			numTokens += t;
		}

		return result;
	}
}
