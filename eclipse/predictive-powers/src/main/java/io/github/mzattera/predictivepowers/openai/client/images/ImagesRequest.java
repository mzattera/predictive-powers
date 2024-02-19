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

package io.github.mzattera.predictivepowers.openai.client.images;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * Request for OpenAI /images/generations API.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class ImagesRequest {

	/**
	 * The quality of the image that will be generated. Defaults to STANDARD. HD
	 * creates images with finer details and greater consistency across the image.
	 * Only supported for dall-e-3.
	 */
	public enum ImageQuality {
		STANDARD("standard"), HD("hd");

		private final @NonNull String label;

		private ImageQuality(@NonNull String label) {
			this.label = label;
		}

		@Override
		@JsonValue
		public String toString() {
			return label;
		}
	}

	/**
	 * The format in which the generated images are returned.
	 */
	public enum ResponseFormat {
		URL("url"), BASE_64("b64_json");

		private final @NonNull String label;

		private ResponseFormat(@NonNull String label) {
			this.label = label;
		}

		@Override
		@JsonValue
		public String toString() {
			return label;
		}
	}

	/**
	 * The size of the generated images. Must be one of _256x256, _512x512, or
	 * _1024x1024 for dall-e-2. Must be one of _1024x1024, _1792x1024, or _1024x1792
	 * for dall-e-3 models. Defaults to 1024x1024.
	 */
	public enum ImageSize {
		_256x256("256x256"), _512x512("512x512"), _1024x1024("1024x1024"), _1792x1024("1792x1024"),
		_1024x1792("1024x1792");

		private final @NonNull String label;

		private ImageSize(@NonNull String label) {
			this.label = label;
		}

		@Override
		@JsonValue
		public String toString() {
			return label;
		}
	}

	/**
	 * The style of the generated images. VIVID causes the model to lean towards
	 * generating hyper-real and dramatic images. NATURAL causes the model to
	 * produce more natural, less hyper-real looking images. Only supported for
	 * dall-e-3.
	 */
	public enum ImageStyle {
		VIVID("vivid"), NATURAL("natural");

		private final @NonNull String label;

		private ImageStyle(@NonNull String label) {
			this.label = label;
		}

		@Override
		@JsonValue
		public String toString() {
			return label;
		}
	}

	// Can be null for edits and variations
	private String prompt;

	private String model;
	private Integer n;
	private ImageQuality quality;
	private ResponseFormat responseFormat;
	private ImageSize size;
	private ImageStyle style;
	private String user;
}
