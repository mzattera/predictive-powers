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

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Fine-tuning hyper parameters, as defined by OpenAI API.
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
public class Hyperparameters {

	private String batchSize;

	public void setBatchSize(int s) {
			batchSize = Integer.toString(s);
	}

	@JsonProperty("batch_size") 
	public void setBatchSize(String s) {
		if (s == null)
			batchSize = "auto";
		else
			batchSize = s;
	}

	@JsonProperty("batch_size") 
	public Integer getBatchSize() {
		if ("auto".equals(batchSize) || (batchSize == null))
			return null;
		else
			return Integer.parseInt(batchSize);
	}
	
	String learningRateMultiplier;

	public void setLearningRateMultiplier(double m) {
			batchSize = Double.toString(m);
	}

	@JsonProperty("learning_rate_multiplier") 
	public void setLearningRateMultiplier(String m) {
		if (m == null)
			learningRateMultiplier = "auto";
		else
			learningRateMultiplier = m;
	}

	@JsonProperty("learning_rate_multiplier") 
	public Double getLearningRateMultiplier() {
		if ("auto".equals(learningRateMultiplier) || (learningRateMultiplier == null))
			return null;
		else
			return Double.parseDouble(learningRateMultiplier);
	}

	@Builder.Default
	private String nEpochs = "auto";

	public void setNEpochs(int e) {
			nEpochs = Integer.toString(e);
	}

	@JsonProperty("n_epochs") // must do for single lower case initial
	public void setNEpochs(String e) {
		if (e == null)
			nEpochs = "auto";
		else
			nEpochs = e;
	}

	@JsonProperty("n_epochs") // must do for single lower case initial
	private Integer getNEpochs() {
		if ("auto".equals(nEpochs) || (nEpochs == null))
			return null;
		else
			return Integer.parseInt(nEpochs);
	}
}
