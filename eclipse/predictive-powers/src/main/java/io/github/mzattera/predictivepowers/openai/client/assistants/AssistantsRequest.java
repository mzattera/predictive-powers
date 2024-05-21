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

package io.github.mzattera.predictivepowers.openai.client.assistants;

import java.util.ArrayList;
import java.util.List;

import io.github.mzattera.predictivepowers.openai.client.Metadata;
import io.github.mzattera.predictivepowers.openai.client.chat.OpenAiTool;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Represents the configuration of an assistant, including details such as name,
 * description, instructions, tools, functions, and attached files.
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
public class AssistantsRequest extends Metadata {

	/**
	 * Create a request taking data from given assistant.
	 */
	public static AssistantsRequest getInstance(Assistant agent) {
		return AssistantsRequest.builder() //
				.description(agent.getDescription()) //
				.fileIds(agent.getFileIds()) //
				.instructions(agent.getInstructions()) //
				.metadata(agent.getMetadata()) //
				.model(agent.getModel()) //
				.name(agent.getName()) //
				.tools(agent.getTools()) //
				.build();
	}

	/**
	 * The ID of the model to use. This is a required field. You can use the List
	 * models API to see all of your available models, or see our Model overview for
	 * descriptions of them.
	 */
	@NonNull
	private String model;

	/**
	 * The name of the assistant. This is an optional field. The maximum length is
	 * 256 characters.
	 */
	private String name;

	/**
	 * The description of the assistant. This is an optional field. The maximum
	 * length is 512 characters.
	 */
	private String description;

	/**
	 * The system instructions that the assistant uses. This is an optional field.
	 * The maximum length is 32768 characters.
	 */
	private String instructions;

	/**
	 * A list of tools enabled on the assistant. This is an optional field. Defaults
	 * to an empty list. There can be a maximum of 128 tools per assistant. Tools
	 * can be of types code_interpreter, retrieval, or function.
	 */
	@Builder.Default
	private List<OpenAiTool> tools = new ArrayList<>();

	/**
	 * A list of file IDs attached to this assistant. This is an optional field.
	 * Defaults to an empty list. There can be a maximum of 20 files attached to the
	 * assistant. Files are ordered by their creation date in ascending order.
	 */
	@Builder.Default
	private List<String> fileIds = new ArrayList<>();
}
