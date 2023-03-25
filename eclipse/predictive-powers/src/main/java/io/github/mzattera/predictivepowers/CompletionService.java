/**
 * 
 */
package io.github.mzattera.predictivepowers;

import io.github.mzattera.predictivepowers.client.openai.completions.CompletionsParameters;
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

	@Getter
	@NonNull
	private final CompletionsParameters defaultParams;

	public String complete(String prompt) {
		return complete(prompt, defaultParams);
	}

	public String complete(String prompt, CompletionsParameters params) {

		CompletionsRequest req = CompletionsRequest.fromParameters(params);
		req.setModel("text-davinci-003");
		req.setMaxTokens(256);
		req.setPrompt(prompt);

		CompletionsResponse resp = ep.getClient().createCompletion(req);
		if (resp.getChoices().size() == 0)
			return "";

		return resp.getChoices().get(0).getText().trim();
	}
}
