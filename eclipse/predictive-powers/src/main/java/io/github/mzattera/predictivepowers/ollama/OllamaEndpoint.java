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

package io.github.mzattera.predictivepowers.ollama;

import java.util.concurrent.TimeUnit;

import io.github.mzattera.ollama.ApiClient;
import io.github.mzattera.ollama.client.api.OllamaApi;
import io.github.mzattera.predictivepowers.AiEndpoint;
import io.github.mzattera.predictivepowers.EndpointException;
import io.github.mzattera.predictivepowers.services.AgentService;
import io.github.mzattera.predictivepowers.services.ImageGenerationService;
import lombok.Getter;
import lombok.NonNull;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * This represents an Ollama API endpoint, from which services can be created.
 * 
 * This class is thread-safe.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
public class OllamaEndpoint implements AiEndpoint {

	@Getter
	private final OllamaApi client;

	/**
	 * Creates an Ollama endpoint at http://localhost:11434.
	 */
	public OllamaEndpoint() {

		ApiClient apiClient = new ApiClient();
		OkHttpClient.Builder httpClientBuilder = apiClient.getHttpClient().newBuilder()
				.connectTimeout(10, TimeUnit.MINUTES).readTimeout(10, TimeUnit.MINUTES);

		// Interceptors for debug
		HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(System.out::println);
		loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
		httpClientBuilder.addInterceptor(loggingInterceptor);

		apiClient.setHttpClient(httpClientBuilder.build());
		this.client = new OllamaApi(apiClient);
	}

	/**
	 * Creates an endpoint using an existing client.
	 */
	public OllamaEndpoint(@NonNull OllamaApi client) {
		this.client = client;
	}

	/**
	 * One instance per endpoint, so user can register new models which will be
	 * valid for all services for the endpoint
	 */
	private final OllamaModelService modelService = new OllamaModelService(this);

	@Override
	public OllamaModelService getModelService() {
		return modelService;
	}

	@Override
	public OllamaCompletionService getCompletionService() {
		return new OllamaCompletionService(this);
	}

	@Override
	public OllamaCompletionService getCompletionService(@NonNull String model) {
		return new OllamaCompletionService(this, model);
	}

	@Override
	public OllamaEmbeddingService getEmbeddingService() {
		return new OllamaEmbeddingService(this);
	}

	@Override
	public OllamaEmbeddingService getEmbeddingService(@NonNull String model) {
		return new OllamaEmbeddingService(this, model);
	}

	@Override
	public OllamaChatService getChatService() {
		return new OllamaChatService(this);
	}

	@Override
	public OllamaChatService getChatService(@NonNull String model) {
		return new OllamaChatService(this, model);
	}

	@Override
	public ImageGenerationService getImageGenerationService() {
		throw new EndpointException(new UnsupportedOperationException());
	}

	@Override
	public ImageGenerationService getImageGenerationService(@NonNull String model) {
		throw new EndpointException(new UnsupportedOperationException());
	}

	@Override
	public AgentService getAgentService() {
		throw new EndpointException(new UnsupportedOperationException());
	}

	@Override
	public AgentService getAgentService(@NonNull String model) {
		throw new EndpointException(new UnsupportedOperationException());
	}

	@Override
	public synchronized void close() {
	}
}
