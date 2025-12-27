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

/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.util;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.openai.core.JsonValue;
import com.openai.errors.BadRequestException;
import com.openai.errors.OpenAIException;
import com.openai.errors.OpenAIServiceException;
import com.openai.models.FunctionParameters;
import com.openai.models.images.Image;

import io.github.mzattera.predictivepowers.EndpointException;
import io.github.mzattera.predictivepowers.RestException;
import io.github.mzattera.predictivepowers.services.Tool.ToolParameter;
import io.github.mzattera.predictivepowers.services.messages.Base64FilePart;
import io.github.mzattera.predictivepowers.services.messages.FilePart;
import io.github.mzattera.predictivepowers.services.messages.FinishReason;
import io.github.mzattera.predictivepowers.services.messages.JsonSchema;
import lombok.Getter;
import lombok.NonNull;

/**
 * Utility methods to support working with OpenAI Java SDK.
 * 
 * @author Massimiliano "Maxi" Zattera
 */
public final class OpenAiUtil {

	private OpenAiUtil() {
	}

	/**
	 * Additional data we can obtain by inspecting a {@link OpenAIException}.
	 */
	@Getter
	public static class OpenAiExceptionData {

		private OpenAiExceptionData() {
		}

		@NonNull
		OpenAIException exception;

		/**
		 * Maximum new tokens for this model, accordingly to exception message.
		 */
		private int maxNewTokens = -1;

		/**
		 * Context size for this model, accordingly to exception message.
		 */
		private int contextSize = -1;

		/**
		 * Length of the prompt, accordingly to exception message.
		 */
		private int promptLength = -1;

		/**
		 * Maximum new tokens set in the request, accordingly to exception message.
		 */
		private int requestMaxNewTokens = -1;
	}

	private final static Pattern PATTERN01 = Pattern.compile(
			"This model's maximum context length is ([0-9]+) tokens(\\. However,|, however) you requested ([0-9]+) tokens \\(([0-9]+) (in the messages,|in your prompt;) ([0-9]+) (in|for) the completion\\)\\.");
	private static Pattern PATTERN02 = Pattern
			.compile("This model supports at most ([0-9]+) completion tokens, whereas you provided ([0-9]+)");
	private static Pattern PATTERN03 = Pattern.compile( // Notice this must be evaluated after PATTERN01
			"This model's maximum context length is ([0-9]+)");
	private static Pattern PATTERN04 = Pattern.compile("Input tokens exceed the configured limit of ([0-9]+) tokens");

	/**
	 * 
	 * @param ex
	 * @return A {@link OpenAiExceptionData} instance containing additional error
	 *         data obtained by parsing input exception.
	 */
	public static OpenAiExceptionData getExceptionData(@NonNull OpenAIException ex) {

		// TODO Explore because exceptions how have headers with additional data

		OpenAiExceptionData d = new OpenAiExceptionData();
		d.exception = ex;

		// Pattern matching for context length exceeded
		if ((ex instanceof BadRequestException) && (ex.getMessage() != null)) {
			Matcher m = PATTERN01.matcher(ex.getMessage());
			if (m.find()) {
				d.contextSize = Integer.parseInt(m.group(1));
				d.promptLength = Integer.parseInt(m.group(4));
				d.requestMaxNewTokens = Integer.parseInt(m.group(6));
				return d;
			}

			m = PATTERN02.matcher(ex.getMessage());
			if (m.find()) {
				d.maxNewTokens = Integer.parseInt(m.group(1));
				d.requestMaxNewTokens = Integer.parseInt(m.group(2));
				return d;
			}

			m = PATTERN03.matcher(ex.getMessage());
			if (m.find()) {
				d.contextSize = Integer.parseInt(m.group(1));
				return d;
			}

			m = PATTERN04.matcher(ex.getMessage());
			if (m.find()) {
				d.contextSize = Integer.parseInt(m.group(1));
				return d;
			}
		}

		return d;
	}

	/**
	 * 
	 * @return An EndpointException wrapper for any exception happening when
	 *         invoking OpenAI API.
	 */
	public static EndpointException toEndpointException(Exception e) {

		if (e instanceof EndpointException)
			return (EndpointException) e;

		if (e instanceof OpenAIServiceException) {
			
			OpenAIServiceException oaie = (OpenAIServiceException) e;
			if ((oaie instanceof BadRequestException) && oaie.getMessage().contains("Your request was rejected as a result of our safety system")) {
				// UnprocessableEntityException
				return RestException.fromHttpException(422, oaie, oaie.body().toString());
			}
			return RestException.fromHttpException(oaie.statusCode(), oaie, oaie.body().toString());
		}

		return EndpointException.fromException(e, null);
	}

	/**
	 * Translates SDK finish reason into library one.
	 */
	public static FinishReason fromOpenAiApi(
			com.openai.models.chat.completions.ChatCompletion.Choice.FinishReason finishReason) {
		switch (finishReason.value()) {
		case STOP:
		case TOOL_CALLS:
		case FUNCTION_CALL:
			return FinishReason.COMPLETED;
		case LENGTH:
			return FinishReason.TRUNCATED;
		case CONTENT_FILTER:
			return FinishReason.INAPPROPRIATE;
		default:
			throw new IllegalArgumentException("Unrecognized finish reason: " + finishReason);
		}
	}

	/**
	 * Translates SDK finish reason into library one.
	 */
	public static @NonNull FinishReason fromOpenAiApi(
			com.openai.models.completions.CompletionChoice.FinishReason finishReason) {
		switch (finishReason.value()) {
		case STOP:
			return FinishReason.COMPLETED;
		case LENGTH:
			return FinishReason.TRUNCATED;
		case CONTENT_FILTER:
			return FinishReason.INAPPROPRIATE;
		default:
			throw new IllegalArgumentException("Unrecognized finish reason: " + finishReason);
		}
	}

	/**
	 * Transforms returned images into a List<Base64FilePart>.
	 */
	public static List<FilePart> readImages(List<Image> images) throws IOException {

		List<FilePart> result = new ArrayList<>();
		if (images == null)
			return result;
		for (Image image : images) {
			if (image.b64Json().isPresent())
				// Notice this will scan image for its type, should be OK as the image is in
				// memory already
				result.add(new Base64FilePart(image.b64Json().get(), "img_" + UUID.randomUUID()));
			else if (image.url().isPresent())
				// Suffice to say the file is an image, to avoid scanning it for type
				result.add(new FilePart(URI.create(image.url().get()).toURL(), "image"));
			else
				// New unsupported feature probably
				throw new IOException("Cannot read image from URL or Base64 representation");
		}
		return result;
	}

	/**
	 * Because of the idiotic way SDK defined function parameters instead of just
	 * getting a schema, we must translate a list of {@link ToolParameter}s into
	 * {@link FunctionParameters}.
	 * 
	 * @author Luna
	 * @author Massimiliano "Maxi" Zattera
	 * 
	 * @param params Definition of parameters for the function.
	 * @param string If true uses strict mode (see below).
	 * 
	 * @see <a href=
	 *      "https://platform.openai.com/docs/guides/function-calling?api-mode=chat#additional-configurations">strict
	 *      mode</a>
	 */
	public static FunctionParameters toFunctionParameters(List<? extends ToolParameter> params, boolean strict) {

		Map<String, Object> schema = new JsonSchema(params).asMap(strict);

		FunctionParameters.Builder builder = FunctionParameters.builder();
		schema.entrySet().forEach(e -> builder.putAdditionalProperty(e.getKey(), JsonValue.from(e.getValue())));
		return builder.build();
	}
}
