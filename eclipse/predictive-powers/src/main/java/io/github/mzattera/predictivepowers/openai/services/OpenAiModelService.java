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

package io.github.mzattera.predictivepowers.openai.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mzattera.predictivepowers.openai.client.models.Model;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiModelMetaData.SupportedApi;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiModelMetaData.SupportedCallType;
import io.github.mzattera.predictivepowers.services.AbstractModelService;
import io.github.mzattera.predictivepowers.services.ModelService;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

/**
 * This class provides {@link ModelService}s for OpenAI.
 * 
 * The class is tread-safe and uses a single data repository for all of its
 * instances.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class OpenAiModelService extends AbstractModelService {

	@ToString
	public static class OpenAiModelMetaData extends ModelMetaData {

		@Override
		public OpenAiTokenizer getTokenizer() {
			return (OpenAiTokenizer)super.getTokenizer();
		}
		
		public enum SupportedCallType {
			NONE, FUNCTIONS, TOOLS
		}

		/** The type of tools (none, only functions, or tools) a model can call */
		@Getter
		private final SupportedCallType supportedCallType;

		public enum SupportedApi {
			COMPLETIONS, CHAT, AUDIO, EMBEDDINGS
		}

		/** The API this model supports */
		@Getter
		private final SupportedApi supportedApi;

		public OpenAiModelMetaData(@NonNull String model, int contextSize) {
			this(model, SupportedApi.CHAT, contextSize, null, SupportedCallType.NONE);
		}

		public OpenAiModelMetaData(@NonNull String model, int contextSize, SupportedApi api) {
			this(model, api, contextSize, null, SupportedCallType.NONE);
		}

		public OpenAiModelMetaData(@NonNull String model, int contextSize, SupportedCallType supportedCallType) {
			this(model, SupportedApi.CHAT, contextSize, null, supportedCallType);
		}

		public OpenAiModelMetaData(@NonNull String model, int contextSize, int maxNewTokens) {
			this(model, SupportedApi.CHAT, contextSize, maxNewTokens, SupportedCallType.NONE);
		}

		public OpenAiModelMetaData(@NonNull String model, int contextSize, Integer maxNewTokens,
				SupportedCallType supportedCallType) {
			this(model, SupportedApi.CHAT, contextSize, maxNewTokens, supportedCallType);
		}

		public OpenAiModelMetaData(@NonNull String model, SupportedApi api, int contextSize, Integer maxNewTokens,
				SupportedCallType supportedCallType) {
			super(model, new OpenAiTokenizer(model), contextSize, maxNewTokens);
			this.supportedCallType = supportedCallType;
			this.supportedApi = api;
		}
	}

	/**
	 * Maps each model into its parameters. Paramters can be an int, which is then
	 * interpreted as the context size or a Pair<int,SupportedCalls>.
	 */
	private final static Map<String, ModelMetaData> MODEL_CONFIG = new HashMap<>();
	static {
		MODEL_CONFIG.put("babbage-002", new OpenAiModelMetaData("babbage-002", 16384, SupportedApi.COMPLETIONS));
		MODEL_CONFIG.put("davinci-002", new OpenAiModelMetaData("davinci-002", 16384, SupportedApi.COMPLETIONS));
		MODEL_CONFIG.put("gpt-3.5-turbo", new OpenAiModelMetaData("gpt-3.5-turbo", 4096, SupportedCallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-3.5-turbo-16k",
				new OpenAiModelMetaData("gpt-3.5-turbo-16k", 16384, SupportedCallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-3.5-turbo-instruct",
				new OpenAiModelMetaData("gpt-3.5-turbo-instruct", 4096, SupportedApi.COMPLETIONS));
		MODEL_CONFIG.put("gpt-3.5-turbo-instruct-0914",
				new OpenAiModelMetaData("gpt-3.5-turbo-instruct-0914", 4096, SupportedApi.COMPLETIONS));
		MODEL_CONFIG.put("gpt-3.5-turbo-1106",
				new OpenAiModelMetaData("gpt-3.5-turbo-1106", 16385, 4096, SupportedCallType.TOOLS));
		MODEL_CONFIG.put("gpt-3.5-turbo-0613",
				new OpenAiModelMetaData("gpt-3.5-turbo-0613", 4096, SupportedCallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-3.5-turbo-16k-0613",
				new OpenAiModelMetaData("gpt-3.5-turbo-16k-0613", 16384, SupportedCallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-3.5-turbo-0301", new OpenAiModelMetaData("gpt-3.5-turbo-0301", 4096));
		MODEL_CONFIG.put("gpt-4", new OpenAiModelMetaData("gpt-4", 8192, SupportedCallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-4-0613", new OpenAiModelMetaData("gpt-4-0613", 8192, SupportedCallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-4-32k", new OpenAiModelMetaData("gpt-4-32k", 32768, SupportedCallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-4-32k-0613",
				new OpenAiModelMetaData("gpt-4-32k-0613", 32768, SupportedCallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-4-32k-0314", new OpenAiModelMetaData("gpt-4-32k-0314", 32768));
		MODEL_CONFIG.put("gpt-4-1106-preview",
				new OpenAiModelMetaData("gpt-4-1106-preview", 128000, 4096, SupportedCallType.TOOLS));
		MODEL_CONFIG.put("gpt-4-vision-preview", new OpenAiModelMetaData("gpt-4-vision-preview", 128000, 4096));
		MODEL_CONFIG.put("text-embedding-ada-002",
				new OpenAiModelMetaData("text-embedding-ada-002", 8192, SupportedApi.EMBEDDINGS));
		MODEL_CONFIG.put("tts-1", new OpenAiModelMetaData("tts-1", 4096, SupportedApi.AUDIO));
		MODEL_CONFIG.put("tts-1-1106", new OpenAiModelMetaData("tts-1-1106", 2046, SupportedApi.AUDIO));
		MODEL_CONFIG.put("tts-1-hd", new OpenAiModelMetaData("tts-1-hd", 2046, SupportedApi.AUDIO));
		MODEL_CONFIG.put("tts-1-hd-1106", new OpenAiModelMetaData("tts-1-hd-1106", 2046, SupportedApi.AUDIO));
	}

	static Stream<OpenAiModelMetaData> getModelsMetadata() {
		return MODEL_CONFIG.values().stream().map(e -> (OpenAiModelMetaData) e);
	}

	@NonNull
	@Getter
	protected final OpenAiEndpoint endpoint;

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

	public OpenAiModelService(OpenAiEndpoint endpoint) {
		super(MODEL_CONFIG);
		this.endpoint = endpoint;
	}

	@Override
	public OpenAiModelMetaData get(@NonNull String model) {
		return (OpenAiModelMetaData) (super.get(model));
	}

	@Override
	public OpenAiModelMetaData put(@NonNull String model, @NonNull ModelMetaData data) {
		return (OpenAiModelMetaData) super.put(model, (OpenAiModelMetaData) data);
	}

	@Override
	public OpenAiTokenizer getTokenizer(@NonNull String model) throws IllegalArgumentException {
		return (OpenAiTokenizer) super.getTokenizer(model);
	}

	@Override
	public OpenAiTokenizer getTokenizer(@NonNull String model, Tokenizer def) {
		return (OpenAiTokenizer) super.getTokenizer(model, def);
	}

	@Override
	public int getMaxNewTokens(@NonNull String model) {
		// By default, max number of returned tokens matches context size
		return getMaxNewTokens(model, getContextSize(model));
	}

	@Override
	public List<String> listModels() {
		List<Model> l = endpoint.getClient().listModels();
		List<String> result = new ArrayList<>(l.size());
		for (Model m : l)
			result.add(m.getId());
		return result;
	}

	/**
	 * 
	 * @param model
	 * @return The type of calls (function or tool) that the model supports.
	 */
	public SupportedCallType getSupportedCall(@NonNull String model) {
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
