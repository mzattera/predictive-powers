/*
 * Copyright 2023-2024 Massimiliano "Maxi" Zattera
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openai.core.JsonMissing;
import com.openai.errors.OpenAIServiceException;
import com.openai.models.completions.Completion;
import com.openai.models.completions.CompletionChoice;
import com.openai.models.completions.CompletionCreateParams;

import io.github.mzattera.predictivepowers.EndpointException;
import io.github.mzattera.predictivepowers.openai.util.OpenAiUtil;
import io.github.mzattera.predictivepowers.services.CompletionService;
import io.github.mzattera.predictivepowers.services.messages.FinishReason;
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
public class OpenAiCompletionService implements CompletionService {

	private final static Logger LOG = LoggerFactory.getLogger(OpenAiCompletionService.class);

	public static final String DEFAULT_MODEL = "davinci-002";

	@NonNull
	@Getter
	protected final OpenAiEndpoint endpoint;

	private final OpenAiModelService modelService;

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
	private CompletionCreateParams defaultRequest;

	@Override
	public String getModel() {
		return defaultRequest.model().asString();
	}

	@Override
	public void setModel(@NonNull String model) {
		defaultRequest = defaultRequest.toBuilder().model(model).build();
	}

	@Override
	public Integer getTopK() {
		return null;
	}

	@Override
	public void setTopK(Integer topK) {
		if (topK != null)
			throw new UnsupportedOperationException();
	}

	@Override
	public Double getTopP() {
		return defaultRequest.topP().orElse(null);
	}

	@Override
	public void setTopP(Double topP) {
		defaultRequest = defaultRequest.toBuilder().topP(topP).build();
	}

	@Override
	public Double getTemperature() {
		// Must scale from [0-2] to [0-100] considering default value as well
		return defaultRequest.temperature().isEmpty() ? null : defaultRequest.temperature().get() * 50;
	}

	@Override
	public void setTemperature(Double temperature) {
		// Must scale from [0-2] to [0-100] considering default value as well
		if (temperature == null)
			defaultRequest = defaultRequest.toBuilder().temperature((Double) null).build();
		else
			defaultRequest = defaultRequest.toBuilder().temperature(temperature / 50).build();
	}

	/**
	 * This service will try to calculate this so to allow the longest possible
	 * output, if it is null.
	 */
	@Override
	public Integer getMaxNewTokens() {
		return defaultRequest.maxTokens().isEmpty() ? null : defaultRequest.maxTokens().get().intValue();
	}

	/**
	 * This service will try to calculate this so to allow the longest possible
	 * output, if it is null.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setMaxNewTokens(Integer maxNewTokens) {
		// See https://github.com/openai/openai-java/issues/468
		if (maxNewTokens == null)
			defaultRequest = defaultRequest.toBuilder().maxTokens(JsonMissing.of()).build();
		else
			defaultRequest = defaultRequest.toBuilder().maxTokens(maxNewTokens).build();
	}

	@Override
	public boolean getEcho() {
		return defaultRequest.echo().orElse(null);
	}

	@Override
	public void setEcho(boolean echo) {
		defaultRequest = defaultRequest.toBuilder().echo(echo).build();
	}

	public OpenAiCompletionService(@NonNull OpenAiEndpoint ep) {
		this(ep, DEFAULT_MODEL);
	}

	@SuppressWarnings("unchecked")
	public OpenAiCompletionService(@NonNull OpenAiEndpoint ep, @NonNull String model) {
		this(ep, CompletionCreateParams.builder() //
				.model(model) //
				.prompt(JsonMissing.of()) //
				.maxTokens(Math.min(ep.getModelService().getMaxNewTokens(model, 250), 250)) //
				.echo(false).n(1).build());
	}

	public OpenAiCompletionService(OpenAiEndpoint ep, CompletionCreateParams defaultReq) {
		this.endpoint = ep;
		this.defaultRequest = defaultReq;
		this.modelService = ep.getModelService();
	}

	@Override
	public TextCompletion complete(String prompt) throws EndpointException {
		return complete(prompt, defaultRequest);
	}

	@Override
	public TextCompletion insert(String prompt, String suffix) throws EndpointException {
		// This seems to work only with gpt-3.5-turbo-instruct, but that models error
		// when used with the OpenAI SDK
		CompletionCreateParams req = defaultRequest.toBuilder().suffix(suffix).build();
		return complete(prompt, req);
	}

	private TextCompletion complete(String prompt, CompletionCreateParams req) throws EndpointException {

		try {
			req = req.toBuilder().prompt(prompt).build();

			Completion resp = null;
			while (resp == null) { // Loop till I get an answer
				try {
					resp = endpoint.getClient().completions().create(req);
				} catch (OpenAIServiceException e) {

					// Check for policy violations
					if (e.getMessage().contains("violating our usage policy")) {
						return new TextCompletion(FinishReason.INAPPROPRIATE, e.getMessage());
					}

					// Automatically recover if request is too long
					// This makes sense as req is modified only for this call (it is immutable).
					OpenAiUtil.OpenAiExceptionData d = OpenAiUtil.getExceptionData(e);
					int contextSize = modelService.getContextSize(getModel(), d.getContextSize());
					if ((contextSize > 0) && (d.getPromptLength() > 0)) {
						int optimal = contextSize - d.getPromptLength() - 1;
						if (optimal > 0) {
							LOG.warn("Reducing reply length for OpenAI completion service from "
									+ req.maxTokens().orElse(-1L) + " to " + optimal);
							req = req.toBuilder().maxTokens(optimal).build();
						} else
							throw e; // Context too small anyway
					} else
						throw e; // Not a context length issue
				}
			}

			CompletionChoice choice = resp.choices().get(0);
			return new TextCompletion(OpenAiUtil.fromOpenAiApi(choice.finishReason()), choice.text());
		} catch (Exception e) {
			throw OpenAiUtil.toEndpointException(e);
		}
	}

	@Override
	public void close() {
	}
}
