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
 * It extends {@link TextCompletion} by providing a {@ChatMessage as response}.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
@Getter
@SuperBuilder
@NoArgsConstructor
//@RequiredArgsConstructor
@AllArgsConstructor
@ToString
public class ChatCompletion {

	// TODO URGENT This should not extend TextCompletion or the builder should not
	// have access to super.text()

	public String getText() {
		return message.getContent();
	}

	@Getter
//	@NonNull
	private ChatMessage message;

	@Getter
//	@NonNull
	private FinishReason finishReason;
}
