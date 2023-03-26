/**
 * 
 */
package io.github.mzattera.predictivepowers;

import io.github.mzattera.predictivepowers.client.openai.completions.CompletionsRequest;
import io.github.mzattera.predictivepowers.client.openai.completions.CompletionsResponse;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * A class that exposes model inventory services.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@RequiredArgsConstructor
public class CompletionService {

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
	private final CompletionsRequest defaultReq;

	public String complete(String prompt) {
		return complete(prompt, defaultReq);
	}

	public String complete(String prompt, CompletionsRequest params) {

		CompletionsRequest req = (CompletionsRequest) defaultReq.clone();
		req.setModel("text-davinci-003");
		req.setMaxTokens(256);
		req.setPrompt(prompt);

		CompletionsResponse resp = ep.getClient().createCompletion(req);
		if (resp.getChoices().size() == 0)
			return "";

		return resp.getChoices().get(0).getText().trim();
	}
}
