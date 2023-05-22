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

package io.github.mzattera.predictivepowers.openai.client;

import io.github.mzattera.predictivepowers.openai.client.OpenAiError.ErrorDetails;
import lombok.NonNull;

/**
 * Thjis exception is thrown when an error is returned by the OpenAI API in form
 * of an {@link OpenAiError}
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class OpenAiException extends RuntimeException {

	private static final long serialVersionUID = 2872745638990379630L;

	@NonNull
	private final OpenAiError error;

	/**
	 * 
	 * @return Details for the error that caused the exception.
	 */
	public ErrorDetails getErrorDetails() {
		return error.getError();
	}
	
	/**
	 * 
	 * @return HTTP status code for the error, if this was caused by an HTTP error,
	 *         or -1.
	 */
	public int getHttpStatusCode() {
		try {
			return Integer.parseInt(error.getError().getCode());
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	public OpenAiException(@NonNull OpenAiError error, Throwable rootCause) {
		super(error.getError().message, rootCause);
		this.error = error;
	}
}
