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

import java.util.Map;

import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiModelMetaData.SupportedApi;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiModelMetaData.SupportedCallType;
import io.github.mzattera.predictivepowers.services.AbstractModelService;
import io.github.mzattera.predictivepowers.services.ModelService;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

/**
 * This interface represents a {@link ModelService} for OpenAI models, whether
 * they are provided by OpenAI API or Azure OpenAI Service.
 */
public abstract class OpenAiModelService extends AbstractModelService {

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

		public OpenAiModelMetaData(@NonNull String model, int contextSize, SupportedCallType supportedCallType, boolean hasVision) {
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

	protected OpenAiModelService(@NonNull Map<String, ModelMetaData> map) {
		super(map);
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