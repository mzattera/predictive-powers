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

package io.github.mzattera.predictivepowers.openai.services;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Used in audio and chat APIs to specify output format for audio responses.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class AudioFormat {

	/**
	 * Specifies the output audio format. Must be one of wav, mp3, flac, opus, or
	 * pcm16.
	 */
	@NonNull
	private Format format;

	/**
	 * The voice the model uses to respond. Supported voices are alloy, ash, ballad,
	 * coral, echo, sage, and shimmer.
	 */
	@NonNull
	private Voice voice;

	public static enum Format {
		AAC("aac"), WAV("wav"), MP3("mp3"), FLAC("flac"), OPUS("opus"), PCM("pcm"), PCM16("pcm16");

		private final String label;

		private Format(String label) {
			this.label = label;
		}

		@Override
		@JsonValue
		public String toString() {
			return label;
		}
	}

	public static enum Voice {
		ALLOY("alloy"), ASH("ash"), BALLAD("ballad"), CORAL("coral"), ECHO("echo"), FABLE("fable"), ONYX("onyx"),
		NOVA("nova"), SAGE("sage"), SHIMMER("shimmer"), VERSE("verse");

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
}