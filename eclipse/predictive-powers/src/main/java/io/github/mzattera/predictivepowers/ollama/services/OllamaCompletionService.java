/*
 * Copyright 2023-2025 Massimiliano "Maxi" Zattera
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
package io.github.mzattera.predictivepowers.ollama.services;

import io.github.mzattera.ollama.client.model.GenerateRequest;
import io.github.mzattera.ollama.client.model.GenerateResponse;
import io.github.mzattera.ollama.client.model.RequestOptions;
import io.github.mzattera.predictivepowers.EndpointException;
import io.github.mzattera.predictivepowers.services.CompletionService;
import io.github.mzattera.predictivepowers.services.messages.TextCompletion;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * This class does completions (prompt execution).
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class OllamaCompletionService implements CompletionService {

	// TODO URGENT we do not have such a6 thing...either we take the first or
	// pretend user specifies a model
	public static final String DEFAULT_MODEL = "ministral-3:3b";

	@NonNull
	@Getter
	protected final OllamaEndpoint endpoint;

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
	@Setter
	@NonNull
	private GenerateRequest defaultRequest;

	@Override
	public String getModel() {
		return defaultRequest.getModel();
	}

	@Override
	public void setModel(@NonNull String model) {
		defaultRequest.setModel(model);
	}

	@Override
	public Integer getTopK() {
		RequestOptions opt = defaultRequest.getOptions();
		if (opt != null)
			return opt.getTopK();
		return null;
	}

	@Override
	public void setTopK(Integer topK) {
		RequestOptions opt = defaultRequest.getOptions();
		if (opt == null) {
			opt = new RequestOptions();
			defaultRequest.setOptions(opt);
		}
		opt.setTopK(topK);
	}

	@Override
	public Double getTopP() {
		RequestOptions opt = defaultRequest.getOptions();
		if (opt != null)
			return opt.getTopP() == null ? null : opt.getTopP().doubleValue();
		return null;
	}

	@Override
	public void setTopP(Double topP) {
		RequestOptions opt = defaultRequest.getOptions();
		if (opt == null) {
			opt = new RequestOptions();
			defaultRequest.setOptions(opt);
		}
		opt.setTopP(topP == null ? null : topP.floatValue());
	}

	@Override
	public Double getTemperature() {
		// Must scale from [0-1] to [0-100] considering default value as well
		RequestOptions opt = defaultRequest.getOptions();
		if (opt != null)
			return opt.getTemperature() == null ? null : opt.getTemperature().doubleValue() * 100.0d;
		return null;
	}

	@Override
	public void setTemperature(Double temperature) {
		// Must scale from [0-1] to [0-100] considering default value as well
		RequestOptions opt = defaultRequest.getOptions();
		if (opt == null) {
			opt = new RequestOptions();
			defaultRequest.setOptions(opt);
		}
		opt.setTemperature(temperature == null ? null : temperature.floatValue() / 100.0f);
	}

	@Override
	public Integer getMaxNewTokens() {
		RequestOptions opt = defaultRequest.getOptions();
		if (opt != null)
			return opt.getNumPredict() == null ? null : opt.getNumPredict();
		return null;
	}

	@Override
	public void setMaxNewTokens(Integer maxNewTokens) {
		RequestOptions opt = defaultRequest.getOptions();
		if (opt == null) {
			opt = new RequestOptions();
			defaultRequest.setOptions(opt);
		}
		opt.setNumPredict(maxNewTokens);
	}

	@Override
	public boolean getEcho() {
		return false;
	}

	@Override
	public void setEcho(boolean echo) {
		if (echo)
			throw new EndpointException(new UnsupportedOperationException());
	}

	protected OllamaCompletionService(@NonNull OllamaEndpoint ep) {
		this(ep, DEFAULT_MODEL);
	}

	protected OllamaCompletionService(@NonNull OllamaEndpoint ep, @NonNull String model) {
		this.endpoint = ep;
		this.defaultRequest = new GenerateRequest().model(model);
	}

	@Override
	public TextCompletion complete(String prompt) throws EndpointException {
		try {
			return complete(prompt, null);
		} catch (Exception e) {
			throw OllamaUtil.toEndpointException(e);
		}
	}

	@Override
	public TextCompletion insert(String prompt, String suffix) throws EndpointException {
		try {
			return complete(prompt, suffix);
		} catch (Exception e) {
			throw OllamaUtil.toEndpointException(e);
		}
	}

	private TextCompletion complete(String prompt, String suffix) {

		defaultRequest.setPrompt(prompt);
		defaultRequest.setSuffix(suffix);
		GenerateResponse response = endpoint.getClient().generate(defaultRequest);

		return new TextCompletion(OllamaUtil.fromOllamaFinishReason(response), response.getResponse());
	}

	@Override
	public void close() {
	}
}
