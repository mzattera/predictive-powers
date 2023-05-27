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
 */package io.github.mzattera.predictivepowers.openai.client.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.mzattera.predictivepowers.services.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Parameters for a request to /chat/completions API.
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
public class ChatCompletionsRequest implements Cloneable {

	@NonNull
	String model;

	@NonNull
	@Builder.Default
	List<ChatMessage> messages = new ArrayList<>();

	Double temperature;
	Double topP;
	Integer n;

	// TODO: Add support for streaming input at least in direct API calls, if so make sure services do not stream
	final boolean stream = false;

	List<String> stop;

	/**
	 * Higher-level functions in the library will try to calculate this automatically if it is
	 * null when submitting a request.
	 */
	Integer maxTokens;

	Double presencePenalty;
	Double frequencyPenalty;
	Map<String, Integer> logitBias;
	String user;
}
