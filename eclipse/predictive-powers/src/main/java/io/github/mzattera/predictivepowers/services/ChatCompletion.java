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

package io.github.mzattera.predictivepowers.services;

import io.github.mzattera.predictivepowers.services.TextCompletion.FinishReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * This class encapsulates a response from a {#link ChatService}.
 * 
 * 
 * In addition to providing the returned text, this also contains a reason why
 * the response terminated, which allows the developer to take corrective
 * measures, eventually.
 * 
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
@SuperBuilder
@NoArgsConstructor
//@RequiredArgsConstructor
@AllArgsConstructor
@ToString
public class ChatCompletion {

	public String getText() {
		return message.getContent();
	}

	@Getter
//	TODO URGENT Setting to @NonNull causes issues with @SuperBuilder ChatGPT says I cna have a @Builder.Default method to check after creation, as I have no setters
	private ChatMessage message;

	@Getter
//	@NonNull
	private FinishReason finishReason;
}
