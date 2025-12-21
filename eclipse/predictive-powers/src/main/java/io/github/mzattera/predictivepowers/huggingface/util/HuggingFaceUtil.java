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
package io.github.mzattera.predictivepowers.huggingface.util;

import io.github.mzattera.hfinferenceapi.ApiException;
import io.github.mzattera.predictivepowers.EndpointException;
import io.github.mzattera.predictivepowers.services.messages.FinishReason;
import lombok.NonNull;

/**
 * Utility methods for Hugging Face
 * 
 * @author Massimiliano "Maxi" Zattera
 */
public final class HuggingFaceUtil {

	private HuggingFaceUtil() {
	}

	/**
	 * 
	 * @param model A model in format author/model:provider (e.g.
	 *              Qwen/Qwen3-Embedding-8B:nebius).
	 * 
	 * @return a String[2] array with the model and provider provided separately
	 *         (eventually empty when missing).
	 */
	public static String[] parseModel(@NonNull String model) {
		String[] parts = new String[2];

		int colonIndex = model.lastIndexOf(':');
		if (colonIndex != -1) {
			parts[0] = model.substring(0, colonIndex).trim();
			parts[1] = model.substring(colonIndex + 1).trim();
		} else {
			parts[0] = model;
			parts[1] = "";
		}

		return parts;
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
			return EndpointException.fromHttpException(ae.getCode(), ae, ae.getResponseBody());
		}

		return new EndpointException(e);
	}

	/**
	 * Translates SDK finish reason into library one.
	 */
	public static @NonNull FinishReason fromHuggingFaceApi(
			String finishReason) {
		switch (finishReason) {
		case "stop":
		case "tool_calls":
			return FinishReason.COMPLETED;
		case "length":
			return FinishReason.TRUNCATED;
		case "content_filter":
			return FinishReason.INAPPROPRIATE;
		default:
			throw new IllegalArgumentException("Unrecognized finish reason: " + finishReason);
		}
	}
}
