package io.github.mzattera.predictivepowers.service;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsChoice;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsResponse;
import io.github.mzattera.predictivepowers.openai.util.ModelUtil;
import io.github.mzattera.predictivepowers.openai.util.TokenUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * This class does completions (prompt execution).
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@RequiredArgsConstructor
public class CompletionService {

	// TODO add "slot filling" capabilities: fill a slot in the prompt based on
	// values from a Map

	public CompletionService(OpenAiEndpoint ep) {
		this(ep, new CompletionsRequest());
		defaultReq.setModel("text-davinci-003");
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
	protected final CompletionsRequest defaultReq;

	/**
	 * Completes text (executes given prompt).
	 * 
	 * It uses parameters specified in {@link #getDefaultReq()}.
	 */
	public TextResponse complete(String prompt) {
		return insert(prompt, null, defaultReq);
	}

	/**
	 * Completes text (executes given prompt); uses provided
	 * {@link CompletionsRequest} to get parameters for the call.
	 * 
	 * Notice that if maxToxens is null, this method will try to set it
	 * automatically, based on prompt length.
	 */
	public TextResponse complete(String prompt, CompletionsRequest req) {
		return insert(prompt, null, req);
	}

	/**
	 * Inserts text between given prompt and the suffix (executes given prompt).
	 * 
	 * It uses {@link #getDefaultReq()} parameters.
	 */
	public TextResponse insert(String prompt, String suffix) {
		return insert(prompt, suffix, defaultReq);
	}

	/**
	 * Inserts text between given prompt and the suffix (executes given prompt).
	 * uses provided {@link CompletionsRequest} to get parameters for the call.
	 * 
	 * Notice that if maxToxens is null, this method will try to set it
	 * automatically, based on prompt length.
	 */
	public TextResponse insert(String prompt, String suffix, CompletionsRequest req) {
		req.setPrompt(prompt);
		req.setSuffix(suffix);

		// Adjust token limit if needed
		boolean autofit = (req.getMaxTokens() == null) && (ModelUtil.getContextSize(req.getModel()) != -1);
		try {
			if (autofit) {
				int tok = TokenUtil.count(prompt);
				if (suffix != null)
					tok += TokenUtil.count(suffix);
				req.setMaxTokens(ModelUtil.getContextSize(req.getModel()) - tok);
			}

			// TODO: catch exception if maxToken is too high, parse prompt token length and
			// resubmit, optionally
			CompletionsResponse resp = ep.getClient().createCompletion(req);
			CompletionsChoice choice = resp.getChoices().get(0);
			return TextResponse.fromGptApi(choice.getText(), choice.getFinishReason());

		} finally {

			// for next call, or maxTokens will remain fixed
			if (autofit)
				req.setMaxTokens(null);
		}
	}
}
