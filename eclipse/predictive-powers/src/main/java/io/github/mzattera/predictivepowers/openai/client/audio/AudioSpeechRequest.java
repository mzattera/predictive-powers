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

/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client.audio;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Request for the audio/speech OpenAI API.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
@ToString
public class AudioSpeechRequest {

	/**
	 * The voice to use when generating the audio.
	 */
	public enum Voice {
		ALLOY("alloy"), ECHO("echo"), FABLE("fable"), ONYX("onyx"), NOVA("nova"), SHIMMER("shimmer");

		private final String label;

		private Voice(String label) {
			this.label = label;
		}

		@Override
		@JsonValue
		public String toString() {
			return label;
		}
	}

	/**
	 * The format to audio in. Supported formats are
	 */
	public enum ResponseFormat {
		MP3("mp3"), OPUS("opus"), AAC("aac"), FLAC("flac");

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
	String model;

	@NonNull
	String input;

	@NonNull
	Voice voice;

	ResponseFormat responseFormat;
	Double speed;
}
