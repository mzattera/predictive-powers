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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.github.mzattera.ollama.client.model.Model;
import io.github.mzattera.ollama.client.model.ModelDetails;
import io.github.mzattera.ollama.client.model.ShowRequest;
import io.github.mzattera.ollama.client.model.ShowResponse;
import io.github.mzattera.predictivepowers.services.AbstractModelService;
import io.github.mzattera.predictivepowers.services.ModelService;
import io.github.mzattera.predictivepowers.util.SimpleTokenizer;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * This class provides {@link ModelService}s for Hugging Face.
 * 
 * The class is tread-safe.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class OllamaModelService extends AbstractModelService {

	/** Tokeniser to use when no other tokeniser is found */
	public static final Tokenizer FALLBACK_TOKENIZER = new SimpleTokenizer(2.5);

	@Getter
	@SuperBuilder(toBuilder = true)
	@ToString(callSuper = true)
	public static class OllamaModelMetaData extends ModelMetaData {

		/** Is this a reasoning model? */
		@lombok.Builder.Default
		private final boolean reasoningModel = false;
	}

	@NonNull
	@Getter
	protected final OllamaEndpoint endpoint;

	protected OllamaModelService(OllamaEndpoint endpoint) {
		this.endpoint = endpoint;
	}

	@Override
	public List<String> listModels() {
		try {
			return endpoint.getClient().tags().getModels().stream().map(m -> m.getName()).collect(Collectors.toList());
		} catch (Exception e) {
			throw OllamaUtil.toEndpointException(e);
		}
	}

	@Override
	public OllamaModelMetaData get(@NonNull String model) {
		OllamaModelMetaData result = (OllamaModelMetaData) data.get(model);
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
	private OllamaModelMetaData load(String model) {

		Objects.requireNonNull(model, "modelName");

		Optional<Model> tagsResponse = endpoint.getClient().tags().getModels().stream()
				.filter(m -> m.getName().equals(model)).findFirst();
		Model modelEntry = null;
		if (tagsResponse.isPresent())
			modelEntry = tagsResponse.get();
		else
			throw new IllegalArgumentException("Missing local model: " + model);

		ShowResponse showResponse = endpoint.getClient().show(new ShowRequest().model(model).verbose(false));

		ModelDetails showDetails = showResponse.getDetails();
		ModelDetails tagDetails = (modelEntry != null) ? modelEntry.getDetails() : null;

		Map<String, Object> info = safeMap(showResponse.getModelInfo());
		List<String> capabilities = safeList(showResponse.getCapabilities());

		// Modalities (capabilities first, then name heuristics)
		EnumSet<ModelMetaData.Modality> inputModes = EnumSet.of(ModelMetaData.Modality.TEXT);
		EnumSet<ModelMetaData.Modality> outputModes = EnumSet.of(ModelMetaData.Modality.TEXT);
		boolean isVision = containsIgnoreCase(capabilities, "vision")
				|| looksVisionByName(model, showDetails, tagDetails);
		boolean isEmbedding = containsIgnoreCase(capabilities, "embeddings")
				|| containsIgnoreCase(capabilities, "embedding") || looksEmbeddingByName(model);
		if (isVision) {
			inputModes.add(ModelMetaData.Modality.IMAGE);
		}
		if (isEmbedding) {
			outputModes.clear();
			outputModes.add(ModelMetaData.Modality.EMBEDDINGS);
		}

		// Context size (model_info keys are model-family specific; use suffix scanning)
		Integer contextSize = inferContextSize(info, showResponse.getParameters());

		// maxNewTokens (best-effort: parse parameters; otherwise null)
		Integer maxNewTokens = inferMaxNewTokens(showResponse.getParameters());

		// Reasoning model heuristic (caps + name + family)
		boolean reasoning = inferReasoning(model, capabilities, showDetails, tagDetails, info);

		return OllamaModelMetaData.builder().model(model).tokenizer(null).contextSize(contextSize)
				.maxNewTokens(maxNewTokens).inputModes(new ArrayList<>(inputModes))
				.outputModes(new ArrayList<>(outputModes)).reasoningModel(reasoning).build();
	}

	private static boolean looksEmbeddingByName(String modelName) {
		String n = modelName.toLowerCase(Locale.ROOT);
		return n.contains("embed") || n.contains("embedding");
	}

	private static boolean looksVisionByName(String modelName, ModelDetails showDetails, ModelDetails tagDetails) {
		String n = modelName.toLowerCase(Locale.ROOT);

		if (n.contains("vision") || n.contains("llava") || n.contains("vl"))
			return true;

		// family/families sometimes encode "clip" / "vision"
		if (detailsContains(showDetails, "clip", "vision") || detailsContains(tagDetails, "clip", "vision"))
			return true;

		// some projects suffix with "-vl" or "-vision"
		return n.endsWith("-vl") || n.endsWith("-vision");

	}

	private static boolean detailsContains(ModelDetails d, String... needles) {
		if (d == null)
			return false;
		List<String> hay = new ArrayList<>();
		if (d.getFamily() != null)
			hay.add(d.getFamily());
		if (d.getFamilies() != null)
			hay.addAll(d.getFamilies());
		for (String h : hay) {
			if (h == null)
				continue;
			String hl = h.toLowerCase(Locale.ROOT);
			for (String needle : needles) {
				if (hl.contains(needle.toLowerCase(Locale.ROOT)))
					return true;
			}
		}
		return false;
	}

	private static Integer inferContextSize(Map<String, Object> modelInfo, String parametersText) {

		// Prefer explicit keys (common ones), then suffix scan, then parameters parsing
		Integer v = firstInt(modelInfo, "llama.context_length", "context_length", "llm.context_length",
				"gpt.context_length", "n_ctx");

		if (v != null)
			return v;

		// Suffix scan: many keys are like "gemma3.context_length",
		// "qwen2.context_length", etc.
		v = findIntByKeySuffix(modelInfo, ".context_length", "context_length");
		if (v != null)
			return v;

		// Fallback to parameters ("num_ctx 8192")
		v = parseIntParam(parametersText, "num_ctx");
		if (v != null)
			return v;

		// Some modelfiles might use "context_length"
		return parseIntParam(parametersText, "context_length");
	}

	private static Integer inferMaxNewTokens(String parametersText) {
		// Ollama convention: num_predict
		Integer v = parseIntParam(parametersText, "num_predict");
		if (v != null)
			return v;

		// other common aliases
		v = parseIntParam(parametersText, "max_tokens");
		if (v != null)
			return v;

		v = parseIntParam(parametersText, "max_new_tokens");
		if (v != null)
			return v;

		return null;
	}

	private static boolean inferReasoning(String modelName, List<String> capabilities, ModelDetails showDetails,
			ModelDetails tagDetails, Map<String, Object> modelInfo) {
		String n = modelName.toLowerCase(Locale.ROOT);

		// capability hint if present
		if (containsIgnoreCase(capabilities, "reasoning") || containsIgnoreCase(capabilities, "thinking")
				|| containsIgnoreCase(capabilities, "reason") || containsIgnoreCase(capabilities, "think"))
			return true;

		// name hint: common local naming conventions
		if (n.contains("reason") || n.contains("thinking") || n.matches(".*\\br1\\b.*") || n.contains("-r1"))
			return true;
		if (n.contains("gpt-oss") || n.contains("qwen3") || n.contains("deepseek"))
			return true;

		// family hint
		String fam = firstNonNull((showDetails != null) ? showDetails.getFamily() : null,
				(tagDetails != null) ? tagDetails.getFamily() : null);
		if (fam != null) {
			String fl = fam.toLowerCase(Locale.ROOT);
			if (fl.contains("deepseek") && (n.contains("r1") || n.contains("reason")))
				return true;
		}

		// model_info sometimes includes architecture/notes keys (best-effort suffix
		// scan)
		// If you see "reasoning=true" style keys in your deployments, add them here.
		Object maybe = modelInfo.get("reasoning");
		if (maybe instanceof Boolean)
			return (Boolean) maybe;
		if (maybe instanceof String)
			return "true".equalsIgnoreCase(((String) maybe).trim());

		return false;
	}

	private static Integer firstInt(Map<String, Object> map, String... keys) {
		for (String k : keys) {
			Object v = map.get(k);
			Integer i = coerceInt(v);
			if (i != null)
				return i;
		}
		return null;
	}

	private static Integer findIntByKeySuffix(Map<String, Object> map, String... suffixes) {
		if (map == null || map.isEmpty())
			return null;
		for (Map.Entry<String, Object> e : map.entrySet()) {
			String key = e.getKey();
			if (key == null)
				continue;
			String kl = key.toLowerCase(Locale.ROOT);
			for (String suf : suffixes) {
				if (kl.endsWith(suf.toLowerCase(Locale.ROOT))) {
					Integer v = coerceInt(e.getValue());
					if (v != null)
						return v;
				}
			}
		}
		return null;
	}

	private static Integer coerceInt(Object v) {
		if (v == null)
			return null;
		if (v instanceof Integer)
			return (Integer) v;
		if (v instanceof Long)
			return Math.toIntExact((Long) v);
		if (v instanceof Number)
			return ((Number) v).intValue();
		if (v instanceof String) {
			try {
				return Integer.parseInt(((String) v).trim());
			} catch (Exception ignored) {
			}
		}
		return null;
	}

	private static Integer parseIntParam(String parametersText, String key) {
		if (parametersText == null || key == null)
			return null;
		Pattern p = Pattern.compile("(?m)^\\s*" + Pattern.quote(key) + "\\s+(\\d+)\\s*$");
		Matcher m = p.matcher(parametersText);
		if (m.find()) {
			try {
				return Integer.parseInt(m.group(1));
			} catch (Exception ignored) {
			}
		}
		return null;
	}

	private static boolean containsIgnoreCase(List<String> list, String value) {
		if (list == null || value == null)
			return false;
		for (String s : list) {
			if (s != null && s.equalsIgnoreCase(value))
				return true;
		}
		return false;
	}

	private static <T> List<T> safeList(List<T> l) {
		return (l == null) ? Collections.emptyList() : l;
	}

	private static Map<String, Object> safeMap(Map<String, Object> m) {
		return (m == null) ? Collections.emptyMap() : m;
	}

	@SafeVarargs
	private static <T> T firstNonNull(T... values) {
		for (T v : values)
			if (v != null)
				return v;
		return null;
	}

	public static void main(String[] args) {
		try (OllamaEndpoint ep = new OllamaEndpoint(); OllamaModelService modelSvc = ep.getModelService()) {
			System.out.println(modelSvc.get("qwen3-vl:8b").toString());
			System.out.println(modelSvc.get("ministral-3:3b").toString());
			System.out.println(modelSvc.get("embeddinggemma:300m").toString());
		}
	}
}
