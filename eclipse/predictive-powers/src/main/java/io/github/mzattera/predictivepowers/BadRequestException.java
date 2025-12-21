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

public class BadRequestException extends EndpointException {

	private static final long serialVersionUID = 1L;

	@Builder(builderMethodName = "badRequestBuilder")
	public BadRequestException(String message, String responseBody, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(400, message, responseBody, cause, enableSuppression, writableStackTrace);
	}

	public BadRequestException(int statusCode, String message) {
		super(400, message);
	}
}
