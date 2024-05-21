/*
 * Copyright 2023-2024 Massimiliano "Maxi" Zattera
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

package io.github.mzattera.predictivepowers.openai.client.audio;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Request ofr /audio OpneAi API.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class AudioRequest {

	/**
	 * The format of the transcript output, in one of these options: json, text,
	 * srt, verbose_json, or vtt.
	 */
	public enum ResponseFormat {

		/**
		 * Returns JSON data with a single text element that can be parsed into an
		 * {@link AudioResponse}.
		 */
		JSON("json"),

		/**
		 * Returns JSON that can be converted into a fully populated
		 * {@link AudioResponse}.
		 */
		VERBOSE_JSON("verbose_json"),

		/** Returns just the transcribed text. */
		TEXT("text"),

		/** Returns the transcribed text in SRT format. */
		SRT("srt"),

		/** Returns the transcribed text in VTT format. */
		VTT("vtt");

		private final String label;

		private ResponseFormat(String label) {
			this.label = label;
		}

		@Override
		@JsonValue
		public String toString() {
			return label;
		}
	}

	@NonNull
	private String model;

	private String prompt;
	private ResponseFormat responseFormat;
	private Double temperature;
	private String language;
}
