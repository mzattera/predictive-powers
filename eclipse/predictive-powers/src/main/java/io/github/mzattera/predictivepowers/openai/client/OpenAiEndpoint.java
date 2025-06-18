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

package io.github.mzattera.predictivepowers.openai.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

import io.github.mzattera.predictivepowers.AiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiAgentService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiCompletionService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiEmbeddingService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiImageGenerationService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiQuestionAnsweringService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiQuestionExtractionService;
import lombok.Getter;
import lombok.NonNull;

/**
 * This represents an OpenAI API endpoint, from which services can be created.
 * 
 * This class is thread-safe.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
public class OpenAiEndpoint implements AiEndpoint {

	private final static Logger LOG = LoggerFactory.getLogger(OpenAiEndpoint.class);

	@Getter
	private final OpenAIClient client;

	/**
	 * Creates an endpoint using the `OPENAI_API_KEY`, `OPENAI_ORG_ID`,
	 * `OPENAI_PROJECT_ID` and `OPENAI_BASE_URL` environment variables.
	 */
	public OpenAiEndpoint() {
		this(OpenAIOkHttpClient.fromEnv());
	}

	/**
	 * Creates an endpoint using the `OPENAI_ORG_ID`, `OPENAI_PROJECT_ID` and
	 * `OPENAI_BASE_URL` environment variables.
	 */
	public OpenAiEndpoint(@NonNull String apiKey) {
		this(OpenAIOkHttpClient.builder().fromEnv().apiKey(apiKey).build());
	}

	/**
	 * Creates an endpoint using the `OPENAI_API_KEY` and `OPENAI_BASE_URL`
	 * environment variable.
	 */
	public OpenAiEndpoint(@NonNull String organizationId, @NonNull String projectId) {
		this(OpenAIOkHttpClient.builder().fromEnv().organization(organizationId).project(projectId).build());
	}

	/**
	 * Creates an endpoint using the `OPENAI_BASE_URL` environment variable.
	 */
	public OpenAiEndpoint(@NonNull String apiKey, @NonNull String organizationId, @NonNull String projectId) {
		this(OpenAIOkHttpClient.builder().fromEnv().apiKey(apiKey).organization(organizationId).project(projectId)
				.build());
	}

	/**
	 * Creates an endpoint connected to given Azure OpenAI endpoint. Gets the API
	 * key from `AZURE_OPENAI_KEY`.
	 * 
	 * @param azureEndpoint
	 */
//	public OpenAiEndpoint(URL azureEndpoint, TokenCredential credential) {
//		this(OpenAIOkHttpClient.builder()
//				// Gets the API key and endpoint from the `AZURE_OPENAI_KEY` and
//				// `OPENAI_BASE_URL` environment variables, respectively
////				.fromEnv() //
////				.apiKey("7d4346c5468c466a97cd75195ad5ffdd")
//				.baseUrl("https://maxiazureopenaisw.openai.azure.com/")
//				// Set the Azure Entra ID
//				.credential(BearerTokenCredential.create(AuthenticationUtil.getBearerTokenSupplier(credential,
//						"https://cognitiveservices.azure.com/.default")))
//				.build());
//	}

	/**
	 * Creates an endpoint using an existing client.
	 */
	public OpenAiEndpoint(@NonNull OpenAIClient client) {
		this.client = client;
	}

	// Model service is state-less, we do not need to create a new model service
	// each
	// time.
	private final OpenAiModelService modelService = new OpenAiModelService(this);

	@Override
	public OpenAiModelService getModelService() {
		return modelService;
	}

	@Override
	public OpenAiCompletionService getCompletionService() {
		return new OpenAiCompletionService(this);
	}

	@Override
	public OpenAiCompletionService getCompletionService(@NonNull String model) {
		return new OpenAiCompletionService(this, model);
	}

	@Override
	public OpenAiEmbeddingService getEmbeddingService() {
		return new OpenAiEmbeddingService(this);
	}

	@Override
	public OpenAiEmbeddingService getEmbeddingService(@NonNull String model) {
		return new OpenAiEmbeddingService(this, model);
	}

	@Override
	public OpenAiChatService getChatService() {
		return new OpenAiChatService(this);
	}

	@Override
	public OpenAiChatService getChatService(@NonNull String model) {
		return new OpenAiChatService(this, model);
	}

	@Override
	public OpenAiQuestionExtractionService getQuestionExtractionService() {
		return new OpenAiQuestionExtractionService(this);
	}

	@Override
	public OpenAiQuestionExtractionService getQuestionExtractionService(@NonNull String model) {
		return new OpenAiQuestionExtractionService(this, model);
	}

	@Override
	public OpenAiQuestionAnsweringService getQuestionAnsweringService() {
		return new OpenAiQuestionAnsweringService(this);
	}

	@Override
	public OpenAiQuestionAnsweringService getQuestionAnsweringService(@NonNull String model) {
		return new OpenAiQuestionAnsweringService(this, model);
	}

	@Override
	public OpenAiImageGenerationService getImageGenerationService() {
		return new OpenAiImageGenerationService(this);
	}

	@Override
	public OpenAiImageGenerationService getImageGenerationService(@NonNull String model) {
		return new OpenAiImageGenerationService(this, model);
	}

	@Override
	public OpenAiAgentService getAgentService() {
		return new OpenAiAgentService(this);
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
			LOG.warn("Error while closing endpoint client", e);
		}
	}
}
