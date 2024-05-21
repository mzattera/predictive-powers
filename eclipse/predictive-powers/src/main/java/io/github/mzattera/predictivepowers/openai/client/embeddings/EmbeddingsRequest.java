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
 */package io.github.mzattera.predictivepowers.openai.client.embeddings;

import java.util.ArrayList;
import java.util.List;

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
 * Parameters for a request to /embeddings API.
 * 
 * @author Massmiliano "Maxi" Zattera.
 *
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class EmbeddingsRequest {

	public enum EncodingFormat {

		FLOAT("float"), BASE_64("base64");

		private final @NonNull String label;

		private EncodingFormat(@NonNull String label) {
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

	/**
	 * Input text to get embeddings for, encoded as a string.
	 * 
	 * To get embeddings for multiple inputs in a single request, pass multiple
	 * strings.
	 */
	// TODO in reality this supports multiple formats, but I do not think they are really useful
	@NonNull
	@Builder.Default
	private List<String> input = new ArrayList<>();

	private EncodingFormat encodingFormat;
	
	private Integer dimensions;
	
	private String user;
}
