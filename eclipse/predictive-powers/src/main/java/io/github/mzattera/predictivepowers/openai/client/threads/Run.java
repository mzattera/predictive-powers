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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonValue;

import io.github.mzattera.predictivepowers.openai.client.Error;
import io.github.mzattera.predictivepowers.openai.client.Metadata;
import io.github.mzattera.predictivepowers.openai.client.Usage;
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
 * A Run in the OpenAI API.
 * 
 * @author GPT-4
 * 
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Getter
@Setter
@ToString
public class Run extends Metadata {

	// Enumeration for Run Status
	public enum Status {
		QUEUED("queued"), //
		IN_PROGRESS("in_progress"), //
		REQUIRES_ACTION("requires_action"), //
		CANCELLING("cancelling"), //
		CANCELLED("cancelled"), //
		FAILED("failed"), //
		COMPLETED("completed"), //
		EXPIRED("expired");

		private final String label;

		private Status(String label) {
			this.label = label;
		}

		@Override
		@JsonValue
		public String toString() {
			return label;
		}
	}

	/**
	 * The identifier, which can be referenced in API endpoints.
	 */
	@NonNull
	private String id;

	/**
	 * The object type, which is always thread.run.
	 */
	@NonNull
	private String object;

	/**
	 * The Unix timestamp (in seconds) for when the run was created.
	 */
	private long createdAt;

	/**
	 * The ID of the thread that was executed on as a part of this run.
	 */
	@NonNull
	private String threadId;

	/**
	 * The ID of the assistant used for execution of this run.
	 */
	@NonNull
	private String assistantId;

	/**
	 * The status of the run, which can be either queued, in_progress,
	 * requires_action, cancelling, cancelled, failed, completed, or expired.
	 */
	@NonNull
	private Status status;

	/**
	 * Details on the action required to continue the run. Will be null if no action
	 * is required.
	 */
	private RequiredAction requiredAction;

	/**
	 * The last error associated with this run. Will be null if there are no errors.
	 */
	private Error lastError;

	/**
	 * The Unix timestamp (in seconds) for when the run will expire.
	 */
	private long expiresAt;

	/**
	 * The Unix timestamp (in seconds) for when the run was started. Nullable.
	 */
	private Long startedAt;

	/**
	 * The Unix timestamp (in seconds) for when the run was cancelled. Nullable.
	 */
	private Long cancelledAt;

	/**
	 * The Unix timestamp (in seconds) for when the run failed. Nullable.
	 */
	private Long failedAt;

	/**
	 * The Unix timestamp (in seconds) for when the run was completed. Nullable.
	 */
	private Long completedAt;

	/**
	 * The model that the assistant used for this run.
	 */
	@NonNull
	private String model;

	/**
	 * The instructions that the assistant used for this run.
	 */
	@NonNull
	private String instructions;

	/**
	 * The list of tools that the assistant used for this run.
	 */
	@NonNull
	@Builder.Default
	private List<OpenAiTool> tools = new ArrayList<>();

	/**
	 * The list of File IDs the assistant used for this run.
	 */
	@NonNull
	@Builder.Default
	private List<String> fileIds = new ArrayList<>();

	/**
	 * Usage statistics related to the run. This value will be null if the run is
	 * not in a terminal state.
	 */
	private Usage usage;
}
