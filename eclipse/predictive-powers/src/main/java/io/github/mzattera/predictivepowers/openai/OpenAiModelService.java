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

package io.github.mzattera.predictivepowers.openai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.openai.models.models.Model;

import io.github.mzattera.predictivepowers.EndpointException;
import io.github.mzattera.predictivepowers.openai.OpenAiModelService.OpenAiModelMetaData.CallType;
import io.github.mzattera.predictivepowers.openai.OpenAiModelService.OpenAiModelMetaData.SupportedApi;
import io.github.mzattera.predictivepowers.services.AbstractModelService;
import io.github.mzattera.predictivepowers.services.ModelService.ModelMetaData.Modality;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Service for working with OpenAI models.
 * 
 * This class is thread-safe.
 * 
 * @author Massimiliano "Maxi" Zattera
 * @author Luna
 */
public class OpenAiModelService extends AbstractModelService {

	// TODO: When introducing Azure this need to be changed so deployments can be
	// mapped into actual modes to get their parameters

	@Getter
	@SuperBuilder(toBuilder = true)
	@ToString(callSuper = true)
	public static class OpenAiModelMetaData extends ModelMetaData {

		public enum CallType {
			NONE, FUNCTIONS, TOOLS
		}

		public enum SupportedApi {
			CHAT, RESPONSES, REALTIME, ASSISTANTS, BATCH, FINE_TUNING, EMBEDDINGS, IMAGES, SPEECH_GENERATION,
			TRANSCRIPTION, TRANSLATION, MODERATION, COMPLETIONS, VIDEOS
		}

		@lombok.Builder.Default
		private final @NonNull CallType supportedCallType = CallType.NONE;

		@Singular("supportedApi")
		private final @NonNull List<SupportedApi> supportedApis;

		public boolean supportsApi(SupportedApi api) {
			return supportedApis.contains(api);
		}

		@lombok.Builder.Default
		private boolean structuredOutput = true;

		/**
		 * 
		 * @return True if the model supports structured outputs.
		 * 
		 * @see <a href=
		 *      "https://platform.openai.com/docs/guides/structured-outputs">strict
		 *      mode</a>
		 */
		public boolean supportsStructuredOutput() {
			return structuredOutput;
		}

		@lombok.Builder.Default
		private final boolean strictModeToolCall = true;

		/**
		 * 
		 * @return True if the model supports strict mode in tool calls (unfortunately,
		 *         not all models do).
		 * 
		 * @see <a href=
		 *      "https://platform.openai.com/docs/guides/structured-outputs">strict
		 *      mode</a>
		 */
		public boolean supportsStrictModeToolCall() {
			return (supportedCallType != CallType.TOOLS ? false : strictModeToolCall);
		}

		@Override
		public OpenAiTokenizer getTokenizer() {
			return (OpenAiTokenizer) super.getTokenizer();
		}
	}

	private static final List<SupportedApi> CHAT = List.of(SupportedApi.CHAT);
	private static final List<SupportedApi> COMPLETIONS = List.of(SupportedApi.COMPLETIONS);
	private static final List<SupportedApi> CHAT_RESPONSES = List.of(SupportedApi.CHAT, SupportedApi.RESPONSES);
	private static final List<SupportedApi> CHAT_RESPONSES_BATCH = List.of(SupportedApi.CHAT, SupportedApi.RESPONSES,
			SupportedApi.BATCH);
	private static final List<SupportedApi> CHAT_ASSISTANTS = List.of(SupportedApi.CHAT, SupportedApi.ASSISTANTS);
	private static final List<SupportedApi> CHAT_RESPONSES_ASSISTANTS = List.of(SupportedApi.CHAT,
			SupportedApi.RESPONSES, SupportedApi.ASSISTANTS);
	private static final List<SupportedApi> RESPONSES_BATCH = List.of(SupportedApi.RESPONSES, SupportedApi.BATCH);
	private static final List<SupportedApi> CHAT_RESPONSES_ASSISTANTS_BATCH = List.of(SupportedApi.CHAT,
			SupportedApi.RESPONSES, SupportedApi.ASSISTANTS, SupportedApi.BATCH);
	private static final List<SupportedApi> CHAT_RESPONSES_BATCH_FINE_TUNING = List.of(SupportedApi.CHAT,
			SupportedApi.RESPONSES, SupportedApi.BATCH, SupportedApi.FINE_TUNING);
	private static final List<SupportedApi> CHAT_RESPONSES_ASSISTANTS_BATCH_FINE_TUNING = List.of(SupportedApi.CHAT,
			SupportedApi.RESPONSES, SupportedApi.ASSISTANTS, SupportedApi.BATCH, SupportedApi.FINE_TUNING);
	private static final List<SupportedApi> REALTIME = List.of(SupportedApi.REALTIME);
	private static final List<SupportedApi> IMAGES = List.of(SupportedApi.IMAGES);

	private static final List<SupportedApi> SPEECH_GENERATION = List.of(SupportedApi.SPEECH_GENERATION);
	private static final List<SupportedApi> TRANSLATION_TRANSCRIPTION = List.of(SupportedApi.TRANSLATION,
			SupportedApi.TRANSCRIPTION);
	private static final List<SupportedApi> REALTIME_TRANSCRIPTION = List.of(SupportedApi.REALTIME,
			SupportedApi.TRANSCRIPTION);
	private static final List<SupportedApi> BATCH_EMBEDDINGS = List.of(SupportedApi.BATCH, SupportedApi.EMBEDDINGS);
	private static final List<SupportedApi> MODERATION = List.of(SupportedApi.MODERATION);

	private static final List<Modality> TEXT = List.of(Modality.TEXT);
	private static final List<Modality> IMAGE = List.of(Modality.IMAGE);
	private static final List<Modality> AUDIO = List.of(Modality.AUDIO);
	private static final List<Modality> EMBEDDINGS = List.of(Modality.EMBEDDINGS);
	private static final List<Modality> TEXT_IMAGE = List.of(Modality.TEXT, Modality.IMAGE);
	private static final List<Modality> TEXT_AUDIO = List.of(Modality.TEXT, Modality.AUDIO);
	private static final List<Modality> TEXT_IMAGE_AUDIO = List.of(Modality.TEXT, Modality.AUDIO, Modality.IMAGE);

