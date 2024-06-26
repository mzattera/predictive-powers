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
package io.github.mzattera.predictivepowers.openai.client;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.mzattera.predictivepowers.openai.client.OpenAiError.ErrorDetails;
import lombok.Getter;
import lombok.NonNull;
import retrofit2.HttpException;

/**
 * Specialized HttpExeption that is thrown when an error is returned by the
 * OpenAI API.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class OpenAiException extends HttpException {

	private static final long serialVersionUID = 1L;

	@NonNull
	private final OpenAiError error;

	/**
	 * 
	 * @return Details for the error that caused the exception.
	 */
	public ErrorDetails getErrorDetails() {
		return error.getError();
	}

	/** True if the error is because request exceeded context size. */
	@Getter
	private boolean contextLengthExceeded = false;

	/**
	 * If the error is because request exceeded context size, this is the number of
	 * tokens in the prompt. Notice the code tries to populate this from the rror
	 * message, but it might fail and this remains -1.
	 */
	@Getter
	private int promptLength = -1;

	/**
	 * If the error is because request exceeded context size, this is the number of
	 * tokens requested for the completion. Notice the code tries to populate this
	 * from the rror message, but it might fail and this remains -1.
	 */
	@Getter
	private int completionLength = -1;

	/**
	 * If the error is because request exceeded context size, this is the total
	 * number of requested tokens (prompt + completion). Notice the code tries to
	 * populate this from the error message, but it might fail and this remains -1.
	 */
	@Getter
	private int requestLength = -1;

	/**
	 * If the error is because request exceeded context size, this is the maximum
	 * context length for the model (in tokens). Notice the code tries to populate
	 * this from the error message, but it might fail and this remains -1.
	 */
	@Getter
	private int maxContextLength = -1;

	private final static Pattern PATTERN01 = Pattern.compile(
			"This model's maximum context length is ([0-9]+) tokens(\\. However,|, however) you requested ([0-9]+) tokens \\(([0-9]+) (in the messages,|in your prompt;) ([0-9]+) (in|for) the completion\\)\\.");

	private final static Pattern PATTERN02 = Pattern.compile(
			"max_tokens is too large: ([0-9]+). This model supports at most ([0-9]+) completion tokens, whereas you provided 10000.");

	/**
	 * Builds an exception after an HTTP error.
	 * 
	 * @param rootCause Exception thrown by the client API.
	 * @throws IOException If an error occurred parsing the error reply. This
	 *                     indicates there are no information in the root cause
	 *                     exception to build an OpenAiException.
	 */
	public OpenAiException(@NonNull HttpException rootCause) throws IOException {

		super(rootCause.response());
		initCause(rootCause);
		error = OpenAiClient.getJsonMapper().readValue(rootCause.response().errorBody().string(), OpenAiError.class);

		if ((rootCause.code() == 400) && (error.getError().getMessage() != null)) {
			Matcher m = PATTERN01.matcher(error.getError().getMessage());
			if (m.find()) {
				// Too many tokens requested, extract how many
				maxContextLength = Integer.parseInt(m.group(1));
				requestLength = Integer.parseInt(m.group(3));
				promptLength = Integer.parseInt(m.group(4));
				completionLength = Integer.parseInt(m.group(6));
				contextLengthExceeded = true;
			} else {

				// New error message
				m = PATTERN02.matcher(error.getError().getMessage());
				if (m.find()) {
					// Too many tokens requested, extract how many
					requestLength = Integer.parseInt(m.group(1));
					completionLength = Integer.parseInt(m.group(2));
					contextLengthExceeded = true;
				}
			}
		}
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
