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
 * Categories in a moderation.
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
public class ModerationCategories {

	private boolean hate;

	@JsonProperty("hate/threatening")
	private boolean hateThreatening;

	private boolean harassment;

	@JsonProperty("harassment/threatening")
	private boolean harassmentThreatening;

	@JsonProperty("self-harm")
	private boolean selfHarm;

	@JsonProperty("self-harm/intent")
	private boolean selfHarmIntent;

	@JsonProperty("self-harm/instructions")
	private boolean selfHarmInstructions;

	private boolean sexual;

	@JsonProperty("sexual/minors")
	private boolean sexualMinors;

	private boolean violence;

	@JsonProperty("violence/graphic")
	private boolean violenceGraphic;
}
