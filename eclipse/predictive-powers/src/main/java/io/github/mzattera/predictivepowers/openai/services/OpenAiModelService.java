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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.github.mzattera.predictivepowers.openai.client.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiModelMetaData.CallType;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiModelMetaData.SupportedApi;
import io.github.mzattera.predictivepowers.services.AbstractModelService;
import io.github.mzattera.predictivepowers.services.ModelService;
import io.github.mzattera.predictivepowers.services.ModelService.ModelMetaData.Mode;
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

		public enum CallType {
			NONE, FUNCTIONS, TOOLS
		}

		/** The type of tools (none, only functions, or tools) a model can call */
		@Getter
		private final CallType supportedCallType;

		public enum SupportedApi {
			CHAT, RESPONSES, REALTIME, ASSISTANTS, BATCH, FINE_TUNING, EMBEDDINGS, IMAGES, SPEECH_GENERATION,
			TRANSCRIPTION, TRANSLATION, MODERATION, COMPLETIONS
		}

		/** The API this model supports */
		@Getter
		private final List<SupportedApi> supportedApis = new ArrayList<>();

		public OpenAiModelMetaData(@NonNull String model, SupportedApi api) {
			this(model, null, null, new SupportedApi[] { api }, CallType.NONE, TEXT, TEXT);
		}

		public OpenAiModelMetaData(@NonNull String model, int contextSize, SupportedApi api) {
			this(model, contextSize, null, new SupportedApi[] { api }, CallType.NONE, TEXT, TEXT);
		}

		public OpenAiModelMetaData(@NonNull String model, int contextSize, int maxNewTokens, SupportedApi api,
				CallType supportedCallType) {
			this(model, contextSize, maxNewTokens, new SupportedApi[] { api }, supportedCallType, TEXT, TEXT);
		}

		public OpenAiModelMetaData(@NonNull String model, SupportedApi[] api, Mode[] inputModes, Mode[] outputModes) {
			this(model, null, null, api, CallType.NONE, inputModes, outputModes);
		}

		public OpenAiModelMetaData(@NonNull String model, Integer contextSize, Integer maxNewTokens, SupportedApi[] api,
				CallType supportedCallType) {
			this(model, contextSize, maxNewTokens, api, supportedCallType, TEXT, TEXT);
		}

		public OpenAiModelMetaData(@NonNull String model, Integer contextSize, Integer maxNewTokens, SupportedApi[] api,
				CallType supportedCallType, Mode[] inputModes) {
			this(model, contextSize, maxNewTokens, api, supportedCallType, inputModes, TEXT);
		}

		public OpenAiModelMetaData(@NonNull String model, Integer contextSize, SupportedApi[] api, Mode[] inputModes,
				Mode[] outputModes) {
			this(model, contextSize, null, api, CallType.NONE, inputModes, outputModes);
		}

		public OpenAiModelMetaData(@NonNull String model, Integer contextSize, Integer maxNewTokens, SupportedApi[] api,
				Mode[] inputModes, Mode[] outputModes) {
			this(model, contextSize, null, api, CallType.NONE, inputModes, outputModes);
		}

		public OpenAiModelMetaData(@NonNull String model, Integer contextSize, Integer maxNewTokens, SupportedApi[] api,
				CallType supportedCallType, Mode[] inputModes, Mode[] outputModes) {
			super(model, OpenAiTokenizer.getTokenizer(model), contextSize, maxNewTokens, inputModes, outputModes);
			this.supportedCallType = supportedCallType;
			Collections.addAll(this.supportedApis, api);
		}

		/**
		 * Copy constructor; this is to use data from a model (e.g. gpt-4o) for another
		 * versioned model (e.g. gpt-4o-2025-02-03).
		 * 
		 * @param model
		 * @param other
		 */
		public OpenAiModelMetaData(@NonNull String model, OpenAiModelMetaData other) {
			super(model, null, other.getContextSize(), other.getMaxNewTokens(), other.getInputModes(),
					other.getOutputModes());
			this.tokenizer = new OpenAiTokenizer(model,
					other.getTokenizer() == null ? null : other.getTokenizer().getEncoding());
			this.supportedCallType = other.getSupportedCallType();
			this.supportedApis.addAll(other.getSupportedApis());
		}
	}

	// Constants for supported APIs and input modes
	private final static SupportedApi[] CHAT = new SupportedApi[] { SupportedApi.CHAT };
	private final static SupportedApi[] COMPLETIONS = new SupportedApi[] { SupportedApi.COMPLETIONS };
	private final static SupportedApi[] CHAT_RESPONSES = new SupportedApi[] { SupportedApi.CHAT,
			SupportedApi.RESPONSES };
	private final static SupportedApi[] CHAT_RESPONSES_BATCH = new SupportedApi[] { SupportedApi.CHAT,
			SupportedApi.RESPONSES, SupportedApi.BATCH };
	private final static SupportedApi[] CHAT_ASSISTANTS = new SupportedApi[] { SupportedApi.CHAT,
			SupportedApi.ASSISTANTS };
	private final static SupportedApi[] CHAT_RESPONSES_ASSISTANTS = new SupportedApi[] { SupportedApi.CHAT,
			SupportedApi.RESPONSES, SupportedApi.ASSISTANTS };
	private final static SupportedApi[] RESPONSES_BATCH = new SupportedApi[] { SupportedApi.RESPONSES,
			SupportedApi.BATCH };
	private final static SupportedApi[] CHAT_RESPONSES_ASSISTANTS_BATCH = new SupportedApi[] { SupportedApi.CHAT,
			SupportedApi.RESPONSES, SupportedApi.ASSISTANTS, SupportedApi.BATCH };
	private final static SupportedApi[] CHAT_RESPONSES_BATCH_FINE_TUNING = new SupportedApi[] { SupportedApi.CHAT,
			SupportedApi.RESPONSES, SupportedApi.BATCH, SupportedApi.FINE_TUNING };
	private final static SupportedApi[] CHAT_RESPONSES_ASSISTANTS_BATCH_FINE_TUNING = new SupportedApi[] {
			SupportedApi.CHAT, SupportedApi.RESPONSES, SupportedApi.ASSISTANTS, SupportedApi.BATCH,
			SupportedApi.FINE_TUNING };
	private final static SupportedApi[] REALTIME = new SupportedApi[] { SupportedApi.REALTIME };
	private final static SupportedApi[] IMAGES = new SupportedApi[] { SupportedApi.IMAGES };
	private final static SupportedApi[] SPEECH_GENERATION = new SupportedApi[] { SupportedApi.SPEECH_GENERATION };
	private final static SupportedApi[] TRANSLATION_TRANSCRIPTION = new SupportedApi[] { SupportedApi.TRANSLATION,
			SupportedApi.TRANSCRIPTION };
	private final static SupportedApi[] REALTIME_TRANSCRIPTION = new SupportedApi[] { SupportedApi.REALTIME,
			SupportedApi.TRANSCRIPTION };
	private final static SupportedApi[] BATCH_EMBEDDINGS = new SupportedApi[] { SupportedApi.BATCH,
			SupportedApi.EMBEDDINGS };
	private final static SupportedApi[] MODERATION = new SupportedApi[] { SupportedApi.MODERATION };
	private final static Mode[] TEXT = new Mode[] { Mode.TEXT };
	private final static Mode[] IMAGE = new Mode[] { Mode.IMAGE };
	private final static Mode[] AUDIO = new Mode[] { Mode.AUDIO };
	private final static Mode[] EMBEDDINGS = new Mode[] { Mode.EMBEDDINGS };
	private final static Mode[] TEXT_IMAGE = new Mode[] { Mode.TEXT, Mode.IMAGE };
	private final static Mode[] TEXT_AUDIO = new Mode[] { Mode.TEXT, Mode.AUDIO };

	/**
	 * Maps each OpenAI model into its metadata
	 */
	final static Map<String, ModelMetaData> MODEL_CONFIG = new ConcurrentHashMap<>();
	static {

		// Notice class is smart enough to return model checkpoints like
		// o3-mini-2025-01-31 returning data for o3-mini

		// Reasoning Models
		MODEL_CONFIG.put("o3",
				new OpenAiModelMetaData("o3", 200_000, 100_000, CHAT_RESPONSES_BATCH, CallType.TOOLS, TEXT_IMAGE));
		MODEL_CONFIG.put("o1", new OpenAiModelMetaData("o1", 200_000, 100_000, CHAT_RESPONSES_ASSISTANTS_BATCH,
				CallType.TOOLS, TEXT_IMAGE));
		MODEL_CONFIG.put("o1-preview",
				new OpenAiModelMetaData("o1-preview", 128_000, 32_768, CHAT_ASSISTANTS, CallType.NONE));
		MODEL_CONFIG.put("o1-pro",
				new OpenAiModelMetaData("o1-pro", 200_000, 100_000, RESPONSES_BATCH, CallType.TOOLS, TEXT_IMAGE));

		// Flagship Chat Models
		MODEL_CONFIG.put("gpt-4.1", new OpenAiModelMetaData("gpt-4.1", 1_047_576, 32_768,
				CHAT_RESPONSES_ASSISTANTS_BATCH, CallType.TOOLS, TEXT_IMAGE));
		MODEL_CONFIG.put("gpt-4o", new OpenAiModelMetaData("gpt-4o", 128_000, 16_384,
				CHAT_RESPONSES_ASSISTANTS_BATCH_FINE_TUNING, CallType.TOOLS, TEXT_IMAGE));
		MODEL_CONFIG.put("gpt-4o-audio-preview", new OpenAiModelMetaData("gpt-4o-audio-preview", 128_000, 16_384, CHAT,
				CallType.TOOLS, TEXT_AUDIO, TEXT_AUDIO));
		MODEL_CONFIG.put("chatgpt-4o-latest", new OpenAiModelMetaData("chatgpt-4o-latest", 128_000, 16_384,
				CHAT_RESPONSES, CallType.NONE, TEXT_IMAGE));

		// Cost-optimized Models
		MODEL_CONFIG.put("o4-mini",
				new OpenAiModelMetaData("o4-mini", 200_000, 100_000, CHAT_RESPONSES_BATCH, CallType.TOOLS, TEXT_IMAGE));
		MODEL_CONFIG.put("gpt-4.1-mini", new OpenAiModelMetaData("gpt-4.1-mini", 1_047_576, 32_768,
				CHAT_RESPONSES_ASSISTANTS_BATCH_FINE_TUNING, CallType.TOOLS, TEXT_IMAGE));
		MODEL_CONFIG.put("gpt-4.1-nano", new OpenAiModelMetaData("gpt-4.1-nano", 1_047_576, 32_768,
				CHAT_RESPONSES_ASSISTANTS_BATCH_FINE_TUNING, CallType.TOOLS, TEXT_IMAGE));
		MODEL_CONFIG.put("o3-mini",
				new OpenAiModelMetaData("o3-mini", 200_000, 100_000, CHAT_RESPONSES_ASSISTANTS_BATCH, CallType.TOOLS));
		MODEL_CONFIG.put("gpt-4o-mini", new OpenAiModelMetaData("gpt-4o-mini", 128_000, 16_384,
				CHAT_RESPONSES_ASSISTANTS_BATCH_FINE_TUNING, CallType.TOOLS, TEXT_IMAGE));
		MODEL_CONFIG.put("gpt-4o-mini-audio-preview", new OpenAiModelMetaData("gpt-4o-mini-audio-preview", 128_000,
				16_384, CHAT, CallType.TOOLS, TEXT_AUDIO, TEXT_AUDIO));
		MODEL_CONFIG.put("o1-mini",
				new OpenAiModelMetaData("o1-mini", 128_000, 65_536, CHAT_RESPONSES_ASSISTANTS, CallType.NONE));

		// Realtime Models
		MODEL_CONFIG.put("gpt-4o-realtime-preview", new OpenAiModelMetaData("gpt-4o-realtime-preview", 128_000, 4_096,
				REALTIME, CallType.TOOLS, TEXT_AUDIO, TEXT_AUDIO));
		MODEL_CONFIG.put("gpt-4o-mini-realtime-preview", new OpenAiModelMetaData("gpt-4o-mini-realtime-preview",
				128_000, 4_096, REALTIME, CallType.TOOLS, TEXT_AUDIO, TEXT_AUDIO));

		// DALL-E
		MODEL_CONFIG.put("gpt-image-1", new OpenAiModelMetaData("gpt-image-1", IMAGES, TEXT_IMAGE, IMAGE));
		MODEL_CONFIG.put("dall-e-2", new OpenAiModelMetaData("dall-e-2", IMAGES, TEXT, IMAGE));
		MODEL_CONFIG.put("dall-e-3", new OpenAiModelMetaData("dall-e-3", IMAGES, TEXT, IMAGE));

		// TTS
		MODEL_CONFIG.put("gpt-4o-mini-tts",
				new OpenAiModelMetaData("gpt-4o-mini-tts", 2_000, SPEECH_GENERATION, TEXT, AUDIO));
		MODEL_CONFIG.put("tts-1", new OpenAiModelMetaData("tts-1", 2_046, SPEECH_GENERATION, TEXT, AUDIO));
		MODEL_CONFIG.put("tts-1-1106", new OpenAiModelMetaData("tts-1-1106", 2046, SPEECH_GENERATION, TEXT, AUDIO));
		MODEL_CONFIG.put("tts-1-hd", new OpenAiModelMetaData("tts-1-hd", 2_046, SPEECH_GENERATION, TEXT, AUDIO));
		MODEL_CONFIG.put("tts-1-hd-1106",
				new OpenAiModelMetaData("tts-1-hd-1106", 2_046, SPEECH_GENERATION, TEXT, AUDIO));

		// Transcription
		MODEL_CONFIG.put("gpt-4o-transcribe",
				new OpenAiModelMetaData("gpt-4o-transcribe", 16_000, 2_000, REALTIME_TRANSCRIPTION, TEXT_AUDIO, TEXT));
		MODEL_CONFIG.put("gpt-4o-mini-transcribe", new OpenAiModelMetaData("gpt-4o-mini-transcribe", 16_000, 2_000,
				REALTIME_TRANSCRIPTION, TEXT_AUDIO, TEXT));
		MODEL_CONFIG.put("whisper-1", new OpenAiModelMetaData("whisper-1", TRANSLATION_TRANSCRIPTION, AUDIO, TEXT));

		// Tool Specific models
		MODEL_CONFIG.put("gpt-4o-search-preview",
				new OpenAiModelMetaData("gpt-4o-search-preview", 128_000, 16_384, CHAT, TEXT, TEXT));
		MODEL_CONFIG.put("gpt-4o-mini-search-preview",
				new OpenAiModelMetaData("gpt-4o-mini-search-preview", 128_000, 16_384, CHAT, TEXT, TEXT));
		MODEL_CONFIG.put("computer-use-preview",
				new OpenAiModelMetaData("computer-use-preview", 8_192, 1_024, RESPONSES_BATCH, TEXT_IMAGE, TEXT));

		// Embeddings
		MODEL_CONFIG.put("text-embedding-3-large",
				new OpenAiModelMetaData("text-embedding-3-large", 8191, BATCH_EMBEDDINGS, TEXT, EMBEDDINGS));
		MODEL_CONFIG.put("text-embedding-3-small",
				new OpenAiModelMetaData("text-embedding-3-small", 8192, BATCH_EMBEDDINGS, TEXT, EMBEDDINGS));
		MODEL_CONFIG.put("text-embedding-ada-002",
				new OpenAiModelMetaData("text-embedding-ada-002", 8192, BATCH_EMBEDDINGS, TEXT, EMBEDDINGS));

		// Moderation
		MODEL_CONFIG.put("omni-moderation-latest",
				new OpenAiModelMetaData("omni-moderation-latest", MODERATION, TEXT_IMAGE, TEXT));
		MODEL_CONFIG.put("omni-moderation-2024-09-26",
				new OpenAiModelMetaData("omni-moderation-2024-09-26", MODERATION, TEXT_IMAGE, TEXT));
//		MODEL_CONFIG.put("text-moderation-latest",
//				new OpenAiModelMetaData("text-moderation-latest", null, 32_768, MODERATION, TEXT, TEXT));
//		MODEL_CONFIG.put("text-moderation-007",
//				new OpenAiModelMetaData("text-moderation-007", null, 32_768, MODERATION, TEXT, TEXT));
//		MODEL_CONFIG.put("text-moderation-stable",
//				new OpenAiModelMetaData("text-moderation-stable", null, 32_768, MODERATION, TEXT, TEXT));

		// Older GPT Models
		MODEL_CONFIG.put("gpt-3.5-turbo", new OpenAiModelMetaData("gpt-3.5-turbo", 16_385, 4_096,
				CHAT_RESPONSES_BATCH_FINE_TUNING, CallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-3.5-turbo-0125", new OpenAiModelMetaData("gpt-3.5-turbo-0125", 16_385, 4_096,
				CHAT_RESPONSES_BATCH_FINE_TUNING, CallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-3.5-turbo-1106", new OpenAiModelMetaData("gpt-3.5-turbo-1106", 16_385, 4_096,
				CHAT_RESPONSES_BATCH_FINE_TUNING, CallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-3.5-turbo-instruct",
				new OpenAiModelMetaData("gpt-3.5-turbo-instruct", 4_096, 4_096, COMPLETIONS, CallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-3.5-turbo-instruct-0914",
				new OpenAiModelMetaData("gpt-3.5-turbo-instruct-0914", 4096, SupportedApi.COMPLETIONS));
//		MODEL_CONFIG.put("gpt-3.5-turbo-16k",
//				new OpenAiModelMetaData("gpt-3.5-turbo-16k", 16384, null, COMPLETIONS, CallType.FUNCTIONS));
//		MODEL_CONFIG.put("gpt-3.5-turbo-16k-0613",
//				new OpenAiModelMetaData("gpt-3.5-turbo-16k-0613", 16384, null, COMPLETIONS, CallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-4-turbo", new OpenAiModelMetaData("gpt-4-turbo", 128_000, 4_096,
				CHAT_RESPONSES_ASSISTANTS_BATCH, CallType.TOOLS, TEXT_IMAGE));
		MODEL_CONFIG.put("gpt-4-turbo-preview", new OpenAiModelMetaData("gpt-4-turbo-preview", 128_000, 4_096,
				CHAT_RESPONSES_ASSISTANTS, CallType.TOOLS));
		MODEL_CONFIG.put("gpt-4-0125-preview", new OpenAiModelMetaData("gpt-4-0125-preview", 128_000, 4_096,
				CHAT_RESPONSES_ASSISTANTS, CallType.TOOLS));
//		MODEL_CONFIG.put("gpt-4-1106-vision-preview", new OpenAiModelMetaData("gpt-4-1106-vision-preview", 128_000,
//				4_096, CHAT_RESPONSES_ASSISTANTS, CallType.TOOLS, TEXT_IMAGE));
		MODEL_CONFIG.put("gpt-4", new OpenAiModelMetaData("gpt-4", 8_192, 8_192,
				CHAT_RESPONSES_ASSISTANTS_BATCH_FINE_TUNING, CallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-4-1106-preview",
				new OpenAiModelMetaData("gpt-4-1106-preview", 128_000, 4_096, CHAT_ASSISTANTS, CallType.TOOLS));
		MODEL_CONFIG.put("gpt-4-0613", new OpenAiModelMetaData("gpt-4-0613", 8_192, 8_192,
				CHAT_RESPONSES_ASSISTANTS_BATCH_FINE_TUNING, CallType.FUNCTIONS));
//		MODEL_CONFIG.put("gpt-4-0314", new OpenAiModelMetaData("gpt-4-0314", 8_192, 8_192,
//				CHAT_RESPONSES_ASSISTANTS_BATCH, CallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-4.5-preview", new OpenAiModelMetaData("gpt-4.5-preview", 128_000, 16_384,
				CHAT_RESPONSES_ASSISTANTS, CallType.TOOLS, TEXT_IMAGE, TEXT));

		// GPT Base Models
		MODEL_CONFIG.put("babbage-002", new OpenAiModelMetaData("babbage-002", 16384, SupportedApi.COMPLETIONS));
		MODEL_CONFIG.put("davinci-002", new OpenAiModelMetaData("davinci-002", 16385, SupportedApi.COMPLETIONS));
	}

	/**
	 * For testing purpose only.
	 */
	protected static Set<@NonNull String> getModelsMetadata() {
		return MODEL_CONFIG.values().stream() //
				.map(ModelMetaData::getModel) //
				.collect(Collectors.toSet());
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

	private final static Pattern P = Pattern.compile("^(.+)-(\\d{4}-\\d{2}-\\d{2})$");

	@Override
	public OpenAiModelMetaData get(@NonNull String model) {
		// Note this gets same data for model and model-yyyy-mm-dd
		OpenAiModelMetaData data = (OpenAiModelMetaData) super.get(model);
		if (data != null)
			return data;

		Matcher m = P.matcher(model);
		if (m.matches()) { // model seems to be a snapshot
			// Try to get the value for corresponding model without date
			data = (OpenAiModelMetaData) super.get(m.group(1));
			if (data != null)
				return new OpenAiModelMetaData(model, data);
		}
		return data;
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
	public CallType getSupportedCallType(@NonNull String model) {
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
	public List<SupportedApi> getSupportedApis(@NonNull String model) {
		OpenAiModelMetaData data = get(model);
		if (data == null)
			throw new IllegalArgumentException(
					"No metadata found for model " + model + ". Consider registering model data");
		return data.getSupportedApis();
	}

	public static void main(String[] args) {
		try (OpenAiEndpoint ep = new OpenAiEndpoint(); OpenAiModelService instance = ep.getModelService();) {
			OpenAiModelMetaData data = instance.get("o3-mini");
			System.out.println(data.toString());
			data = instance.get("o3-mini-2025-01-31");
			System.out.println(data.toString());
		}
	}
}