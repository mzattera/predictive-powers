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

/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client.chat;

import io.github.mzattera.predictivepowers.services.messages.TextPart;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * This is a special text message that contains a refusal message from a OpenAI
 * API.
 * 
 * OpenAI API responses return refusal as a separate string field, but when the
 * message is serialized back in chat history for API call, it is provided as a
 * message part.
 * 
 * This special class is used to store refusal as a message part; we assume
 * either a refusal or a content text is provided in the reply.
 */
@Getter
@Setter
@ToString
public class RefusalPart extends TextPart {

	public RefusalPart(String content) {
		super(content);
	}
}
