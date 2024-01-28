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
@Getter
@Setter
@Builder
@NoArgsConstructor
//@RequiredArgsConstructor
@AllArgsConstructor
@ToString
public class FineTuningJob {

	@Getter
	@Setter
	@Builder
	@NoArgsConstructor
//	@RequiredArgsConstructor
	@AllArgsConstructor
	@ToString
	public static class Error {

		/** A machine-readable error code. */
		String code;

		/** A human-readable error message. */
		String message;

		/**
		 * The parameter that was invalid, usually training_file or validation_file.
		 * This field will be null if the failure was not parameter-specific.
		 */
		String param;
	}

	String id;
	long createdAt;
	Error error;
	String fineTunedModel;
	long finishedAt;
	Hyperparameters hyperparameters;
	String object;
	String model;
	String organizationId;
	List<String> resultFiles;

	/**
	 * This goes: validating_files, queued, running, succeeded, failed, or
	 * cancelled.
	 */
	String status;

	Integer trainedTokens;
	String validationFile;
	String trainingFile;
}
