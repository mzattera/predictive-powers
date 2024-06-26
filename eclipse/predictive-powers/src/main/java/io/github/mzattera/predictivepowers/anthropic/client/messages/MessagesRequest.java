/*
 * Copyright 2024 Massimiliano "Maxi" Zattera
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

package io.github.mzattera.predictivepowers.anthropic.client.messages;

import java.util.ArrayList;
import java.util.List;

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
 * @author Massimiliano "Maxi" Zattera
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class MessagesRequest {

	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@AllArgsConstructor
	@Builder
	@Getter
	@Setter
	@ToString
	public static class Metadata {

		/**
		 * An external identifier for the user associated with the request. Optional.
		 */
		private String userId;
	}

	/**
	 * The model that will complete your prompt. Required.
	 */
	@NonNull
	private String model;

	/**
	 * Input messages which are alternating user and assistant conversational turns.
	 * Required.
	 */
	@NonNull
	@Builder.Default
	private List<Message> messages = new ArrayList<>();

	/**
	 * System prompt providing context and instructions to the model. Optional.
	 */
	private String system;

	/**
	 * The maximum number of tokens to generate before stopping. Required.
	 */
	private int maxTokens;

	/**
	 * Metadata about the request. Optional.
	 */
	private Metadata metadata;

	/**
	 * Custom text sequences that will cause the model to stop generating.
	 */
	@NonNull
	@Builder.Default
	private List<String> stopSequences = new ArrayList<>();

	/**
	 * Whether to incrementally stream the response using server-sent events.
	 * Optional.
	 */
	// TODO Add support fpr streaming
	@Builder.Default
	private final boolean stream = false;

	/**
	 * Amount of randomness injected into the response. Optional.
	 */
	private Double temperature;

	/**
	 * Definitions of tools that the model may use. 
	 */
	List<AnthropicTool> tools;
	
	/**
	 * Use nucleus sampling. Optional.
	 */
	private Double topP;

	/**
	 * Only sample from the top K options for each subsequent token. Optional.
	 */
	private Integer topK;
}
