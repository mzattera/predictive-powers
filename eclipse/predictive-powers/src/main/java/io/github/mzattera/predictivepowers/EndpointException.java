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

import lombok.NonNull;

/**
 * Base class for all exceptions thrown when calling an {@link Endpoint} method.
 */
public class EndpointException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public EndpointException(@NonNull String message) {
		super(message);
	}

	/**
	 * Use {@link #fromException(Exception, String)} to convert a generic exception
	 * into an EndpoitException. This method is meant to be used when you know cause
	 * is not an EndpointException already.
	 * 
	 * @param cause
	 */
	public EndpointException(@NonNull Exception cause) {
		super(cause);
	}

	public EndpointException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/**
	 * @param message Optional message to override cause message.
	 * 
	 * @return An EndpointException that wraps up another exception. This can be
	 *         used by all services to translate any exception into an
	 *         EndpointException. Notice the input exception is returned if cause is
	 *         an EndpointException already.
	 */
	public static EndpointException fromException(@NonNull Exception cause, String message) {

		if (cause instanceof EndpointException)
			return (EndpointException) cause;

		if (message == null)
			return new EndpointException(cause.getMessage(), cause, false, false);

		return new EndpointException(message, cause, false, false);
	}
}
