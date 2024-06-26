/*
 * Copyright 2024 Massimiliano "Maxi" Zattera
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

package io.github.mzattera.predictivepowers.openai.client.threads;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;

import io.github.mzattera.predictivepowers.openai.services.OpenAiFilePart;
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
 * Content for an assistant Message.
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class Content {

	public static enum Type {
		IMAGE_FILE("image_file"), //
		TEXT("text");

		private final String label;

		private Type(String label) {
			this.label = label;
		}

		@Override
		@JsonValue
		public String toString() {
			return label;
		}
	}

	private Type type;

	// For image_file type, this will contain a single key "file_id"
	@Builder.Default
	private Map<String, String> imageFile = new HashMap<>();

	// In case message us text type
	private Text text;

	public Content(String text) {
		this(Type.TEXT, null, new Text(text));
	}

	public Content(OpenAiFilePart img) {
		if (img.getUrl() == null)
			throw new IllegalArgumentException("Only image URLs are supported.");
		type = Type.IMAGE_FILE;
		imageFile = new HashMap<>();
		imageFile.put("file_id", img.getFileId());
	}

	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@RequiredArgsConstructor
	@AllArgsConstructor
	@Builder
	@Getter
	@Setter
	@ToString
	public static class Text {

		@NonNull
		private String value;

		@Builder.Default
		private List<Annotation> annotations = new ArrayList<>();

		@NoArgsConstructor
		@AllArgsConstructor
		@Builder
		@Getter
		@Setter
		@ToString
		public static class Annotation {

			public enum AnnotationType {
				FILE_CITATION("file_citation"), //
				FILE_PATH("file_path");

				private final String label;

				private AnnotationType(String label) {
					this.label = label;
				}

				@Override
				@JsonValue
				public String toString() {
					return label;
				}
			}

			private AnnotationType type;
			private String text;
			private Citation fileCitation;
			private Citation filePath;
			private int startIndex;
			private int endIndex;

			@NoArgsConstructor
			@AllArgsConstructor
			@Getter
			@Setter
			@ToString
			public static class Citation {
				private String fileId;
				private String quote;
			}
		} // Annotation
	} // Text
}
