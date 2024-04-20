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
package io.github.mzattera.predictivepowers.huggingface.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import retrofit2.HttpException;

/**
 * Specialized HttpExeption that is thrown when an error is returned by the
 * Hugging Face API.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class HuggingFaceException extends HttpException {

	private static final long serialVersionUID = 1L;

	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	@Getter
	@Setter
	@ToString
	public static class ErrorDetails {

		@Getter
		private String error;

		@Getter
		@Builder.Default
		private List<String> warnings = new ArrayList<>();
	}

	@Getter
	@NonNull
	private ErrorDetails errorDetails;

	/**
	 * Builds an exception after an HTTP error.
	 * 
	 * @param rootCause Exception thrown by the client API.
	 * @throws IOException If an error occurred parsing the error reply. This
	 *                     indicates there are no information in the root cause
	 *                     exception to build an OpenAiException.
	 */
	public HuggingFaceException(@NonNull HttpException rootCause) throws IOException {

		super(rootCause.response());
		initCause(rootCause);
		errorDetails = HuggingFaceClient.getJsonMapper().readValue(rootCause.response().errorBody().string(),
				ErrorDetails.class);
	}

	@Override
	public String getMessage() {
		return "HTTP " + ((HttpException) getCause()).code()
				+ (errorDetails == null ? "" : ": " + errorDetails.toString());
	}

	@Override
	public String getLocalizedMessage() {
		return getMessage();
	}
}
