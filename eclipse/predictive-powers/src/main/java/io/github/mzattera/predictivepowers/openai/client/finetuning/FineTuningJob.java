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

package io.github.mzattera.predictivepowers.openai.client.finetuning;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A fine-tuining job, as represented by OpenAI API.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class FineTuningJob {

	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	@Getter
	@Setter
	@ToString
	public static class Error {

		/** A machine-readable error code. */
		private String code;

		/** A human-readable error message. */
		private String message;

		/**
		 * The parameter that was invalid, usually training_file or validation_file.
		 * This field will be null if the failure was not parameter-specific.
		 */
		private String param;
	}

	private String id;
	private long createdAt;
	private Error error;
	private String fineTunedModel;
	private long finishedAt;
	private Hyperparameters hyperparameters;
	private String object;
	private String model;
	private String organizationId;

	@Builder.Default
	private List<String> resultFiles = new ArrayList<>();

	/**
	 * This goes: validating_files, queued, running, succeeded, failed, or
	 * cancelled.
	 */
	private String status;

	private Integer trainedTokens;
	private String validationFile;
	private String trainingFile;
}