	/* ====================================================================== */
	/* MODEL CONFIGURATION */
	/* ====================================================================== */

	private static final Map<String, ModelMetaData> MODEL_CONFIG = new ConcurrentHashMap<>();

	static {

		// Reasoning Models
		MODEL_CONFIG.put("o3", OpenAiModelMetaData.builder() //
				.model("o3") //
				.tokenizer(OpenAiTokenizer.getTokenizer("o3")) //
				.contextSize(200_000) //
				.maxNewTokens(100_000) //
				.inputModes(TEXT_IMAGE) //
				.outputModes(TEXT_IMAGE) //
				.strictModeToolCall(false).supportedApis(CHAT_RESPONSES_BATCH) //
				.supportedCallType(CallType.TOOLS).build());

		MODEL_CONFIG.put("o1", OpenAiModelMetaData.builder() //
				.model("o1") //
				.tokenizer(OpenAiTokenizer.getTokenizer("o1")) //
				.contextSize(200_000) //
				.maxNewTokens(100_000) //
				.inputModes(TEXT_IMAGE) //
				.outputModes(TEXT_IMAGE) //
				.strictModeToolCall(false).supportedApis(CHAT_RESPONSES_ASSISTANTS_BATCH) //
				.supportedCallType(CallType.TOOLS).build());

		MODEL_CONFIG.put("o1-pro",
				OpenAiModelMetaData.builder().model("o1-pro").tokenizer(OpenAiTokenizer.getTokenizer("o1-pro"))
						.contextSize(200_000).maxNewTokens(100_000).inputModes(TEXT_IMAGE).outputModes(TEXT_IMAGE)
						.supportedApis(RESPONSES_BATCH).supportedCallType(CallType.TOOLS).build());

		MODEL_CONFIG.put("codex-mini-latest",
				OpenAiModelMetaData.builder().model("codex-mini-latest")
						.tokenizer(OpenAiTokenizer.getTokenizer("codex-mini-latest")).contextSize(200_000)
						.maxNewTokens(100_000).inputModes(TEXT_IMAGE).outputModes(TEXT)
						.supportedApis(List.of(SupportedApi.RESPONSES)).supportedCallType(CallType.TOOLS).build());

		MODEL_CONFIG.put("gpt-5-codex",
				OpenAiModelMetaData.builder().model("gpt-5-codex")
						.tokenizer(OpenAiTokenizer.getTokenizer("gpt-5-codex")).contextSize(400_000)
						.maxNewTokens(128_000).inputModes(TEXT_IMAGE).outputModes(TEXT)
						.supportedApis(List.of(SupportedApi.RESPONSES)).supportedCallType(CallType.TOOLS).build());

		// Flagship Chat Models
		MODEL_CONFIG.put("gpt-4.1",
				OpenAiModelMetaData.builder().model("gpt-4.1").tokenizer(OpenAiTokenizer.getTokenizer("gpt-4.1"))
						.contextSize(1_047_576).maxNewTokens(32_768).inputModes(TEXT_IMAGE).outputModes(TEXT_IMAGE)
						.supportedApis(CHAT_RESPONSES_ASSISTANTS_BATCH).supportedCallType(CallType.TOOLS).build());

		MODEL_CONFIG.put("gpt-4o",
				OpenAiModelMetaData.builder().model("gpt-4o").tokenizer(OpenAiTokenizer.getTokenizer("gpt-4o"))
						.contextSize(128_000).maxNewTokens(16_384).inputModes(TEXT_IMAGE).outputModes(TEXT_IMAGE)
						.supportedApis(CHAT_RESPONSES_ASSISTANTS_BATCH_FINE_TUNING).supportedCallType(CallType.TOOLS)
						.build());

		// Needed because structured output and ctx size are different than gpt-4o
		MODEL_CONFIG.put("gpt-4o-2024-05-13",
				OpenAiModelMetaData.builder().model("gpt-4o-2024-05-13")
						.tokenizer(OpenAiTokenizer.getTokenizer("gpt-4o")).contextSize(128_000).maxNewTokens(4096)
						.inputModes(TEXT_IMAGE).outputModes(TEXT_IMAGE)
						.supportedApis(CHAT_RESPONSES_ASSISTANTS_BATCH_FINE_TUNING).supportedCallType(CallType.TOOLS)
						.structuredOutput(false).build());

		// Structured output and max new tokens different than 4o
		MODEL_CONFIG.put("gpt-4o-audio-preview",
				OpenAiModelMetaData.builder().model("gpt-4o-audio-preview")
						.tokenizer(OpenAiTokenizer.getTokenizer("gpt-4o-audio-preview")).contextSize(128_000)
						.maxNewTokens(16_384).inputModes(TEXT_AUDIO).outputModes(TEXT_AUDIO).supportedApis(CHAT)
						.supportedCallType(CallType.TOOLS).build());

		MODEL_CONFIG.put("chatgpt-4o-latest", OpenAiModelMetaData.builder().model("chatgpt-4o-latest")
				.tokenizer(OpenAiTokenizer.getTokenizer("chatgpt-4o-latest")).contextSize(128_000).maxNewTokens(16_384)
				.inputModes(TEXT_IMAGE).outputModes(TEXT_IMAGE).supportedApis(CHAT_RESPONSES)
				.supportedCallType(CallType.NONE).structuredOutput(false).build());

		MODEL_CONFIG.put("gpt-5",
				OpenAiModelMetaData.builder().model("gpt-5").tokenizer(OpenAiTokenizer.getTokenizer("gpt-5"))
						.contextSize(272_000).maxNewTokens(128_000).inputModes(TEXT_IMAGE).outputModes(TEXT)
						.supportedApis(CHAT_RESPONSES_BATCH).supportedCallType(CallType.TOOLS).build());

		MODEL_CONFIG.put("gpt-5-mini",
				OpenAiModelMetaData.builder().model("gpt-5-mini").tokenizer(OpenAiTokenizer.getTokenizer("gpt-5-mini"))
						.contextSize(272_000).maxNewTokens(128_000).inputModes(TEXT_IMAGE).outputModes(TEXT)
						.supportedApis(CHAT_RESPONSES_BATCH).supportedCallType(CallType.TOOLS).build());

		MODEL_CONFIG.put("gpt-5-nano",
				OpenAiModelMetaData.builder().model("gpt-5-nano").tokenizer(OpenAiTokenizer.getTokenizer("gpt-5-nano"))
						.contextSize(272_000).maxNewTokens(128_000).inputModes(TEXT_IMAGE).outputModes(TEXT)
						.supportedApis(CHAT_RESPONSES_BATCH).supportedCallType(CallType.TOOLS).build());

		MODEL_CONFIG.put("gpt-5-pro",
				OpenAiModelMetaData.builder().model("gpt-5-pro").tokenizer(OpenAiTokenizer.getTokenizer("gpt-5-pro"))
						.contextSize(400_000).maxNewTokens(272_000).inputModes(TEXT_IMAGE).outputModes(TEXT)
						.supportedApis(RESPONSES_BATCH).supportedCallType(CallType.TOOLS).build());

		MODEL_CONFIG.put("gpt-5-chat-latest",
				OpenAiModelMetaData.builder().model("gpt-5-chat-latest")
						.tokenizer(OpenAiTokenizer.getTokenizer("gpt-5-chat-latest")).contextSize(128_000)
						.maxNewTokens(16_384).inputModes(TEXT_IMAGE).outputModes(TEXT).supportedApis(CHAT_RESPONSES)
						.supportedCallType(CallType.TOOLS).build());

		MODEL_CONFIG.put("gpt-5.1", OpenAiModelMetaData.builder().model("gpt-5.1") //
				.tokenizer(OpenAiTokenizer.getTokenizer("gpt-5.1")) //
				.contextSize(272_000) //
				.maxNewTokens(128_000) //
				.inputModes(TEXT_IMAGE) //
				.outputModes(TEXT) //
				.supportedApis(CHAT_RESPONSES) //
				.supportedCallType(CallType.TOOLS) //
				.structuredOutput(true).build());

		MODEL_CONFIG.put("gpt-5.1-chat-latest", OpenAiModelMetaData.builder().model("gpt-5.1-chat-latest") //
				.tokenizer(OpenAiTokenizer.getTokenizer("gpt-5.1-chat-latest")) //
				.contextSize(272_000) //
				.maxNewTokens(131_072) //
				.inputModes(TEXT_IMAGE) //
				.outputModes(TEXT) //
				.supportedApis(CHAT_RESPONSES) //
				.supportedCallType(CallType.TOOLS) //
				.structuredOutput(true).build());

		MODEL_CONFIG.put("gpt-5.1-codex", OpenAiModelMetaData.builder().model("gpt-5.1-codex") //
				.tokenizer(OpenAiTokenizer.getTokenizer("gpt-5.1-codex")) //
				.contextSize(400_000) //
				.maxNewTokens(128_000) //
				.inputModes(TEXT_IMAGE) //
				.outputModes(TEXT) //
				.supportedApis(List.of(SupportedApi.RESPONSES)) //
				.supportedCallType(CallType.TOOLS) //
				.structuredOutput(true).build());

		MODEL_CONFIG.put("gpt-5.1-codex-max", OpenAiModelMetaData.builder().model("gpt-5.1-codex-max") //
				.tokenizer(OpenAiTokenizer.getTokenizer("gpt-5.1-codex-max")) //
				.contextSize(400_000) //
				.maxNewTokens(128_000) //
				.inputModes(TEXT_IMAGE) //
				.outputModes(TEXT) //
				.supportedApis(List.of(SupportedApi.RESPONSES)) //
				.supportedCallType(CallType.TOOLS) //
				.structuredOutput(true).build());

		MODEL_CONFIG.put("gpt-5.1-codex-mini", OpenAiModelMetaData.builder().model("gpt-5.1-codex-mini") //
				.tokenizer(OpenAiTokenizer.getTokenizer("gpt-5.1-codex-mini")) //
				.contextSize(400_000) //
				.maxNewTokens(128_000) //
				.inputModes(TEXT_IMAGE) //
				.outputModes(TEXT) //
				.supportedApis(List.of(SupportedApi.RESPONSES)) //
				.supportedCallType(CallType.TOOLS) //
				.structuredOutput(true).build());

		MODEL_CONFIG.put("gpt-5.2", OpenAiModelMetaData.builder().model("gpt-5.2") //
				.tokenizer(OpenAiTokenizer.getTokenizer("gpt-5.2")) //
				.contextSize(272_000) //
				.maxNewTokens(128_000) //
				.inputModes(TEXT_IMAGE) //
				.outputModes(TEXT) //
				.supportedApis(CHAT_RESPONSES) //
				.supportedCallType(CallType.TOOLS) //
				.structuredOutput(true).build());

		MODEL_CONFIG.put("gpt-5.2-pro", OpenAiModelMetaData.builder().model("gpt-5.2-pro") //
				.tokenizer(OpenAiTokenizer.getTokenizer("gpt-5.2-pro")) //
				.contextSize(400_000) //
				.maxNewTokens(128_000) //
				.inputModes(TEXT_IMAGE) //
				.outputModes(TEXT) //
				.supportedApis(List.of(SupportedApi.RESPONSES)) //
				.supportedCallType(CallType.TOOLS) //
				.structuredOutput(false).build());

		MODEL_CONFIG.put("gpt-5.2-chat-latest", OpenAiModelMetaData.builder().model("gpt-5.2-chat-latest") //
				.tokenizer(OpenAiTokenizer.getTokenizer("gpt-5.2-chat-latest")) //
				.contextSize(272_000) //
				.maxNewTokens(128_000) //
				.inputModes(TEXT_IMAGE) //
				.outputModes(TEXT) //
				.supportedApis(CHAT_RESPONSES) //
				.supportedCallType(CallType.TOOLS) //
				.structuredOutput(true).build());

		// Cost-optimized Models
		MODEL_CONFIG.put("o4-mini", OpenAiModelMetaData.builder() //
				.model("o4-mini") //
				.tokenizer(OpenAiTokenizer.getTokenizer("o4-mini")) //
				.contextSize(200_000) //
				.maxNewTokens(100_000) //
				.inputModes(TEXT_IMAGE) //
				.outputModes(TEXT_IMAGE) //
				.strictModeToolCall(false).supportedApis(CHAT_RESPONSES_BATCH) //
				.supportedCallType(CallType.TOOLS).build());

		MODEL_CONFIG.put("gpt-4.1-mini", OpenAiModelMetaData.builder().model("gpt-4.1-mini")
				.tokenizer(OpenAiTokenizer.getTokenizer("gpt-4.1-mini")).contextSize(1_047_576).maxNewTokens(32_768)
				.inputModes(TEXT_IMAGE).outputModes(TEXT_IMAGE)
				.supportedApis(CHAT_RESPONSES_ASSISTANTS_BATCH_FINE_TUNING).supportedCallType(CallType.TOOLS).build());

		MODEL_CONFIG.put("gpt-4.1-nano", OpenAiModelMetaData.builder().model("gpt-4.1-nano")
				.tokenizer(OpenAiTokenizer.getTokenizer("gpt-4.1-nano")).contextSize(1_047_576).maxNewTokens(32_768)
				.inputModes(TEXT_IMAGE).outputModes(TEXT_IMAGE)
				.supportedApis(CHAT_RESPONSES_ASSISTANTS_BATCH_FINE_TUNING).supportedCallType(CallType.TOOLS).build());

		MODEL_CONFIG.put("o3-mini", OpenAiModelMetaData.builder() //
				.model("o3-mini") //
				.tokenizer(OpenAiTokenizer.getTokenizer("o3-mini")) //
				.contextSize(200_000) //
				.maxNewTokens(100_000) //
				.inputModes(TEXT) //
				.outputModes(TEXT) //
				.strictModeToolCall(false).supportedApis(CHAT_RESPONSES_ASSISTANTS_BATCH) //
				.supportedCallType(CallType.TOOLS).build());

		MODEL_CONFIG.put("o3-pro", OpenAiModelMetaData.builder() //
				.model("o3-pro") //
				.tokenizer(OpenAiTokenizer.getTokenizer("o3-pro")) //
				.contextSize(200_000) //
				.maxNewTokens(100_000) //
				.inputModes(TEXT_IMAGE) //
				.outputModes(TEXT) //
				.strictModeToolCall(false).supportedApis(RESPONSES_BATCH) //
				.supportedCallType(CallType.TOOLS).build());

		MODEL_CONFIG.put("gpt-4o-mini", OpenAiModelMetaData.builder().model("gpt-4o-mini")
				.tokenizer(OpenAiTokenizer.getTokenizer("gpt-4o-mini")).contextSize(128_000).maxNewTokens(16_384)
				.inputModes(TEXT_IMAGE).outputModes(TEXT_IMAGE)
				.supportedApis(CHAT_RESPONSES_ASSISTANTS_BATCH_FINE_TUNING).supportedCallType(CallType.TOOLS).build());

		MODEL_CONFIG.put("gpt-4o-mini-audio-preview",
				OpenAiModelMetaData.builder().model("gpt-4o-mini-audio-preview")
						.tokenizer(OpenAiTokenizer.getTokenizer("gpt-4o-mini-audio-preview")).contextSize(128_000)
						.maxNewTokens(16_384).inputModes(TEXT_AUDIO).outputModes(TEXT_AUDIO).supportedApis(CHAT)
						.supportedCallType(CallType.TOOLS).build());

		// Realtime Models
		MODEL_CONFIG.put("gpt-4o-realtime-preview",
				OpenAiModelMetaData.builder().model("gpt-4o-realtime-preview")
						.tokenizer(OpenAiTokenizer.getTokenizer("gpt-4o-realtime-preview")).contextSize(128_000)
						.maxNewTokens(4_096).inputModes(TEXT_AUDIO).outputModes(TEXT_AUDIO).supportedApis(REALTIME)
						.supportedCallType(CallType.TOOLS).build());

		MODEL_CONFIG.put("gpt-4o-mini-realtime-preview",
				OpenAiModelMetaData.builder().model("gpt-4o-mini-realtime-preview")
						.tokenizer(OpenAiTokenizer.getTokenizer("gpt-4o-mini-realtime-preview")).contextSize(128_000)
						.maxNewTokens(4_096).inputModes(TEXT_AUDIO).outputModes(TEXT_AUDIO).supportedApis(REALTIME)
						.supportedCallType(CallType.TOOLS).build());

		MODEL_CONFIG.put("gpt-realtime-mini",
				OpenAiModelMetaData.builder().model("gpt-realtime-mini")
						.tokenizer(OpenAiTokenizer.getTokenizer("gpt-realtime-mini")).contextSize(32_000)
						.maxNewTokens(4_096).inputModes(TEXT_IMAGE_AUDIO).outputModes(TEXT_AUDIO)
						.supportedApis(REALTIME).supportedCallType(CallType.TOOLS).build());

		// DALL-E
		MODEL_CONFIG.put("gpt-image-1", OpenAiModelMetaData.builder().model("gpt-image-1")
				.tokenizer(OpenAiTokenizer.getTokenizer("gpt-image-1")).contextSize(32_000).inputModes(TEXT_IMAGE)
				.outputModes(IMAGE).supportedApis(IMAGES).supportedCallType(CallType.NONE).build());

		MODEL_CONFIG.put("gpt-image-1-mini",
				OpenAiModelMetaData.builder().model("gpt-image-1-mini")
						.tokenizer(OpenAiTokenizer.getTokenizer("gpt-image-1-mini")).inputModes(TEXT_IMAGE)
						.outputModes(IMAGE).supportedApis(IMAGES).supportedCallType(CallType.NONE).build());

		MODEL_CONFIG.put("dall-e-2",
				OpenAiModelMetaData.builder().model("dall-e-2").tokenizer(OpenAiTokenizer.getTokenizer("dall-e-2"))
						.contextSize(2_000).inputModes(TEXT).outputModes(IMAGE).supportedApis(IMAGES)
						.supportedCallType(CallType.NONE).build());

		MODEL_CONFIG.put("dall-e-3",
				OpenAiModelMetaData.builder().model("dall-e-3").tokenizer(OpenAiTokenizer.getTokenizer("dall-e-3"))
						.contextSize(2_000).inputModes(TEXT).outputModes(IMAGE).supportedApis(IMAGES)
						.supportedCallType(CallType.NONE).build());

		MODEL_CONFIG.put("chatgpt-image-latest", OpenAiModelMetaData.builder().model("chatgpt-image-latest") //
				.tokenizer(OpenAiTokenizer.getTokenizer("chatgpt-image-latest")) //
//				.contextSize(8_192) //
//				.maxNewTokens(8_192) //
				.inputModes(TEXT_IMAGE) //
				.outputModes(TEXT_IMAGE) //
				.supportedApis(IMAGES) //
				.supportedCallType(CallType.NONE) //
				.structuredOutput(false).build());

		MODEL_CONFIG.put("gpt-image-1.5", OpenAiModelMetaData.builder().model("gpt-image-1.5") //
				.tokenizer(OpenAiTokenizer.getTokenizer("gpt-image-1.5")) //
//				.contextSize(8_192) //
//				.maxNewTokens(8_192) //
				.inputModes(TEXT_IMAGE) //
				.outputModes(TEXT_IMAGE) //
				.supportedApis(IMAGES) //
				.supportedCallType(CallType.NONE) //
				.structuredOutput(false).build());

		// Video (SORA)
		MODEL_CONFIG.put("sora-2",
				OpenAiModelMetaData.builder().model("sora-2").tokenizer(OpenAiTokenizer.getTokenizer("sora-2"))
						.inputModes(TEXT_IMAGE).outputModes(List.of(Modality.AUDIO, Modality.VIDEO))
						.supportedApis(List.of(SupportedApi.VIDEOS)).supportedCallType(CallType.NONE).build());

		MODEL_CONFIG.put("sora-2-pro",
				OpenAiModelMetaData.builder().model("sora-2-pro").tokenizer(OpenAiTokenizer.getTokenizer("sora-2-pro"))
						.inputModes(TEXT_IMAGE).outputModes(List.of(Modality.AUDIO, Modality.VIDEO))
						.supportedApis(List.of(SupportedApi.VIDEOS)).supportedCallType(CallType.NONE).build());

		// Audio
		// TODO URGENT fix tokeniser for this model
		MODEL_CONFIG.put("gpt-audio",
				OpenAiModelMetaData.builder().model("gpt-audio").tokenizer(new OpenAiTokenizer("gpt-audio", null))
						.contextSize(128_000).maxNewTokens(16_384).inputModes(TEXT_AUDIO).outputModes(TEXT_AUDIO)
						.supportedApis(CHAT).structuredOutput(false).supportedCallType(CallType.TOOLS).build());

		// TODO URGENT fix tokeniser for this model
		MODEL_CONFIG.put("gpt-audio-mini",
				OpenAiModelMetaData.builder().model("gpt-audio-mini").tokenizer(new OpenAiTokenizer("gpt-audio", null))
						.contextSize(128_000).maxNewTokens(16_384).inputModes(TEXT_AUDIO).outputModes(TEXT_AUDIO)
						.supportedApis(CHAT).structuredOutput(false).supportedCallType(CallType.TOOLS).build());

		// TODO URGENT fix tokeniser for this model
		MODEL_CONFIG.put("gpt-realtime",
				OpenAiModelMetaData.builder().model("gpt-realtime").tokenizer(new OpenAiTokenizer("gpt-realtime", null))
						.contextSize(32_000).maxNewTokens(4_096).inputModes(TEXT_IMAGE_AUDIO).outputModes(TEXT_AUDIO)
						.supportedApis(REALTIME).structuredOutput(false).supportedCallType(CallType.TOOLS).build());

		// TODO URGENT fix tokeniser for this model
		MODEL_CONFIG.put("gpt-realtime-mini",
				OpenAiModelMetaData.builder().model("gpt-realtime-mini")
						.tokenizer(new OpenAiTokenizer("gpt-realtime-mini", null)).contextSize(32_000)
						.maxNewTokens(4_096).inputModes(TEXT_IMAGE_AUDIO).outputModes(TEXT_AUDIO)
						.supportedApis(REALTIME).structuredOutput(false).supportedCallType(CallType.TOOLS).build());

		// TTS
		MODEL_CONFIG.put("gpt-4o-mini-tts",
				OpenAiModelMetaData.builder().model("gpt-4o-mini-tts")
						.tokenizer(OpenAiTokenizer.getTokenizer("gpt-4o-mini-tts")).contextSize(2_000).inputModes(TEXT)
						.outputModes(AUDIO).supportedApis(SPEECH_GENERATION).supportedCallType(CallType.NONE).build());

		MODEL_CONFIG.put("tts-1",
				OpenAiModelMetaData.builder().model("tts-1").tokenizer(OpenAiTokenizer.getTokenizer("tts-1"))
						.contextSize(2_046).inputModes(TEXT).outputModes(AUDIO).supportedApis(SPEECH_GENERATION)
						.supportedCallType(CallType.NONE).build());

		MODEL_CONFIG.put("tts-1-1106",
				OpenAiModelMetaData.builder().model("tts-1-1106").tokenizer(OpenAiTokenizer.getTokenizer("tts-1-1106"))
						.contextSize(2_046).inputModes(TEXT).outputModes(AUDIO).supportedApis(SPEECH_GENERATION)
						.supportedCallType(CallType.NONE).build());

		MODEL_CONFIG.put("tts-1-hd",
				OpenAiModelMetaData.builder().model("tts-1-hd").tokenizer(OpenAiTokenizer.getTokenizer("tts-1-hd"))
						.contextSize(2_046).inputModes(TEXT).outputModes(AUDIO).supportedApis(SPEECH_GENERATION)
						.supportedCallType(CallType.NONE).build());

		MODEL_CONFIG.put("tts-1-hd-1106",
				OpenAiModelMetaData.builder().model("tts-1-hd-1106")
						.tokenizer(OpenAiTokenizer.getTokenizer("tts-1-hd-1106")).contextSize(2_046).inputModes(TEXT)
						.outputModes(AUDIO).supportedApis(SPEECH_GENERATION).supportedCallType(CallType.NONE).build());

		// Transcription
		MODEL_CONFIG.put("gpt-4o-transcribe",
				OpenAiModelMetaData.builder().model("gpt-4o-transcribe")
						.tokenizer(OpenAiTokenizer.getTokenizer("gpt-4o-transcribe")).contextSize(16_000)
						.maxNewTokens(2_000).inputModes(TEXT_AUDIO).outputModes(TEXT)
						.supportedApis(REALTIME_TRANSCRIPTION).supportedCallType(CallType.NONE).build());

		MODEL_CONFIG.put("gpt-4o-mini-transcribe",
				OpenAiModelMetaData.builder().model("gpt-4o-mini-transcribe")
						.tokenizer(OpenAiTokenizer.getTokenizer("gpt-4o-mini-transcribe")).contextSize(16_000)
						.maxNewTokens(2_000).inputModes(TEXT_AUDIO).outputModes(TEXT)
						.supportedApis(List.of(SupportedApi.TRANSCRIPTION)).supportedCallType(CallType.NONE).build());

		MODEL_CONFIG.put("gpt-4o-transcribe-diarize",
				OpenAiModelMetaData.builder().model("gpt-4o-transcribe-diarize")
						.tokenizer(OpenAiTokenizer.getTokenizer("gpt-4o-transcribe-diarize")).contextSize(16_000)
						.maxNewTokens(2_000).inputModes(TEXT_AUDIO).outputModes(TEXT)
						.supportedApis(REALTIME_TRANSCRIPTION).supportedCallType(CallType.NONE).build());

		MODEL_CONFIG.put("whisper-1",
				OpenAiModelMetaData.builder().model("whisper-1").tokenizer(OpenAiTokenizer.getTokenizer("whisper-1"))
						.contextSize(8_192).inputModes(AUDIO).outputModes(TEXT).supportedApis(TRANSLATION_TRANSCRIPTION)
						.supportedCallType(CallType.NONE).build());

		// Tool-specific models

		// TODO URGENT fix this definition
		MODEL_CONFIG.put("gpt-5-search-api", OpenAiModelMetaData.builder().model("gpt-5-search-api")
				.tokenizer(OpenAiTokenizer.getTokenizer("gpt-5-search-api")).contextSize(200_000).maxNewTokens(100_000)
				.inputModes(TEXT).outputModes(TEXT).supportedApis(CHAT).supportedCallType(CallType.NONE).build());

		MODEL_CONFIG.put("gpt-4o-search-preview",
				OpenAiModelMetaData.builder().model("gpt-4o-search-preview")
						.tokenizer(OpenAiTokenizer.getTokenizer("gpt-4o-search-preview")).contextSize(128_000)
						.maxNewTokens(16_384).inputModes(TEXT).outputModes(TEXT).supportedApis(CHAT)
						.supportedCallType(CallType.NONE).build());

		MODEL_CONFIG.put("gpt-4o-mini-search-preview",
				OpenAiModelMetaData.builder().model("gpt-4o-mini-search-preview")
						.tokenizer(OpenAiTokenizer.getTokenizer("gpt-4o-mini-search-preview")).contextSize(128_000)
						.maxNewTokens(16_384).inputModes(TEXT).outputModes(TEXT).supportedApis(CHAT)
						.supportedCallType(CallType.NONE).build());

		MODEL_CONFIG.put("o4-mini-deep-research",
				OpenAiModelMetaData.builder().model("o4-mini-deep-research")
						.tokenizer(OpenAiTokenizer.getTokenizer("o4-mini-deep-research")).contextSize(200_000)
						.maxNewTokens(100_000).inputModes(TEXT_IMAGE).outputModes(TEXT).supportedApis(RESPONSES_BATCH)
						.supportedCallType(CallType.NONE).structuredOutput(false).build());

		MODEL_CONFIG.put("o3-deep-research",
				OpenAiModelMetaData.builder().model("o3-deep-research")
						.tokenizer(OpenAiTokenizer.getTokenizer("o3-deep-research")).contextSize(200_000)
						.maxNewTokens(100_000).inputModes(TEXT_IMAGE).outputModes(TEXT).supportedApis(RESPONSES_BATCH)
						.supportedCallType(CallType.NONE).structuredOutput(false).build());

		MODEL_CONFIG.put("computer-use-preview",
				OpenAiModelMetaData.builder().model("computer-use-preview")
						.tokenizer(OpenAiTokenizer.getTokenizer("computer-use-preview")).contextSize(8_192)
						.maxNewTokens(1_024).inputModes(TEXT_IMAGE).outputModes(TEXT).supportedApis(RESPONSES_BATCH)
						.supportedCallType(CallType.NONE).build());

		// Embeddings
		MODEL_CONFIG.put("text-embedding-3-large", OpenAiModelMetaData.builder().model("text-embedding-3-large")
				.tokenizer(OpenAiTokenizer.getTokenizer("text-embedding-3-large")).contextSize(8_191).inputModes(TEXT)
				.outputModes(EMBEDDINGS).supportedApis(BATCH_EMBEDDINGS).supportedCallType(CallType.NONE).build());

		MODEL_CONFIG.put("text-embedding-3-small", OpenAiModelMetaData.builder().model("text-embedding-3-small")
				.tokenizer(OpenAiTokenizer.getTokenizer("text-embedding-3-small")).contextSize(8_192).inputModes(TEXT)
				.outputModes(EMBEDDINGS).supportedApis(BATCH_EMBEDDINGS).supportedCallType(CallType.NONE).build());

		MODEL_CONFIG.put("text-embedding-ada-002", OpenAiModelMetaData.builder().model("text-embedding-ada-002")
				.tokenizer(OpenAiTokenizer.getTokenizer("text-embedding-ada-002")).contextSize(8_192).inputModes(TEXT)
				.outputModes(EMBEDDINGS).supportedApis(BATCH_EMBEDDINGS).supportedCallType(CallType.NONE).build());

		// Moderation
		MODEL_CONFIG.put("omni-moderation-latest",
				OpenAiModelMetaData.builder().model("omni-moderation-latest")
						.tokenizer(OpenAiTokenizer.getTokenizer("omni-moderation-latest")).contextSize(8_192)
						.inputModes(TEXT_IMAGE).outputModes(TEXT).supportedApis(MODERATION)
						.supportedCallType(CallType.NONE).build());

		MODEL_CONFIG.put("omni-moderation-2024-09-26",
				OpenAiModelMetaData.builder().model("omni-moderation-2024-09-26")
						.tokenizer(OpenAiTokenizer.getTokenizer("omni-moderation-2024-09-26")).contextSize(8_192)
						.inputModes(TEXT_IMAGE).outputModes(TEXT).supportedApis(MODERATION)
						.supportedCallType(CallType.NONE).build());

		// Older GPT Models
		MODEL_CONFIG.put("gpt-3.5-turbo", OpenAiModelMetaData.builder().model("gpt-3.5-turbo")
				.tokenizer(OpenAiTokenizer.getTokenizer("gpt-3.5-turbo")).contextSize(16_385).maxNewTokens(4_096)
				.inputModes(TEXT).outputModes(TEXT).supportedApis(CHAT_RESPONSES_BATCH_FINE_TUNING)
				.supportedCallType(CallType.FUNCTIONS).structuredOutput(false).build());

		MODEL_CONFIG.put("gpt-3.5-turbo-0125", OpenAiModelMetaData.builder().model("gpt-3.5-turbo-0125")
				.tokenizer(OpenAiTokenizer.getTokenizer("gpt-3.5-turbo-0125")).contextSize(16_385).maxNewTokens(4_096)
				.inputModes(TEXT).outputModes(TEXT).supportedApis(CHAT_RESPONSES_BATCH_FINE_TUNING)
				.supportedCallType(CallType.FUNCTIONS).structuredOutput(false).build());

		MODEL_CONFIG.put("gpt-3.5-turbo-1106", OpenAiModelMetaData.builder().model("gpt-3.5-turbo-1106")
				.tokenizer(OpenAiTokenizer.getTokenizer("gpt-3.5-turbo-1106")).contextSize(16_385).maxNewTokens(4_096)
				.inputModes(TEXT).outputModes(TEXT).supportedApis(CHAT_RESPONSES_BATCH_FINE_TUNING)
				.supportedCallType(CallType.FUNCTIONS).structuredOutput(false).build());

		MODEL_CONFIG.put("gpt-3.5-turbo-instruct",
				OpenAiModelMetaData.builder().model("gpt-3.5-turbo-instruct")
						.tokenizer(OpenAiTokenizer.getTokenizer("gpt-3.5-turbo-instruct")).contextSize(4_096)
						.maxNewTokens(4_096).inputModes(TEXT).outputModes(TEXT).supportedApis(COMPLETIONS)
						.supportedCallType(CallType.FUNCTIONS).build());

		MODEL_CONFIG.put("gpt-3.5-turbo-instruct-0914",
				OpenAiModelMetaData.builder().model("gpt-3.5-turbo-instruct-0914")
						.tokenizer(OpenAiTokenizer.getTokenizer("gpt-3.5-turbo-instruct-0914")).contextSize(4_096)
						.inputModes(TEXT).outputModes(TEXT).supportedApis(COMPLETIONS).supportedCallType(CallType.NONE)
						.build());

		MODEL_CONFIG.put("gpt-4-turbo",
				OpenAiModelMetaData.builder().model("gpt-4-turbo")
						.tokenizer(OpenAiTokenizer.getTokenizer("gpt-4-turbo")).contextSize(128_000).maxNewTokens(4_096)
						.inputModes(TEXT_IMAGE).outputModes(TEXT_IMAGE).supportedApis(CHAT_RESPONSES_ASSISTANTS_BATCH)
						.supportedCallType(CallType.TOOLS).structuredOutput(false).build());

		MODEL_CONFIG.put("gpt-4-turbo-preview",
				OpenAiModelMetaData.builder().model("gpt-4-turbo-preview")
						.tokenizer(OpenAiTokenizer.getTokenizer("gpt-4-turbo-preview")).contextSize(128_000)
						.maxNewTokens(4_096).inputModes(TEXT).outputModes(TEXT).supportedApis(CHAT_RESPONSES_ASSISTANTS)
						.supportedCallType(CallType.TOOLS).structuredOutput(false).build());

		MODEL_CONFIG.put("gpt-4-0125-preview",
				OpenAiModelMetaData.builder().model("gpt-4-0125-preview")
						.tokenizer(OpenAiTokenizer.getTokenizer("gpt-4-0125-preview")).contextSize(128_000)
						.maxNewTokens(4_096).inputModes(TEXT).outputModes(TEXT).supportedApis(CHAT_RESPONSES_ASSISTANTS)
						.supportedCallType(CallType.TOOLS).structuredOutput(false).build());

		MODEL_CONFIG.put("gpt-4",
				OpenAiModelMetaData.builder().model("gpt-4").tokenizer(OpenAiTokenizer.getTokenizer("gpt-4"))
						.contextSize(8_192).maxNewTokens(8_192).inputModes(TEXT).outputModes(TEXT)
						.supportedApis(CHAT_RESPONSES_ASSISTANTS_BATCH_FINE_TUNING)
						.supportedCallType(CallType.FUNCTIONS).structuredOutput(false).build());

		MODEL_CONFIG.put("gpt-4-1106-preview",
				OpenAiModelMetaData.builder().model("gpt-4-1106-preview")
						.tokenizer(OpenAiTokenizer.getTokenizer("gpt-4-1106-preview")).contextSize(128_000)
						.maxNewTokens(4_096).inputModes(TEXT).outputModes(TEXT).supportedApis(CHAT_ASSISTANTS)
						.supportedCallType(CallType.TOOLS).structuredOutput(false).build());

		MODEL_CONFIG.put("gpt-4-0613", OpenAiModelMetaData.builder().model("gpt-4-0613") //
				.tokenizer(OpenAiTokenizer.getTokenizer("gpt-4-0613")) //
				.contextSize(8_192) //
				.maxNewTokens(8_192).inputModes(TEXT) //
				.outputModes(TEXT) //
				.supportedApis(CHAT_RESPONSES_ASSISTANTS_BATCH_FINE_TUNING) //
				.supportedCallType(CallType.FUNCTIONS) //
				.structuredOutput(false).build());

		// GPT Base Models
		MODEL_CONFIG.put("babbage-002",
				OpenAiModelMetaData.builder().model("babbage-002")
						.tokenizer(OpenAiTokenizer.getTokenizer("babbage-002")).contextSize(16_384).inputModes(TEXT)
						.outputModes(TEXT).supportedApis(COMPLETIONS).supportedCallType(CallType.NONE).build());

		MODEL_CONFIG.put("davinci-002",
				OpenAiModelMetaData.builder().model("davinci-002")
						.tokenizer(OpenAiTokenizer.getTokenizer("davinci-002")).contextSize(16_385).inputModes(TEXT)
						.outputModes(TEXT).supportedApis(COMPLETIONS).supportedCallType(CallType.NONE).build());
	}

