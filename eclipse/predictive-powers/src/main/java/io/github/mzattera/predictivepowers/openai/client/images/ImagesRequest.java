/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client.images;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Request for OpenAi /images/generations API.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
// @RequiredArgsConstructor
@AllArgsConstructor
@ToString
public class ImagesRequest {

	/**
	 * The size of the generated images. Must be one of "256x256", "512x512", or
	 * "1024x1024". Defaults to "1024x1024".
	 */
	public enum ImageSize {
		_256x256("256x256"), _512x512("512x512"), _1024x1024("1024x1024");

		private final String label;

		private ImageSize(String label) {
			this.label = label;
		}
				
		@Override
		@JsonValue
		public String toString() {
			return label;
		}
	}

	/**
	 * The format in which the generated images are returned. Must be one of "url" or "b64_json".
	 */
	public enum ResponseFormat {
		URL("url"), BASE_64("b64_json");

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

	// Can be null for edits and variations
	String prompt;
	Integer n;
	ImageSize size;
	ResponseFormat responseFormat;
	String user;
}
