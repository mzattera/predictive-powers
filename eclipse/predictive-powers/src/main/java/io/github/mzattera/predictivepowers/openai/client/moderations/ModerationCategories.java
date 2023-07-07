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
 */package io.github.mzattera.predictivepowers.openai.client.moderations;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Categories in a moderation.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
@Getter
@Setter
@ToString
public class ModerationCategories {

	boolean hate;

	@JsonProperty("hate/threatening")
	boolean hateThreatening;

	boolean harassment;

	@JsonProperty("harassment/threatening")
	boolean harassmentThreatening;

	@JsonProperty("self-harm")
	boolean selfHarm;

	@JsonProperty("self-harm/intent")
	boolean selfHarmIntent;

	@JsonProperty("self-harm/instructions")
	boolean selfHarmInstructions;

	boolean sexual;

	@JsonProperty("sexual/minors")
	boolean sexualMinors;

	boolean violence;

	@JsonProperty("violence/graphic")
	boolean violenceGraphic;
}
