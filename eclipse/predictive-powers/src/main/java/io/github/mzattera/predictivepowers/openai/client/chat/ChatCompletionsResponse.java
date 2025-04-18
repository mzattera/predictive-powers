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
package io.github.mzattera.predictivepowers.openai.client.chat;

import java.util.ArrayList;
import java.util.List;

import io.github.mzattera.predictivepowers.openai.client.Usage;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest.ServiceTier;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Response from /chat/completions API.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// @RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class ChatCompletionsResponse {

	@Builder.Default
	private List<ChatCompletionsChoice> choices = new ArrayList<>();
	private long created;
	private String id;
	private String model;
	private String object;
	private ServiceTier serviceTier;
	private String systemFingerprint;
	private Usage usage;
}
