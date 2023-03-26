package io.github.mzattera.predictivepowers;

import java.util.ArrayList;
import java.util.List;

import io.github.mzattera.predictivepowers.client.openai.chatcompletion.ChatCompletionChoice;
import io.github.mzattera.predictivepowers.client.openai.chatcompletion.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.client.openai.chatcompletion.ChatCompletionsResponse;
import io.github.mzattera.predictivepowers.client.openai.chatcompletion.ChatMessage;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

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
@RequiredArgsConstructor
public class ChatService {

	@NonNull
	private final OpenAiEndpoint ep;

	/**
	 * This request, with its parameters, is used as default setting for each call.
	 * 
	 * You can change any parameter to change these defaults (e.g. the model used)
	 * and the change will apply to all subsequent calls.
	 */
	@Getter
	@NonNull
	private final ChatCompletionsRequest defaultReq;

	/** Personality of the agent. If null, agent has NO personality. */
	@Getter
	@Setter
	private String personality = "You are a helpful assistant.";

	/**
	 * These are the messages exchanged in the current chat.
	 * 
	 * They can be manipulated in order to alter chat flow.
	 * 
	 * Notice this will grow indefinitely, unless cleared. Only latest messages
	 * however are considered when calling the API (see
	 * {@link maxConversationLength}).
	 */
	@Getter
	private final List<ChatMessage> history = new ArrayList<>();

	/**
	 * Maximum number of steps in the conversation to consider when interacting with
	 * chat service.
	 * 
	 * Notice this does NOT limit length of conversation history, which is
	 * unlimited, but it limits the number of turns considered when calling the API,
	 * thus limiting the agent memory. THis is needed to avoid exceeding prompt
	 * length when calling the API.
	 */
	@Getter
	private int maxConversationLength = 14;

	public void setMaxConversationLengh(int l) {
		if (l < 1)
			throw new IllegalArgumentException("Must keep at least 1 message.");
		maxConversationLength = l;
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
	private int maxConversationTokens = 4096;

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
		return chat(msg, (ChatCompletionsRequest) defaultReq.clone());
	}

	/**
	 * Continues current chat, with the provided message.
	 * 
	 * The exchange is added to the conversation history.
	 */
	public TextResponse chat(String msg, ChatCompletionsRequest req) {

		history.add(new ChatMessage("user", msg));
		req.setMessages(trimChat(history));

		System.out.println(req.getMessages().toString());

		// TODO is this error handling good? It should in principle as if we cannot get
		// this text, something went wrong and we should react.
		// TODO BETTER USE finish reason.
		ChatCompletionsResponse resp = ep.getClient().createChatCompletion(req);
		ChatCompletionChoice choice = resp.getChoices().get(0);
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

		if (personality != null) {
			result.add(new ChatMessage("system", personality));
		}

		int numMsg = 0;
		int numTokens = TokenCalculator.count(result.get(0));
		for (int i = messages.size() - 1; i >= 0; --i) {
			if (numMsg >= maxConversationLength)
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

	/**
	 * Executes a one-turn interaction outside the current conversation. The agent
	 * personality is still considered, but the current conversation is not
	 * affected.
	 * 
	 * Basically, this is using chat API as a text completion service.
	 */
	public TextResponse complete(String prompt) {
		return complete(prompt, (ChatCompletionsRequest) defaultReq.clone());
	}

	/**
	 * Executes a one-turn interaction outside the current conversation. The agent
	 * personality is still considered, but the current conversation is not
	 * affected.
	 * 
	 * Basically, this is using chat API as a text completion service.
	 */
	public TextResponse complete(String prompt, ChatCompletionsRequest req) {
		List<ChatMessage> msg = new ArrayList<>();
		if (personality != null)
			msg.add(new ChatMessage("system", personality));
		msg.add(new ChatMessage("user", prompt));
		req.setMessages(msg);

		// TODO is this error handling good? It should in principle as if we cannot get
		// this text, something went wrong and we should react.
		// TODO BETTER USE finish reason.
		ChatCompletionsResponse resp = ep.getClient().createChatCompletion(req);
		ChatCompletionChoice choice = resp.getChoices().get(0);
		return TextResponse.fromGptApi(choice.getMessage().getContent(), choice.getFinishReason());
	}

}