	/**
	 * For testing purposes only.
	 */
	protected static List<String> getDefinedModelIDs() {
		return new ArrayList<>(MODEL_CONFIG.keySet());
	}

	@Getter
	@NonNull
	private final OpenAiEndpoint endpoint;

	protected OpenAiModelService(@NonNull OpenAiEndpoint endpoint) {
		super(MODEL_CONFIG);
		this.endpoint = endpoint;
	}

	@Override
	public List<String> listModels() throws EndpointException {
		try {
			return endpoint.getClient().models().list().data().stream().map(Model::id).collect(Collectors.toList());
		} catch (Exception e) {
			throw OpenAiUtil.toEndpointException(e);
		}
	}

	private final static Pattern P = Pattern.compile("^(.+)-(\\d{4}-\\d{2}-\\d{2})$");

	@Override
	public OpenAiModelMetaData get(@NonNull String model) {
		OpenAiModelMetaData data = (OpenAiModelMetaData) super.get(model);
		if (data != null)
			return data;

		Matcher m = P.matcher(model);
		if (m.matches()) { // model seems to be a snapshot
			// Try to get the value for corresponding model without date
			data = (OpenAiModelMetaData) super.get(m.group(1));
			if (data != null)
				return data.toBuilder().model(model).build();
		}
		return null;
	}

