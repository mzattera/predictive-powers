package io.github.mzattera.predictivepowers.service;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.Models;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsChoice;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsResponse;
import io.github.mzattera.predictivepowers.openai.util.TokenUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

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

	// TODO
	/*
	 * Best practices Inserting text is a new feature in beta and you may have to
	 * modify the way you use the API for better results. Here are a few best
	 * practices:
	 */
	
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
	private final CompletionsRequest defaultReq;

	/**
	 * Completes text (executes given prompt).
	 * 
	 * It uses {@link #getDefaultReq()} parameters.
	 */
	public TextResponse complete(String prompt) {
		return complete(prompt, defaultReq);
	}

	/**
	 * Completes text (executes given prompt); uses provided
	 * {@link CompletionsRequest} to get parameters for the call.
	 * 
	 * Notice that if maxToxens is null, this method will try to set it
	 * automatically, based on prompt length.
	 */
	public TextResponse complete(String prompt, CompletionsRequest req) {
		req.setPrompt(prompt);
		req.setSuffix(null);

		// Adjust token limit if needed
		if ((req.getMaxTokens() == null) && (Models.getContextSize(req.getModel()) != -1)) {
			req.setMaxTokens(Models.getContextSize(req.getModel()) - TokenUtil.count(prompt) - 10);
		}

		// TODO is this error handling good? It should in principle as if we cannot get
		// this text, something went wrong and we should react.
		// TODO BETTER USE finish reason.
		// TODO: catch exception if maxToken is too high, parse prompt token length and
		// resubmit, optionally
		CompletionsResponse resp = ep.getClient().createCompletion(req);
		CompletionsChoice choice = resp.getChoices().get(0);
		return TextResponse.fromGptApi(choice.getText(), choice.getFinishReason());
	}
}
