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

package io.github.mzattera.predictivepowers.openai.client.completions;

import java.util.List;
import java.util.Map;

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
@Getter
@Setter
@Builder
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
@ToString
public class CompletionsRequest implements Cloneable {

	@NonNull
	String model;
	
	String prompt;
	
	/**
	 * This is used for Insert edits (part of the prompt following the insertion
	 * point).
	 */
	String suffix;

	/**
	 * Many functions in this library will try to calculate this automatically, if
	 * it is null when submitting a request.
	 */
	Integer maxTokens;

	Double temperature;
	Double topP;
	Integer n;

	// TODO: add support for this
	final boolean stream = false;

	Integer logprobs;
	Boolean echo;
	List<String> stop;
	Double presencePenalty;
	Double frequencyPenalty;
	Integer bestOf;
	Map<String, Integer> logitBias;
	String user;
}
