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
 */package io.github.mzattera.predictivepowers.openai.client;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents the error body when an OpenAI request fails
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@Setter
@Getter
@ToString
public class OpenAiError {

	ErrorDetails error;

	@Getter
	@Setter
	@ToString
	public static class ErrorDetails {
		String message;
		String type;
		String param;
		String code;
	}
}
