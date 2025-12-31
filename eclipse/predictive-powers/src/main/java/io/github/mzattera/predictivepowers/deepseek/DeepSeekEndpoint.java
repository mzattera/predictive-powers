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

package io.github.mzattera.predictivepowers.deepseek;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

import io.github.mzattera.predictivepowers.AiEndpoint;
import io.github.mzattera.predictivepowers.EndpointException;
import io.github.mzattera.predictivepowers.services.AgentService;
import io.github.mzattera.predictivepowers.services.CompletionService;
import io.github.mzattera.predictivepowers.services.EmbeddingService;
import io.github.mzattera.predictivepowers.services.ImageGenerationService;
import lombok.Getter;
import lombok.NonNull;

/**
 * This represents a DeepSeek endpoint, from which services can be created.
 * Underneath, it uses DeepSeek Java SDK.
 * 
 * This class is thread-safe.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
public class DeepSeekEndpoint implements AiEndpoint {

	private final static Logger LOG = LoggerFactory.getLogger(DeepSeekEndpoint.class);

	@Getter
	private final OpenAIClient client;

	/**
	 * Creates an endpoint using the `DEEPSEEK_API_KEY` environment variables.
	 */
	public DeepSeekEndpoint() {
		this(System.getenv("DEEPSEEK_API_KEY"));
	}

	public DeepSeekEndpoint(@NonNull String apiKey) {
		this(OpenAIOkHttpClient.builder().apiKey(apiKey).baseUrl("https://api.deepseek.com/v1").build());
	}

	/**
	 * Creates an endpoint using an existing client.
	 */
	public DeepSeekEndpoint(@NonNull OpenAIClient client) {
		this.client = client;
	}

	/**
	 * One instance per endpoint, so user can register new models which will be
	 * valid for all services for the endpoint
	 */
	private final DeepSeekModelService modelService = new DeepSeekModelService(this);

	@Override
	public DeepSeekModelService getModelService() {
		return modelService;
	}

	@Override
	public CompletionService getCompletionService() {
		throw new EndpointException(new UnsupportedOperationException());
	}

	@Override
	public CompletionService getCompletionService(@NonNull String model) {
		throw new EndpointException(new UnsupportedOperationException());
	}

	@Override
	public EmbeddingService getEmbeddingService() throws EndpointException {
		throw new EndpointException(new UnsupportedOperationException());
	}

	@Override
	public EmbeddingService getEmbeddingService(@NonNull String model) {
		throw new EndpointException(new UnsupportedOperationException());
	}

	@Override
	public DeepSeekChatService getChatService() {
		return new DeepSeekChatService(this);
	}

	@Override
	public DeepSeekChatService getChatService(@NonNull String model) {
		return new DeepSeekChatService(this, model);
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
		try {
			client.close();
		} catch (Exception e) {
			LOG.warn("Error while closing endpoint client", e);
		}
	}
}
