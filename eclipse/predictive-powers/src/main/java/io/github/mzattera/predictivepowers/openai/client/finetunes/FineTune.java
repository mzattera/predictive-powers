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

import io.github.mzattera.predictivepowers.openai.client.files.File;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * A fine-tunes, as represented by OpenAI API.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@Getter
@Setter
@ToString
public class FineTune {

	String id;
	String object;
	String model;
	long createdAt;
	List<FineTuneEvent> events;
	String fineTunedModel;
	Hyperparams hyperparams;
	String organizationId;
	List<File> resultFiles;

	/** This goes: pending -> running -> succeeded (if all goes well). */
	String status;
	List<File> validationFiles;
	List<File> trainingFiles;
	long updatedAt;
}
