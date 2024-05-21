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

import java.util.List;

import io.github.mzattera.predictivepowers.openai.client.Metadata;
import io.github.mzattera.predictivepowers.openai.client.chat.OpenAiTool;
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
 * Request to create a Run in the OpenAI API.
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
public class RunsRequest extends Metadata {

	/**
	 * The ID of the assistant to use to execute this run. Required.
	 */
	@NonNull
	private String assistantId;

	/**
	 * The ID of the Model to be used to execute this run. Optional. If a value is
	 * provided here, it will override the model associated with the assistant. If
	 * not, the model associated with the assistant will be used.
	 */
	private String model;

	/**
	 * Overrides the instructions of the assistant. Optional. This is useful for
	 * modifying the behavior on a per-run basis.
	 */
	private String instructions;

	// TODO would be nice to have this mechanism in our agents

	/**
	 * Appends additional instructions at the end of the instructions for the run.
	 * Optional. This is useful for modifying the behavior on a per-run basis
	 * without overriding other instructions.
	 */
	private String additionalInstructions;

	/**
	 * Override the tools the assistant can use for this run. Optional. This is
	 * useful for modifying the behavior on a per-run basis.
	 */
	// Note this is an OVERRIDE, so it must be null to use default tools avilable to
	// the agent
	private List<OpenAiTool> tools;
}
