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

package io.github.mzattera.predictivepowers.openai.client.completions;

import java.util.List;
import java.util.Map;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Parameters for a request to /completions API.
 * 
 * @author Massmiliano "Maxi" Zattera.
 *
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class CompletionsRequest {

	@NonNull
	private String model;

	private String prompt;

	/**
	 * This is used for Insert edits (part of the prompt following the insertion
	 * point).
	 */
	private String suffix;

	/**
	 * Many functions in this library will try to calculate this automatically, if
	 * it is null when submitting a request.
	 */
	private Integer maxTokens;

	private Double temperature;
	private Double topP;
	private Integer n;

	// TODO: Add support for streaming input at least in direct API calls, if so
	// make sure services do not stream
	private final boolean stream = false;

	private Integer logprobs;
	private Boolean echo;

	// This causes HTTP 400 error if it is an empty list
	private List<String> stop;

	private Double presencePenalty;
	private Double frequencyPenalty;
	private Integer bestOf;

	// Setting this to an empty map can cause errors when other parameters such as
	// logprobs are set
	private Map<String, Integer> logitBias;

	private String user;

	// TODO URGENT completion_config to be added
}
