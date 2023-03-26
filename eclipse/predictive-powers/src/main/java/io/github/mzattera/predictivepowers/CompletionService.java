/**
 * 
 */
package io.github.mzattera.predictivepowers;

import io.github.mzattera.predictivepowers.client.openai.completions.CompletionsChoice;
import io.github.mzattera.predictivepowers.client.openai.completions.CompletionsRequest;
import io.github.mzattera.predictivepowers.client.openai.completions.CompletionsResponse;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * A class that exposes text completion services (prompt execution).
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

	public TextResponse complete(String prompt) {
		return complete(prompt, (CompletionsRequest) defaultReq.clone());
	}

	public TextResponse complete(String prompt, CompletionsRequest req) {
		req.setPrompt(prompt);

		// TODO is this error handling good? It should in principle as if we cannot get
		// this text, something went wrong and we should react.
		// TODO BETTER USE finish reason.
		CompletionsResponse resp = ep.getClient().createCompletion(req);
		CompletionsChoice c = resp.getChoices().get(0);
		return TextResponse.fromGptApi(c.getText().trim(), c.getFinishReason());
	}
}
