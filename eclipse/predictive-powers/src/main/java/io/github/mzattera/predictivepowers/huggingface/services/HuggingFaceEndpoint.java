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

package io.github.mzattera.predictivepowers.huggingface.services;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.github.mzattera.hfinferenceapi.ApiClient;
import io.github.mzattera.hfinferenceapi.auth.Authentication;
import io.github.mzattera.hfinferenceapi.auth.HttpBearerAuth;
import io.github.mzattera.hfinferenceapi.client.api.HuggingFaceApi;
import io.github.mzattera.predictivepowers.AiEndpoint;
import io.github.mzattera.predictivepowers.services.AgentService;
import io.github.mzattera.predictivepowers.services.CompletionService;
import lombok.Getter;
import lombok.NonNull;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * This represents a Hugging Face endpoint, from which services can be created.
 * 
 * This class is thread-safe.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
public class HuggingFaceEndpoint implements AiEndpoint {

	/**
	 * API client, you can access this to confiugure API access, if needed.
	 */
	@Getter
	private final HuggingFaceApi client;

	/**
	 * Creates endpoint by reading API key from HUGGING_FACE_API_KEY environment
	 * variable.
	 */
	public HuggingFaceEndpoint() {
		this(System.getenv("HUGGING_FACE_API_KEY"));
	}

	/**
	 * Creates endpoint that uses provided API key.
	 */
	public HuggingFaceEndpoint(@NonNull String apiKey) {

		ApiClient apiClient = new ApiClient();
		OkHttpClient.Builder httpClientBuilder = apiClient.getHttpClient().newBuilder()
				.connectTimeout(10, TimeUnit.MINUTES).readTimeout(10, TimeUnit.MINUTES);

		// DEBUG code to log traffic
		HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(System.out::println);
		loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
		httpClientBuilder.addInterceptor(loggingInterceptor);

		apiClient.setHttpClient(httpClientBuilder.build());

		// Set authentication bearer
		Map<String, Authentication> auths = apiClient.getAuthentications();
		Authentication bearerAuth = auths.get("bearerAuth");
		if (bearerAuth instanceof HttpBearerAuth) {
			HttpBearerAuth httpBearerAuth = (HttpBearerAuth) bearerAuth;
			httpBearerAuth.setBearerToken(apiKey);
		} else {
			throw new RuntimeException("API client authentication setup failed. Check your client generation.");
		}

		this.client = new HuggingFaceApi(apiClient);
	}

	public HuggingFaceEndpoint(@NonNull HuggingFaceApi client) {
		this.client = client;
	}

	@Override
	public HuggingFaceModelService getModelService() {
		return new HuggingFaceModelService(this);
	}

	@Override
	public CompletionService getCompletionService() {
		throw new UnsupportedOperationException();
	}

	@Override
	public CompletionService getCompletionService(@NonNull String model) {
		throw new UnsupportedOperationException();
	}

	@Override
	public HuggingFaceEmbeddingService getEmbeddingService() {
		return new HuggingFaceEmbeddingService(this);
	}

	@Override
	public HuggingFaceEmbeddingService getEmbeddingService(@NonNull String model) {
		return new HuggingFaceEmbeddingService(this, model);
	}

	@Override
	public HuggingFaceChatService getChatService() {
		return new HuggingFaceChatService(this);
	}

	@Override
	public HuggingFaceChatService getChatService(@NonNull String model) {
		return new HuggingFaceChatService(this, model);
	}

	@Override
	public AgentService getAgentService() {
		throw new UnsupportedOperationException();
	}

	@Override
	public AgentService getAgentService(@NonNull String model) {
		throw new UnsupportedOperationException();
	}

	@Override
	public HuggingFaceImageGenerationService getImageGenerationService() {
		return new HuggingFaceImageGenerationService(this);
	}

	@Override
	public HuggingFaceImageGenerationService getImageGenerationService(@NonNull String model) {
		return new HuggingFaceImageGenerationService(this, model);
	}

	@Override
	public synchronized void close() {
	}
}
