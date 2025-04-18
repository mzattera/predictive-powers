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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.mzattera.predictivepowers.openai.client.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiModelMetaData.SupportedApi;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiModelMetaData.SupportedCallType;
import io.github.mzattera.predictivepowers.services.AbstractModelService;
import io.github.mzattera.predictivepowers.services.ModelService;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

/**
 * This interface represents a {@link ModelService} for OpenAI models.
 */
public class OpenAiModelService extends AbstractModelService {

	@ToString(callSuper = true)
	public static class OpenAiModelMetaData extends ModelMetaData {

		@Override
		public OpenAiTokenizer getTokenizer() {
			return (OpenAiTokenizer) super.getTokenizer();
		}

		public enum SupportedCallType {
			NONE, FUNCTIONS, TOOLS
		}

		/** The type of tools (none, only functions, or tools) a model can call */
		@Getter
		private final SupportedCallType supportedCallType;

		public enum SupportedApi {
			COMPLETIONS, CHAT, TTS, STT, EMBEDDINGS, IMAGES, OTHER
		}

		/** The API this model supports */
		@Getter
		private final SupportedApi supportedApi;

		public OpenAiModelMetaData(@NonNull String model, SupportedApi api) {
			this(model, api, null, null, SupportedCallType.NONE);
		}

		public OpenAiModelMetaData(@NonNull String model, int contextSize) {
			this(model, SupportedApi.CHAT, contextSize, null, SupportedCallType.NONE);
		}

		public OpenAiModelMetaData(@NonNull String model, int contextSize, SupportedApi api) {
			this(model, api, contextSize, null, SupportedCallType.NONE);
		}

		public OpenAiModelMetaData(@NonNull String model, int contextSize, SupportedCallType supportedCallType) {
			this(model, SupportedApi.CHAT, contextSize, null, supportedCallType);
		}

		public OpenAiModelMetaData(@NonNull String model, int contextSize, SupportedCallType supportedCallType,
				boolean hasVision) {
			this(model, SupportedApi.CHAT, contextSize, null, supportedCallType, hasVision);
		}

		public OpenAiModelMetaData(@NonNull String model, int contextSize, int maxNewTokens) {
			this(model, SupportedApi.CHAT, contextSize, maxNewTokens, SupportedCallType.NONE);
		}

		public OpenAiModelMetaData(@NonNull String model, int contextSize, Integer maxNewTokens,
				SupportedCallType supportedCallType) {
			this(model, SupportedApi.CHAT, contextSize, maxNewTokens, supportedCallType);
		}

		public OpenAiModelMetaData(@NonNull String model, int contextSize, Integer maxNewTokens,
				SupportedCallType supportedCallType, boolean hasVision) {
			this(model, SupportedApi.CHAT, contextSize, maxNewTokens, supportedCallType, hasVision);
		}

		public OpenAiModelMetaData(@NonNull String model, SupportedApi api, Integer contextSize, Integer maxNewTokens,
				SupportedCallType supportedCallType) {
			this(model, api, contextSize, maxNewTokens, supportedCallType, false);
		}

		public OpenAiModelMetaData(@NonNull String model, SupportedApi api, Integer contextSize, Integer maxNewTokens,
				SupportedCallType supportedCallType, boolean hasVision) {
			super(model, new OpenAiTokenizer(model), contextSize, maxNewTokens, hasVision);
			this.supportedCallType = supportedCallType;
			this.supportedApi = api;
		}
	}

