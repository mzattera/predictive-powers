/*
 * Copyright 2023 Massimiliano "Maxi" Zattera
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.mzattera.predictivepowers.openai.services;

import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsChoice;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsResponse;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.CompletionService;
import io.github.mzattera.predictivepowers.services.ModelService;
import io.github.mzattera.predictivepowers.services.ModelService.Tokenizer;
import io.github.mzattera.predictivepowers.services.TextResponse;
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
public class OpenAiCompletionService implements CompletionService {

	public static final String DEFAULT_MODEL = "text-davinci-003";

	public OpenAiCompletionService(OpenAiEndpoint ep) {
		this(ep, CompletionsRequest.builder().model(DEFAULT_MODEL).echo(false).n(1).build());
	}

	@NonNull
	@Getter
	protected final OpenAiEndpoint endpoint;

	/**
	 * This request, with its parameters, is used as default setting for each call.
	 * 
	 * You can change any parameter to change these defaults (e.g. the model used)
	 * and the change will apply to all subsequent calls.
	 * 
	 * Getters and setters from {@link CompletionService} will use values in this
	 * request.
	 */
	@Getter
	@NonNull
	private final CompletionsRequest defaultReq;

	@Override
	public String getModel() {
		return defaultReq.getModel();
	}

	@Override
	public void setModel(@NonNull String model) {
		defaultReq.setModel(model);
	}

	@Override
	public Integer getTopK() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setTopK(Integer topK) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Double getTopP() {
		return defaultReq.getTopP();
	}

	@Override
	public void setTopP(Double topP) {
		defaultReq.setTopP(topP);
	}

	@Override
	public Double getTemperature() {
		// Must scale from [0-2] to [0-100] considering default value as well
		if (defaultReq.getTemperature() == null)
			return null;
		return defaultReq.getTemperature() * 50;
	}

	@Override
	public void setTemperature(Double temperature) {
		// Must scale from [0-2] to [0-100] considering default value as well
		if (temperature == null)
			defaultReq.setTemperature(null);
		else
			defaultReq.setTemperature(temperature / 50);
	}

	/**
	 * This service will try to calculate this so to allow the longest possible
	 * output, if it is null.
	 */
	@Override
	public Integer getMaxNewTokens() {
		return defaultReq.getMaxTokens();
	}

	/**
	 * This service will try to calculate this so to allow the longest possible
	 * output, if it is null.
	 */
	@Override
	public void setMaxNewTokens(Integer maxNewTokens) {
		defaultReq.setMaxTokens(maxNewTokens);
	}

	@Override
	public boolean getEcho() {
		if (defaultReq.getEcho() == null)
			return false;
		return defaultReq.getEcho();
	}

	@Override
	public void setEcho(boolean echo) {
		defaultReq.setEcho(echo);
	}

	/**
	 * Completes text (executes given prompt).
	 * 
	 * It uses parameters specified in {@link #getDefaultReq()}.
	 */
	@Override
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
	@Override
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
		ModelService ms = endpoint.getModelService();
		String model = req.getModel();
		boolean autofit = (req.getMaxTokens() == null) && (ms.getContextSize(model, -1) != -1);
		try {
			if (autofit) {
				Tokenizer counter = ms.getTokenizer(model);
				int tok = counter.count(prompt);
				if (suffix != null)
					tok += counter.count(suffix);
				req.setMaxTokens(ms.getContextSize(model) - tok);
			}

			CompletionsResponse resp = endpoint.getClient().createCompletion(req);
			CompletionsChoice choice = resp.getChoices().get(0);
			return TextResponse.fromGptApi(choice.getText(), choice.getFinishReason());

		} finally {

			// for next call, or maxTokens will remain fixed
			if (autofit)
				req.setMaxTokens(null);
		}
	}
}
