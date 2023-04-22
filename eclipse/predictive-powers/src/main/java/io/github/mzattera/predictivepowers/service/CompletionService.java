package io.github.mzattera.predictivepowers.service;

import java.util.ArrayList;
import java.util.List;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.TokenCalculator;
import io.github.mzattera.predictivepowers.client.openai.chat.ChatCompletionChoice;
import io.github.mzattera.predictivepowers.client.openai.chat.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.client.openai.chat.ChatCompletionsResponse;
import io.github.mzattera.predictivepowers.client.openai.chat.ChatMessage;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * This class does completions (propmt execution) through the /chat/completions
 * API.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@RequiredArgsConstructor
public class CompletionService {

	// TODO add "slot filling" capabilities: fill a slot in the prompt based on
	// values from a Map

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
	private final ChatCompletionsRequest defaultReq;

	/** Personality of the agent. If null, agent has NO personality. */
	@Getter
	@Setter
	private String personality = "You are a helpful assistant.";

	/**
	 * Completes text (executes given prompt). The agent personality is considered,
	 * if provided.
	 */
	public TextResponse complete(String prompt) {
		return complete(prompt, defaultReq);
	}

	/**
	 * Completes text (executes given prompt). The agent personality is considered,
	 * if provided.
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
	 * The agent personality is NOT considered, put can be injected as first
	 * message.
	 */
	public TextResponse complete(List<ChatMessage> messages) {
		return complete(messages, defaultReq);
	}

	/**
	 * Completes given conversation.
	 * 
	 * The agent personality is NOT considered, put can be injected as first
	 * message.
	 */
	public TextResponse complete(List<ChatMessage> messages, ChatCompletionsRequest req) {
		req = (ChatCompletionsRequest) req.clone();
		req.setMessages(messages);

		// TODO do we need to do this?
		// Adjust token limit
		int tok = 0;
		for (ChatMessage m : messages)
			tok += TokenCalculator.count(m);
		req.setMaxTokens(req.getMaxTokens() - tok);

		// TODO is this error handling good? It should in principle as if we cannot get
		// this text, something went wrong and we should react.
		// TODO BETTER USE finish reason.
		ChatCompletionsResponse resp = ep.getClient().createChatCompletion(req);
		ChatCompletionChoice choice = resp.getChoices().get(0);
		return TextResponse.fromGptApi(choice.getMessage().getContent(), choice.getFinishReason());
	}

}
