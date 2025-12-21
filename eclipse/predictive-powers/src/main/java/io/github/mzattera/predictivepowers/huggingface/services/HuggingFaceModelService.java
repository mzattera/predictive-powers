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

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import ai.djl.repository.Artifact;
import ai.djl.repository.MRL;
import ai.djl.repository.Repository;
import io.github.mzattera.predictivepowers.huggingface.util.HuggingFaceUtil;
import io.github.mzattera.predictivepowers.services.AbstractModelService;
import io.github.mzattera.predictivepowers.services.ModelService;
import io.github.mzattera.predictivepowers.services.messages.JsonSchema;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * This class provides {@link ModelService}s for Hugging Face.
 * 
 * The class is tread-safe and uses a single data repository for all of its
 * instances.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class HuggingFaceModelService extends AbstractModelService {

	@Getter
	@Setter
	@Builder
//	@NoArgsConstructor
	@RequiredArgsConstructor
//	@AllArgsConstructor
	@ToString
	public static class HuggingFaceTokenizer implements Tokenizer {

		@NonNull
		private final ai.djl.huggingface.tokenizers.HuggingFaceTokenizer tokenizer;

		@Override
		public int count(@NonNull String text) {
			return tokenizer.encode(text).getTokens().length;
		}
	}

	private final static Logger LOG = LoggerFactory.getLogger(HuggingFaceModelService.class);

	/**
	 * Single instance of the data Map, shared by all instances of this model
	 * service class.
	 */
	private final static Map<String, ModelMetaData> data = new ConcurrentHashMap<>();

	@NonNull
	@Getter
	protected final HuggingFaceEndpoint endpoint;

	public HuggingFaceModelService(HuggingFaceEndpoint endpoint) {
		super(data);
		this.endpoint = endpoint;
	}

	@Override
	public ModelMetaData get(@NonNull String model) {
		ModelMetaData result = data.get(model);
		if (result == null) {
			result = load(model);
			put(model, result);
			return result;
		}
		return result;
	}

	/**
	 * Tries to build model meta data by using DLJ library to get tokeniser and
	 * configuration files. It then tries some heuristic to parse the files.
	 * 
	 * @param model
	 * @return Model meta data filled as much as possible
	 */
	public static ModelMetaData load(String model) {
		ModelMetaData.Builder builder = ModelMetaData.builder().model(model);

		// Remove provider, if any
		model = HuggingFaceUtil.parseModel(model)[0];

		// 1. Tokenizer (via DJL)
		try {
			builder.tokenizer(
					new HuggingFaceTokenizer(ai.djl.huggingface.tokenizers.HuggingFaceTokenizer.newInstance(model)));
		} catch (Exception e) {
			LOG.warn("Cannot retrieve Hugging Face Tokenizer for model " + model, e);
		}

		// 2. Fetch Config JSON (Try Local DJL Cache -> Fallback to REST API)
		JsonNode config = null;
		try {
			config = fetchJson(model, "config.json");
			if (config != null) {
				// Context Size Probe
				builder.contextSize(findFirstInt(config, "max_position_embeddings", "n_positions", "n_ctx",
						"max_sequence_length", "seq_length"));
			}
		} catch (Exception e) {
			LOG.info("Error parsing data for Hugging Face model: " + model, e);
		}
		try {
			if (config != null) {
				// Detect Modalities
				detectModalities(config, builder);
			}
		} catch (Exception e) {
			LOG.info("Error parsing data for Hugging Face model: " + model, e);
		}

		// 3. Fetch Generation Config (Try Local -> Fallback to REST API)
		try {
			JsonNode genConfig = fetchJson(model, "generation_config.json");
			if (genConfig != null) {
				builder.maxNewTokens(findFirstInt(genConfig, "max_new_tokens", "max_length"));
			} else if (config != null) {
				// Fallback: Some models keep generation limits in the main config
				builder.maxNewTokens(findFirstInt(config, "max_new_tokens", "max_length"));
			}
		} catch (Exception e) {
			LOG.info("Error parsing data for Hugging Face model: " + model, e);
		}

		return builder.build();
	}

	private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

	private static JsonNode fetchJson(String modelId, String fileName) {
		// --- STEP A: DJL Local Cache (DJL 0.36.0) ---
		try {
			// Use a generic repository instance
			Repository repo = Repository.newInstance("model", "https://mlrepo.djl.ai/model/nlp/");

			// MRL.model(repo, application, groupId, artifactId, version, artifactName)
			// For Hugging Face models, we treat the modelId as the artifactId
			MRL mrl = MRL.model(repo, ai.djl.Application.NLP.ANY, "ai.djl.huggingface.pytorch", // Common group for HF
																								// models in DJL
					modelId, "0.0.1", // Default version
					modelId // Artifact name
			);

			// resolve(mrl, filter) - Using an empty map as filter
			Artifact artifact = repo.resolve(mrl, java.util.Collections.emptyMap());

			if (artifact != null) {
				repo.prepare(artifact); // Downloads/extracts to cache
				Path modelDir = repo.getResourceDirectory(artifact);
				Path filePath = modelDir.resolve(fileName);

				if (Files.exists(filePath)) {
					try (InputStream is = Files.newInputStream(filePath)) {
						return JsonSchema.JSON_MAPPER.readTree(is);
					}
				}
			}
		} catch (Exception e) {
			LOG.info("Error retrieving Hugging Face model data from DJL cache", e);
		}

		// --- STEP B: Hugging Face Raw REST API Fallback ---
		try {
			String url = String.format("https://huggingface.co/%s/raw/main/%s", modelId, fileName);
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("User-Agent", "Java-DJL-Library")
					.GET().build();

			HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() == 200) {
				return JsonSchema.JSON_MAPPER.readTree(response.body());
			}
		} catch (Exception e) {
			LOG.info("Error retrieving Hugging Face model data from Hugging Face endpoint", e);
		}

		return null;
	}

	private static void detectModalities(JsonNode config, ModelMetaData.Builder builder) {
		String modelType = config.path("model_type").asText("").toLowerCase();
		String arch = config.path("architectures").path(0).asText("").toLowerCase();

		// Structural Check
		if (config.has("vision_config"))
			builder.addInputMode(ModelMetaData.Modality.IMAGE);
		if (config.has("audio_config"))
			builder.addInputMode(ModelMetaData.Modality.AUDIO);

		// Logical Check
		if (arch.contains("forcausallm") || arch.contains("seq2seq") || config.has("vocab_size")) {
			builder.addInputMode(ModelMetaData.Modality.TEXT);
			builder.addOutputMode(ModelMetaData.Modality.TEXT);
		}

		// Specialized Mapping
		if (modelType.equals("whisper"))
			builder.addInputMode(ModelMetaData.Modality.AUDIO);
		if (modelType.contains("clip") || modelType.contains("llava"))
			builder.addInputMode(ModelMetaData.Modality.IMAGE);
	}

	private static Integer findFirstInt(JsonNode node, String... keys) {
		for (String key : keys) {
			if (node.has(key) && node.get(key).isNumber())
				return node.get(key).asInt();
		}
		return null;
	}

	/**
	 * Unsupported, as there are too many.
	 */
	@Override
	public List<String> listModels() {
		throw new UnsupportedOperationException();

		// Theoretically we could implement this with
		// https://huggingface.co/docs/hub/api#get-apimodels
		// but this returns only 1000 models at once and the pagination is absurd (see
		// example header below):
		//
		// link=[<https://huggingface.co/api/models?sort=trendingScore&cursor=eyIkb3IiOlt7InRyZW5kaW5nU2NvcmUiOjIsIl9pZCI6eyIkZ3QiOiI2NjM0YTE3MWM1OGY0NTU3NzEwOTkwZjIifX0seyJ0cmVuZGluZ1Njb3JlIjp7IiRsdCI6Mn19LHsidHJlbmRpbmdTY29yZSI6bnVsbH1dfQ%3D%3D>;
		// rel="next"]
		//
		// In addition, it makes little sense to know all models, since users must know
		// the provider they want to use and the model they like.
	}

	public static void main(String[] args) throws JsonProcessingException {
		ModelMetaData meta = load("Qwen/Qwen2.5-3B-Instruct");
		System.out.print(JsonSchema.JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(meta));
	}
}
