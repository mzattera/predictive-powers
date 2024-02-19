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

package io.github.mzattera.predictivepowers.openai.client.moderations;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Scores for categories in a moderation.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class ModerationCategoryScores {

	private double hate;

	@JsonProperty("hate/threatening")
	private double hateThreatening;

	private double harassment;

	@JsonProperty("harassment/threatening")
	private double harassmentThreatening;

	@JsonProperty("self-harm")
	private double selfHarm;

	@JsonProperty("self-harm/intent")
	private double selfHarmIntent;

	@JsonProperty("self-harm/instructions")
	private double selfHarmInstructions;

	private double sexual;

	@JsonProperty("sexual/minors")
	private double sexualMinors;

	private double violence;

	@JsonProperty("violence/graphic")
	private double violenceGraphic;
}