	/**
	 * Maps each OpenAI model into its metadata
	 */
	final static Map<String, ModelMetaData> MODEL_CONFIG = new ConcurrentHashMap<>();
	static {

		MODEL_CONFIG.put("babbage-002", new OpenAiModelMetaData("babbage-002", 16384, SupportedApi.COMPLETIONS));
		MODEL_CONFIG.put("davinci-002", new OpenAiModelMetaData("davinci-002", 16385, SupportedApi.COMPLETIONS));

		MODEL_CONFIG.put("gpt-3.5-turbo", new OpenAiModelMetaData("gpt-3.5-turbo", 4096, SupportedCallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-3.5-turbo-16k",
				new OpenAiModelMetaData("gpt-3.5-turbo-16k", 16384, SupportedCallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-3.5-turbo-instruct",
				new OpenAiModelMetaData("gpt-3.5-turbo-instruct", 4096, SupportedApi.COMPLETIONS));
		MODEL_CONFIG.put("gpt-3.5-turbo-instruct-0914",
				new OpenAiModelMetaData("gpt-3.5-turbo-instruct-0914", 4096, SupportedApi.COMPLETIONS));
		MODEL_CONFIG.put("gpt-3.5-turbo-0125",
				new OpenAiModelMetaData("gpt-3.5-turbo-0125", 16385, 4096, SupportedCallType.TOOLS));
		MODEL_CONFIG.put("gpt-3.5-turbo-1106",
				new OpenAiModelMetaData("gpt-3.5-turbo-1106", 16385, 4096, SupportedCallType.TOOLS));
		MODEL_CONFIG.put("gpt-3.5-turbo-0613",
				new OpenAiModelMetaData("gpt-3.5-turbo-0613", 4096, SupportedCallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-3.5-turbo-16k-0613",
				new OpenAiModelMetaData("gpt-3.5-turbo-16k-0613", 16384, SupportedCallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-3.5-turbo-0301",
				new OpenAiModelMetaData("gpt-3.5-turbo-0301", 4096, SupportedCallType.NONE));
		MODEL_CONFIG.put("gpt-3.5-turbo-1106",
				new OpenAiModelMetaData("gpt-3.5-turbo-1106", 16385, 4096, SupportedCallType.TOOLS));

		MODEL_CONFIG.put("gpt-4", new OpenAiModelMetaData("gpt-4", 8192, SupportedCallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-4-0613", new OpenAiModelMetaData("gpt-4-0613", 8192, SupportedCallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-4-32k", new OpenAiModelMetaData("gpt-4-32k", 32768, SupportedCallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-4-32k-0613",
				new OpenAiModelMetaData("gpt-4-32k-0613", 32768, SupportedCallType.FUNCTIONS));

		MODEL_CONFIG.put("gpt-4-turbo",
				new OpenAiModelMetaData("gpt-4-turbo", 128000, 4096, SupportedCallType.TOOLS, true));
		MODEL_CONFIG.put("gpt-4-turbo-2024-04-09",
				new OpenAiModelMetaData("gpt-4-turbo-2024-04-09", 128000, 4096, SupportedCallType.TOOLS, true));
		MODEL_CONFIG.put("gpt-4-turbo-preview",
				new OpenAiModelMetaData("gpt-4-turbo-preview", 128000, 4096, SupportedCallType.TOOLS));
		MODEL_CONFIG.put("gpt-4-0125-preview",
				new OpenAiModelMetaData("gpt-4-0125-preview", 128000, 4096, SupportedCallType.TOOLS));
		MODEL_CONFIG.put("gpt-4-1106-preview",
				new OpenAiModelMetaData("gpt-4-1106-preview", 128000, 4096, SupportedCallType.TOOLS));
		MODEL_CONFIG.put("gpt-4-vision-preview",
				new OpenAiModelMetaData("gpt-4-vision-preview", 128000, 4096, SupportedCallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-4-1106-vision-preview",
				new OpenAiModelMetaData("gpt-4-1106-vision-preview", 128000, 4096, SupportedCallType.NONE));

		MODEL_CONFIG.put("gpt-4o", new OpenAiModelMetaData("gpt-4o", 128000, 4096, SupportedCallType.TOOLS, true));
		MODEL_CONFIG.put("gpt-4o-2024-05-13",
				new OpenAiModelMetaData("gpt-4o-2024-05-13", 128000, 4096, SupportedCallType.TOOLS, true));

		MODEL_CONFIG.put("text-embedding-3-large",
				new OpenAiModelMetaData("text-embedding-3-large", 8191, SupportedApi.EMBEDDINGS));
		MODEL_CONFIG.put("text-embedding-3-small",
				new OpenAiModelMetaData("text-embedding-3-small", 8192, SupportedApi.EMBEDDINGS));
		MODEL_CONFIG.put("text-embedding-ada-002",
				new OpenAiModelMetaData("text-embedding-ada-002", 8192, SupportedApi.EMBEDDINGS));

		MODEL_CONFIG.put("tts-1", new OpenAiModelMetaData("tts-1", 4096, SupportedApi.TTS));
		MODEL_CONFIG.put("tts-1-1106", new OpenAiModelMetaData("tts-1-1106", 2046, SupportedApi.TTS));
		MODEL_CONFIG.put("tts-1-hd", new OpenAiModelMetaData("tts-1-hd", 2046, SupportedApi.TTS));
		MODEL_CONFIG.put("tts-1-hd-1106", new OpenAiModelMetaData("tts-1-hd-1106", 2046, SupportedApi.TTS));

		MODEL_CONFIG.put("whisper-1", new OpenAiModelMetaData("whisper-1", SupportedApi.STT));

		MODEL_CONFIG.put("dall-e-2", new OpenAiModelMetaData("dall-e-2", SupportedApi.IMAGES));
		MODEL_CONFIG.put("dall-e-3", new OpenAiModelMetaData("dall-e-3", SupportedApi.IMAGES));
	}

	/**
	 * For testing purposes only.
	 */
	static Stream<OpenAiModelMetaData> getModelsMetadata() {
		return MODEL_CONFIG.values().stream().map(e -> (OpenAiModelMetaData) e);
	}

	@NonNull
	@Getter
	protected final OpenAiEndpoint endpoint;

	public OpenAiModelService(@NonNull OpenAiEndpoint endpoint) {
		super(MODEL_CONFIG);
		this.endpoint = endpoint;
	}

	@Override
	public List<String> listModels() {
		return endpoint.getClient().listModels().stream().map(m -> m.getId()).collect(Collectors.toList());
	}

	/**
	 * @return null, as there is no model associated with this service.
	 */
	@Override
	public String getModel() {
		return null;
	}

	/**
	 * Unsupported, as there is no model associated with this service.
	 */
	@Override
	public void setModel(@NonNull String model) {
		throw new UnsupportedOperationException();
	}

	@Override
	public OpenAiModelMetaData get(@NonNull String model) {
		return (OpenAiModelMetaData) (super.get(model));
	}

	@Override
	public OpenAiModelMetaData put(@NonNull String model, @NonNull ModelMetaData data) {
		if ((data.getTokenizer() != null) && !(data.getTokenizer() instanceof OpenAiTokenizer))
			throw new IllegalArgumentException("Tokenizer must be a subclass of OpenAiTokenizer");
		return (OpenAiModelMetaData) super.put(model, data);
	}

	@Override
	public OpenAiTokenizer getTokenizer(@NonNull String model) {
		Tokenizer result = super.getTokenizer(model);
		if (result == null)
			return null;
		return (OpenAiTokenizer) result;
	}

	@Override
	public OpenAiTokenizer getTokenizer(@NonNull String model, Tokenizer def) {
		if ((def != null) && !(def instanceof OpenAiTokenizer))
			throw new IllegalArgumentException("Tokenizer must be a subclass of OpenAiTokenizer");
		return (OpenAiTokenizer) super.getTokenizer(model, def);
	}

	@Override
	public int getMaxNewTokens(@NonNull String model) {
		// By default, max number of returned tokens matches context size
		return getMaxNewTokens(model, getContextSize(model));
	}

	/**
	 * 
	 * @param model
	 * @return The type of calls (function or tool) that the model supports.
	 */
	public SupportedCallType getSupportedCallType(@NonNull String model) {
		OpenAiModelMetaData data = get(model);
		if (data == null)
			throw new IllegalArgumentException(
					"No metadata found for model " + model + ". Consider registering model data");
		return data.getSupportedCallType();
	}

	/**
	 * 
	 * @param model
	 * @return The API (chat or completions) that the model supports.
	 */
	public SupportedApi getSupportedApi(@NonNull String model) {
		OpenAiModelMetaData data = get(model);
		if (data == null)
			throw new IllegalArgumentException(
					"No metadata found for model " + model + ". Consider registering model data");
		return data.getSupportedApi();
	}
}