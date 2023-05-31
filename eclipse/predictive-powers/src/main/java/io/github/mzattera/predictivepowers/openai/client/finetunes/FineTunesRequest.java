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

package io.github.mzattera.predictivepowers.openai.client.finetunes;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Request for fine-tunes OpenAI API.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
@ToString
public class FineTunesRequest {

	@NonNull
	String trainingFile;

	String validationFile;
	String model;

	@JsonProperty("n_epochs") // must do for single lower case initial
	Integer nEpochs;
	
	Integer batchSize;
	Double learningRateMultiplier;
	Double promptLossWeight;
	Boolean computeClassificationMetrics;
	Integer classificationNClasses;
	String classificationPositiveClass;
	List<Double> classificationBetas;
	String suffix;
}
