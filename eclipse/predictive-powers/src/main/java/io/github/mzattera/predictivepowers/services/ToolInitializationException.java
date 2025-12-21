/*
 * Copyright 2024-2025 Massimiliano "Maxi" Zattera
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
package io.github.mzattera.predictivepowers.services;

/**
 * This exception is thrown when a {@link Tool} fails to initialize.
 */
public class ToolInitializationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public ToolInitializationException() {
	}

	/**
	 * @param message
	 */
	public ToolInitializationException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public ToolInitializationException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public ToolInitializationException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public ToolInitializationException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
