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

package io.github.mzattera.predictivepowers.openai.client.threads;

import io.github.mzattera.predictivepowers.openai.client.Metadata;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Message file, as needed in messages OpenAI API.
 * 
 * @author GPT-4
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Getter
@Setter
@ToString
public class MessageFile extends Metadata {

	/**
	 * The identifier, which can be referenced in API endpoints.
	 */
	@NonNull
	private String id;

	/**
	 * The object type, which is always thread.message.file.
	 */
	@NonNull
	private String objectType;

	/**
	 * The Unix timestamp (in seconds) for when the message file was created.
	 */
	private long createdAt;

	/**
	 * The ID of the message that the File is attached to.
	 */
	@NonNull
	private String messageId;
}