	@Override
	public OpenAiModelMetaData put(@NonNull String model, @NonNull ModelMetaData data) {
		if (data instanceof OpenAiModelMetaData)
			throw new IllegalArgumentException("Data must be a OpenAiModelMetaData");
		return (OpenAiModelMetaData) super.put(model, data);
	}

	@Override
	public OpenAiTokenizer getTokenizer(@NonNull String model) {
		return (OpenAiTokenizer) super.getTokenizer(model);
	}

	@Override
	public OpenAiTokenizer getTokenizer(@NonNull String model, Tokenizer def) {
		return (OpenAiTokenizer) super.getTokenizer(model, def);
	}

	@Override
	public int getMaxNewTokens(@NonNull String model) {
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
			throw new IllegalArgumentException("No metadata found for model " + model);
		return data.getSupportedCallType();
	}

	/**
	 * 
	 * @param model
	 * @param def   Default value if no metadata for the model is found.
	 * @return The type of calls (function or tool) that the model supports.
	 */
	public CallType getSupportedCallType(@NonNull String model, CallType def) {
		OpenAiModelMetaData data = get(model);
		if ((data == null) || data.getSupportedCallType() == null)
			return def;
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
			throw new IllegalArgumentException("No metadata found for model " + model);
		return data.getSupportedApis();
	}

