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

import com.fasterxml.jackson.annotation.JsonValue;

import io.github.mzattera.predictivepowers.openai.client.Metadata;
import io.github.mzattera.predictivepowers.openai.client.Usage;
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
 * Run step for the Runs OpenAi API.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Getter
@Setter
@ToString
public class RunStep extends Metadata {

	public enum Type {
		MESSAGE_CREATION("message_creation"), TOOL_CALLS("tool_calls");

		private final String label;

		private Type(String label) {
			this.label = label;
		}

		@Override
		@JsonValue
		public String toString() {
			return label;
		}
	}

	public enum Status {
		IN_PROGRESS("in_progress"), CANCELLED("cancelled"), FAILED("failed"), COMPLETED("completed"),
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
	 * The identifier of the run step, which can be referenced in API endpoints.
	 */
	@NonNull
	private String id;

	/**
	 * The object type, which is always thread.run.step.
	 */
	@NonNull
	private String object;

	/**
	 * The Unix timestamp (in seconds) for when the run step was created.
	 */
	private long createdAt;

	/**
	 * The ID of the assistant associated with the run step.
	 */
	@NonNull
	private String assistantId;

	/**
	 * The ID of the thread that was run.
	 */
	@NonNull
	private String threadId;

	/**
	 * The ID of the run that this run step is a part of.
	 */
	@NonNull
	private String runId;

	/**
	 * The type of run step, which can be either message_creation or tool_calls.
	 */
	@NonNull
	private Type type;

	/**
	 * The status of the run step, which can be either in_progress, cancelled,
	 * failed, completed, or expired.
	 */
	@NonNull
	private Status status;

	/**
	 * The details of the run step.
	 */
	private RunStepDetail stepDetails;

	/**
	 * The last error associated with this run step. Will be null if there are no
	 * errors.
	 */
	private Error lastError;

	/**
	 * The Unix timestamp (in seconds) for when the run step expired.
	 */
	private Long expiredAt;

	/**
	 * The Unix timestamp (in seconds) for when the run step was cancelled.
	 */
	private Long cancelledAt;

	/**
	 * The Unix timestamp (in seconds) for when the run step failed.
	 */
	private Long failedAt;

	/**
	 * The Unix timestamp (in seconds) for when the run step completed.
	 */
	private Long completedAt;

	/**
	 * Usage statistics related to the run step. This value will be null while the
	 * run step's status is in_progress.
	 */
	private Usage usage;
}