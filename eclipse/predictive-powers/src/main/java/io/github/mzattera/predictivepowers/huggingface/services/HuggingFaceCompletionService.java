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
package io.github.mzattera.predictivepowers.huggingface.services;

import io.github.mzattera.predictivepowers.huggingface.client.nlp.TextGenerationRequest;
import io.github.mzattera.predictivepowers.huggingface.client.nlp.TextGenerationRequest.Parameters;
import io.github.mzattera.predictivepowers.huggingface.client.nlp.TextGenerationResponse;
import io.github.mzattera.predictivepowers.huggingface.endpoint.HuggingFaceEndpoint;
import io.github.mzattera.predictivepowers.services.CompletionService;
import io.github.mzattera.predictivepowers.services.TextCompletion;
import io.github.mzattera.predictivepowers.services.TextCompletion.FinishReason;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * This class does completions (prompt execution).
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@RequiredArgsConstructor
public class HuggingFaceCompletionService implements CompletionService {

	public static final String DEFAULT_MODEL = "gpt2-large";

	public HuggingFaceCompletionService(HuggingFaceEndpoint ep) {
		this(ep, TextGenerationRequest.builder()
				.parameters(Parameters.builder().returnFullText(false).numReturnSequences(1).build()).build());
	}

	@NonNull
	@Getter
	protected final HuggingFaceEndpoint endpoint;

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
	private final TextGenerationRequest defaultReq;

	@Getter
	@Setter
	private String model = DEFAULT_MODEL;

	@Override
	public Integer getTopK() {
		return defaultReq.getParameters().getTopK();
	}

	@Override
	public void setTopK(Integer topK) {
		defaultReq.getParameters().setTopK(topK);
	}

	@Override
	public Double getTopP() {
		return defaultReq.getParameters().getTopP();
	}

	@Override
	public void setTopP(Double topP) {
		defaultReq.getParameters().setTopP(topP);
	}

	@Override
	public Double getTemperature() {
		return defaultReq.getParameters().getTemperature();
	}

	@Override
	public void setTemperature(Double temperature) {
		defaultReq.getParameters().setTemperature(temperature);
	}

	/**
	 * This service will try to calculate this so to allow the longest possible
	 * output, if it is null.
	 */
	@Override
	public Integer getMaxNewTokens() {
		return defaultReq.getParameters().getMaxNewTokens();
	}

	/**
	 * This service will try to calculate this so to allow the longest possible
	 * output, if it is null.
	 */
	@Override
	public void setMaxNewTokens(Integer maxNewTokens) {
		defaultReq.getParameters().setMaxNewTokens(maxNewTokens);
	}

	@Override
	public boolean getEcho() {
		if (defaultReq.getParameters().getReturnFullText() == null)
			return false;
		return defaultReq.getParameters().getReturnFullText();
	}

	@Override
	public void setEcho(boolean echo) {
		defaultReq.getParameters().setReturnFullText(echo);
	}

	@Override
	public TextCompletion complete(String prompt) {
		return complete(prompt, defaultReq);
	}

	/**
	 * Completes text (executes given prompt); uses provided
	 * {@link TextGenerationRequest} to get parameters for the call.
	 */
	public TextCompletion complete(String prompt, TextGenerationRequest req) {
		req.getInputs().clear();
		req.getInputs().add(prompt);
		req.getOptions().setWaitForModel(true); // TODO remove? Improve?

		TextGenerationResponse resp = endpoint.getClient().textGeneration(model, req).get(0).get(0);
		return TextCompletion.builder().text(resp.getGeneratedText()).finishReason(FinishReason.OK).build();
	}

	@Override
	public TextCompletion insert(String prompt, String suffix) {
		return insert(prompt, suffix, defaultReq);
	}

	/**
	 * Inserts text between given prompt and the suffix (executes given prompt);
	 * uses provided {@link TextGenerationRequest} to get parameters for the call.
	 */
	public TextCompletion insert(String prompt, String suffix, TextGenerationRequest req) {
		// TODO maybe find a workaround with completion? Or move insertion into a different model?
		throw new UnsupportedOperationException();
	}
}
