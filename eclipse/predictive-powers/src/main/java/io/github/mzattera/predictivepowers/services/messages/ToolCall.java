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

package io.github.mzattera.predictivepowers.services.messages;

import java.util.HashMap;
import java.util.Map;

import io.github.mzattera.predictivepowers.services.Agent;
import io.github.mzattera.predictivepowers.services.Tool;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * {@link Agent}s can invoke tools. This interface represents a single tool
 * invocation, as part of a message.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
@SuperBuilder
@Getter
@Setter
@ToString
public class ToolCall implements MessagePart {

	/**
	 * Unique ID for this tool call.
	 */
	@NonNull
	private String id;

	/**
	 * The tool being called. Notice it is not always guaranteed this to be set
	 * correctly, as some services might not be able to retrieve the proper Tool
	 * instance; this depends on the service generating the call. If this field is
	 * null, developers need to map this call to the proper tool externally from the
	 * service.
	 */
	private Tool tool;

	/**
	 * Arguments to use in the call, as name/value pairs.
	 */
	@NonNull
	@Builder.Default
	private Map<String, Object> arguments = new HashMap<>();

	@Override
	public String getContent() {
		return toString();
	}
}
