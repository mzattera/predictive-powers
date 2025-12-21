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
package io.github.mzattera.predictivepowers;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/**
 * Base class for all exceptions thrown when calling an {@link Endpoint} method.
 */
@Getter
public class EndpointException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Status code returned by the Endpoint call, or -1 if not set.
	 */
	private final int statusCode;

	/**
	 * HTTP response body, if available.
	 */
	private final String responseBody;

	@Builder
	public EndpointException(int statusCode, String message, String responseBody, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		this.statusCode = statusCode;
		this.responseBody = responseBody;
	}

	public EndpointException(int statusCode, String message) {
		super(message);
		this.statusCode = statusCode;
		this.responseBody = null;
	}

	public EndpointException(int statusCode, String message, String responseBody) {
		super(message);
		this.statusCode = statusCode;
		this.responseBody = responseBody;
	}

	public EndpointException(Exception e) {
		this(-1, e.getMessage(), null, e, false, false);
	}

	/**
	 * 
	 * @param statusCode   HTTP error code
	 * @param cause        The original exception
	 * @param responseBody The response body, if any
	 * @return An EndpointException that wraps up another exception caused by some
	 *         HTTP request error. This can be used by all services to translate
	 *         specific API exception into EndpointException.
	 */
	public static EndpointException fromHttpException(int statusCode, @NonNull Exception cause, String responseBody) {

		if (cause instanceof EndpointException)
			return (EndpointException) cause;

		switch (statusCode) {
		case 400:
			return io.github.mzattera.predictivepowers.BadRequestException.badRequestBuilder() //
					.cause(cause) //
					.message(cause.getMessage()) //
					.responseBody(responseBody).build();
		case 401:
			return io.github.mzattera.predictivepowers.UnauthorizedException.unauthorizedExceptionBuilder() //
					.cause(cause) //
					.message(cause.getMessage()) //
					.responseBody(responseBody).build();
		case 403:
			return io.github.mzattera.predictivepowers.PermissionDeniedException.permissionDeniedExceptionBuilder() //
					.cause(cause) //
					.message(cause.getMessage()) //
					.responseBody(responseBody).build();
		case 404:
			return io.github.mzattera.predictivepowers.NotFoundException.notFoundExceptionBuilder() //
					.cause(cause) //
					.message(cause.getMessage()) //
					.responseBody(responseBody).build();
		case 422:
			return io.github.mzattera.predictivepowers.UnprocessableEntityException
					.unprocessableEntityExceptionBuilder() //
					.cause(cause) //
					.message(cause.getMessage()) //
					.responseBody(responseBody).build();
		case 429:
			return io.github.mzattera.predictivepowers.RateLimitException.rateLimitExceptionBuilder() //
					.cause(cause) //
					.message(cause.getMessage()) //
					.responseBody(responseBody).build();
		case 500:
			return io.github.mzattera.predictivepowers.InternalServerException.internalServerExceptionBuilder() //
					.cause(cause) //
					.message(cause.getMessage()) //
					.responseBody(responseBody).build();
		default:
			return io.github.mzattera.predictivepowers.EndpointException.builder() //
					.statusCode(statusCode) //
					.cause(cause) //
					.message(cause.getMessage()) //
					.responseBody(responseBody).build();
		}
	}
}
