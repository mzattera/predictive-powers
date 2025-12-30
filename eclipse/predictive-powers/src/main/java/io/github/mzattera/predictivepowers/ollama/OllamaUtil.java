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

import io.github.mzattera.ollama.ApiException;
import io.github.mzattera.ollama.client.model.ChatResponse;
import io.github.mzattera.ollama.client.model.GenerateResponse;
import io.github.mzattera.predictivepowers.EndpointException;
import io.github.mzattera.predictivepowers.RestException;
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
}
