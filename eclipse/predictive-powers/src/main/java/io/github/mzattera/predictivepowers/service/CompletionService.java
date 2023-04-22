package io.github.mzattera.predictivepowers.service;

import java.util.ArrayList;
import java.util.List;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.Models;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsChoice;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsResponse;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatMessage;
import io.github.mzattera.predictivepowers.util.TokenCalculator;
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

	// TODO: revert back to using completion not chat completion

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

	// TODO: catch exception if maxToken is too high, parse prompt token length and
	// resubmit, optionally

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
	 * The agent personality is NOT considered, but can be injected as first
	 * message.
	 */
	public TextResponse complete(List<ChatMessage> messages) {
		return complete(messages, defaultReq);
	}

	/**
	 * Completes given conversation.
	 * 
	 * The agent personality is NOT considered, but can be injected as first
	 * message.
	 */
	public TextResponse complete(List<ChatMessage> messages, ChatCompletionsRequest req) {

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
		ChatCompletionsChoice choice = resp.getChoices()[0];
		return TextResponse.fromGptApi(choice.getMessage().getContent(), choice.getFinishReason());
	}

}
