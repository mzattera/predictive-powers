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

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.mzattera.ollama.ApiException;
import io.github.mzattera.ollama.client.api.OllamaApi;
import io.github.mzattera.ollama.client.model.ChatResponse;
import io.github.mzattera.ollama.client.model.GenerateResponse;
import io.github.mzattera.ollama.client.model.Model;
import io.github.mzattera.ollama.client.model.ShowRequest;
import io.github.mzattera.ollama.client.model.ShowResponse;
import io.github.mzattera.predictivepowers.EndpointException;
import io.github.mzattera.predictivepowers.RestException;
import io.github.mzattera.predictivepowers.ollama.OllamaModelService.OllamaModelMetaData;
import io.github.mzattera.predictivepowers.services.ModelService.ModelMetaData.Modality;
import io.github.mzattera.predictivepowers.services.messages.FinishReason;
import lombok.NonNull;

/**
 * Utility methods for Ollama
 * 
 * @author Massimiliano "Maxi" Zattera
 */
public final class OllamaUtil {

	private OllamaUtil() {
	}

	/**
	 * 
	 * @return An EndpointException wrapper for any exception happening when
	 *         invoking HuggingFace API.
	 */
	public static EndpointException toEndpointException(Exception e) {
		if (e instanceof EndpointException)
			return (EndpointException) e;

		if (e instanceof ApiException) {
			ApiException ae = (ApiException) e;
			return RestException.fromHttpException(ae.getCode(), ae, ae.getResponseBody());
		}

		return new EndpointException(e);
	}

	/**
	 * Translates SDK finish reason into library one.
	 */
	public static @NonNull FinishReason fromOllamaFinishReason(ChatResponse response) {
		return fromOllamaFinishReason(response.getDone(), response.getDoneReason());
	}

	public static @NonNull FinishReason fromOllamaFinishReason(GenerateResponse response) {
		return fromOllamaFinishReason(response.getDone(), response.getDoneReason());
	}

	public static @NonNull FinishReason fromOllamaFinishReason(Boolean done, String reason) {

		if ((done != null) && !done)
			return FinishReason.OTHER;

		if (reason == null) {
			if ((done != null) && done)
				return FinishReason.COMPLETED;
			else
				return FinishReason.OTHER;
		}

		switch (reason) {
		case "stop":
			return FinishReason.COMPLETED;
		case "length":
			return FinishReason.TRUNCATED;
		case "content_filter":
			return FinishReason.INAPPROPRIATE;
		case "load":
			return FinishReason.OTHER;
		default:
			throw new IllegalArgumentException("Unrecognized finish reason: " + reason);
		}
	}

	/**
	 * Calculates estimated VRAM usage in GB. * @param showResponse The response
	 * from /api/show (contains architecture info)
	 * 
	 * @param contextSize Context size in tokens
	 * @return Estimated VRAM in Gigabytes
	 */
	public static long calculateVramRequirement(OllamaApi client, String model, int contextSize) {

		ShowResponse showResponse = client.show(new ShowRequest().model(model).verbose(false));
		Model tagsModel = client.tags().getModels().stream().filter(m -> model.equals(m.getName())).findFirst().get();
		Map<String, Object> info = showResponse.getModelInfo();

		// 1. Model Weights (Static)
		// tagsModel.getSize() returns the size on disk in bytes (already quantized)
		double weightSizeGB = tagsModel.getSize() / Math.pow(1024, 3);

		// 2. KV Cache (Dynamic)
		// Formulas vary slightly by architecture, but standard Llama/GQA logic:
		// Cache = 2 * Layers * KV_Heads * Head_Dim * Context * Bytes_Per_FP16

		double layers = getInfoLong(info, ".block_count", 32);
		double kvHeads = getInfoLong(info, ".attention.head_count_kv", 8);
		// If kv_heads isn't explicit, it might be under head_count
		if (kvHeads == 0)
			kvHeads = getInfoLong(info, ".attention.head_count", 32);

		double headDim = getInfoLong(info, ".attention.head_dim", 128);
		if (headDim == 0) {
			// fallback: embedding_length / head_count
			double embeddingLength = getInfoLong(info, ".embedding_length", 4096);
			double heads = getInfoLong(info, ".attention.head_count", 32);
			headDim = (heads > 0) ? (embeddingLength / heads) : 128;
		}

		// KV Cache precision: Ollama defaults to FP16 (2 bytes) unless env var is set
		double bytesPerParam = getKvCacheBytesPerParam();

		double kvCacheBytes = 2.0 * layers * kvHeads * headDim * contextSize * bytesPerParam;
		double kvCacheGB = kvCacheBytes / Math.pow(1024, 3);

		// 3. System Overhead (CUDA kernels, activations, etc.)
		// Typically ~0.5GB to 1GB for the llama.cpp backend
		double overheadGB = 0.7;

		return Math.round(weightSizeGB + kvCacheGB + overheadGB);
	}

	/**
	 * Determines the bytes per parameter for the KV cache based on the Ollama
	 * environment variable 'OLLAMA_KV_CACHE_TYPE'. Default is 2.0 (FP16).
	 */
	private static double getKvCacheBytesPerParam() {
		// Read the environment variable used by Ollama
		String kvType = System.getenv("OLLAMA_KV_CACHE_TYPE");

		if (kvType == null || kvType.isBlank()) {
			return 2.0; // Default FP16
		}

		switch (kvType.toLowerCase().trim()) {
		case "f16":
			return 2.0;
		case "fp8":
		case "f8":
		case "q8_0":
			return 1.0;
		case "q4_0":
			return 0.5;
		default:
			// If an unknown type is set, Ollama usually falls back to F16
			return 2.0;
		}
	}

	/**
	 * Helper to extract nested keys from model_info map. Ollama keys often look
	 * like "llama.attention.head_count"
	 */
	private static double getInfoLong(Map<String, Object> info, String suffix, double defaultValue) {
		return info.entrySet().stream().filter(e -> e.getKey().endsWith(suffix))
				.map(e -> Double.parseDouble(e.getValue().toString())).findFirst().orElse(defaultValue);
	}

	public static int[] SIZES = new int[] { 2, 4, 8, 16, 32, 64, 128 };
	public static Pattern P = Pattern.compile(":([0-9]+)b");
	
	public static void main(String[] values) {
		System.out.println("OLLAMA_KV_CACHE_TYPE can change VRAM occupation and is now set to: "
				+ System.getenv("OLLAMA_KV_CACHE_TYPE"));

		try (OllamaEndpoint ep = new OllamaEndpoint(); OllamaModelService modelSvc = ep.getModelService()) {
			
			System.out.print("Model,Parameters [b],");
			for (int size : SIZES) {
					System.out.print("Ctx: "+ size+"Kt,");
			}
			System.out.println();

			for (String model : modelSvc.listModels()) {
				OllamaModelMetaData data = modelSvc.get(model);
				if (data.getOutputModes().contains(Modality.EMBEDDINGS))
					continue;

				Matcher m = P.matcher(model);
				String parameters = "";
				if (m.find())
					parameters = m.group(1);
				
				System.out.print(model + "," + parameters+",");
				for (int size : SIZES) {
					int sizeTk = size * 1024;
					if (sizeTk > data.getContextSize())
						System.out.print("-,");
					else
						System.out.print(calculateVramRequirement(ep.getClient(), model, sizeTk) + ",");
				}
				
				System.out.println();
			}
		}
	}
}