	/**
	 * 
	 * @param model
	 * @param def   Default value if no metadata for the model is found.
	 * @return The API (chat or completions) that the model supports.
	 */
	public List<SupportedApi> getSupportedApis(@NonNull String model, List<SupportedApi> def) {
		OpenAiModelMetaData data = get(model);
		if ((data == null) || (data.getSupportedApis() == null))
			return def;
		return data.getSupportedApis();
	}

	/**
	 * 
	 * @param model
	 * @return True if the model supports structured outputs.
	 */
	public boolean supportsStructuredOutput(@NonNull String model) {
		OpenAiModelMetaData data = get(model);
		if (data == null)
			throw new IllegalArgumentException("No metadata found for model " + model);
		return data.supportsStructuredOutput();
	}

	/**
	 * 
	 * @param model
	 * @param def   Default value if no metadata for the model is found.
	 * @return True if the model supports structured outputs.
	 */
	public boolean supportsStructuredOutput(@NonNull String model, boolean def) {
		OpenAiModelMetaData data = get(model);
		if (data == null)
			return def;
		return data.supportsStructuredOutput();
	}

	/**
	 * 
	 * @param model
	 * @return True if the model supports strict mode in tool calls (unfortunately,
	 *         not all models do).
	 */
	public boolean supportsStrictModeToolCall(@NonNull String model) {
		OpenAiModelMetaData data = get(model);
		if (data == null)
			throw new IllegalArgumentException("No metadata found for model " + model);
		return data.supportsStrictModeToolCall();
	}

	/**
	 * 
	 * @param model
	 * @param def   Default value if no metadata for the model is found.
	 * @return True if the model supports strict mode in tool calls (unfortunately,
	 *         not all models do).
	 */
	public boolean supportsStrictModeToolCall(@NonNull String model, boolean def) {
		OpenAiModelMetaData data = get(model);
		if (data == null)
			return def;
		return data.supportsStrictModeToolCall();
	}
}
