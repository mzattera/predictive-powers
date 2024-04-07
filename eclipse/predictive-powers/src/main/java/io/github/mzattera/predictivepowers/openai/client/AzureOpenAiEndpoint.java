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

package io.github.mzattera.predictivepowers.openai.client;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mzattera.predictivepowers.openai.services.AzureOpenAiModelService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiAgentService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiCompletionService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiEmbeddingService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiImageGenerationService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiQuestionAnsweringService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiQuestionExtractionService;
import lombok.Getter;
import lombok.NonNull;

/**
 * This represents an Azure OpenAI API Service endpoint, from which services can
 * be created.
 * 
 * This class is thread-safe.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
public class AzureOpenAiEndpoint implements OpenAiEndpoint {

	private final static Logger LOG = LoggerFactory.getLogger(AzureOpenAiEndpoint.class);

	// Because of the mapping between deployment IDs and OpenAI model IDs, we keep a
	// model service instance per endpoint (= Azure OpenAI Service).
	// See AzureOpenAiModelService JavaDoc.
	private final static Map<String, AzureOpenAiModelService> cache = new HashMap<>();

	private final AzureOpenAiModelService modelService;

	@Getter
	private final AzureOpenAiClient client;

	public AzureOpenAiEndpoint() {
		this(null, null);
	}

	public AzureOpenAiEndpoint(String resourceName) {
		this(resourceName, null);
	}

	public AzureOpenAiEndpoint(String resourceName, String apiKey) {
		this(new AzureOpenAiClient(resourceName, apiKey));
	}

	public AzureOpenAiEndpoint(@NonNull AzureOpenAiClient client) {
		this.client = client;
		String resourceName = client.getAzureResourceName();
		synchronized (cache) {
			AzureOpenAiModelService svc = cache.get(resourceName);
			if (svc == null) {
				svc = new AzureOpenAiModelService();
				cache.put(resourceName, svc);
			}
			modelService = svc;
		}
	}

	@Override
	public AzureOpenAiModelService getModelService() {
		return modelService;
	}

	@Override
	public OpenAiCompletionService getCompletionService() {
		throw new UnsupportedOperationException("For Azure OpenAI Service a model/deployment ID must be specified.");
	}

	@Override
	public OpenAiCompletionService getCompletionService(@NonNull String model) {
		return new OpenAiCompletionService(this, model);
	}

	@Override
	public OpenAiEmbeddingService getEmbeddingService() {
		throw new UnsupportedOperationException("For Azure OpenAI Service a model/deployment ID must be specified.");
	}

	@Override
	public OpenAiEmbeddingService getEmbeddingService(@NonNull String model) {
		return new OpenAiEmbeddingService(this, model);
	}

	@Override
	public OpenAiChatService getChatService() {
		throw new UnsupportedOperationException("For Azure OpenAI Service a model/deployment ID must be specified.");
	}

	@Override
	public OpenAiChatService getChatService(@NonNull String model) {
		return new OpenAiChatService(this, model);
	}

	@Override
	public OpenAiQuestionExtractionService getQuestionExtractionService() {
		throw new UnsupportedOperationException("For Azure OpenAI Service a model/deployment ID must be specified.");
	}

	@Override
	public OpenAiQuestionExtractionService getQuestionExtractionService(@NonNull String model) {
		return new OpenAiQuestionExtractionService(this, model);
	}

	@Override
	public OpenAiQuestionAnsweringService getQuestionAnsweringService() {
		throw new UnsupportedOperationException("For Azure OpenAI Service a model/deployment ID must be specified.");
	}

	@Override
	public OpenAiQuestionAnsweringService getQuestionAnsweringService(@NonNull String model) {
		return new OpenAiQuestionAnsweringService(this, model);
	}

	@Override
	public OpenAiImageGenerationService getImageGenerationService() {
		throw new UnsupportedOperationException("For Azure OpenAI Service a model/deployment ID must be specified.");
	}

	@Override
	public OpenAiImageGenerationService getImageGenerationService(@NonNull String model) {
		return new OpenAiImageGenerationService(this, model);
	}

	@Override
	public OpenAiAgentService getAgentService() {
		throw new UnsupportedOperationException("For Azure OpenAI Service a model/deployment ID must be specified.");
	}

	@Override
	public OpenAiAgentService getAgentService(@NonNull String model) {
		return new OpenAiAgentService(this, model);
	}

	@Override
	public synchronized void close() {
		try {
			client.close();
		} catch (Exception e) {
			LOG.warn("Error while closing endpoint", e);
		}
	}
}
