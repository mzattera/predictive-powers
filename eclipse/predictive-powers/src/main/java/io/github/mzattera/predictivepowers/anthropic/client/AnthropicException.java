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
package io.github.mzattera.predictivepowers.anthropic.client;

import java.io.IOException;

import io.github.mzattera.predictivepowers.anthropic.client.AnthropicError.ErrorDetails;
import lombok.NonNull;
import retrofit2.HttpException;

/**
 * Specialized HttpExeption that is thrown when an error is returned by the
 * ANTHROP\C API.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class AnthropicException extends HttpException {

	private static final long serialVersionUID = 2872745638990379630L;

	@NonNull
	private final AnthropicError error;

	/**
	 * 
	 * @return Details for the error that caused the exception.
	 */
	public ErrorDetails getErrorDetails() {
		return error.getError();
	}

	/**
	 * Builds an exception after an HTTP error.
	 * 
	 * @param rootCause Exception thrown by the client API.
	 * @throws IOException If an error occurred parsing the error reply. This
	 *                     indicates there are no information in the root cause
	 *                     exception to build an OpenAiException.
	 */
	public AnthropicException(@NonNull HttpException rootCause) throws IOException {

		super(rootCause.response());
		initCause(rootCause);
		error = AnthropicClient.getJsonMapper().readValue(rootCause.response().errorBody().string(),
				AnthropicError.class);
	}

	@Override
	public String getMessage() {
		return "HTTP " + ((HttpException) getCause()).code() + ": " + error.getError().getMessage();
	}

	@Override
	public String getLocalizedMessage() {
		return getMessage();
	}
}
