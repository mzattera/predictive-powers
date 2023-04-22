/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client.images;

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
 * Request for OpenAi /images/generations API.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
@ToString
public class ImagesGenerationsRequest {

	/**
	 * The size of the generated images. Must be one of "256x256", "512x512", or
	 * "1024x1024". Defaults to "1024x1024".
	 */
	public enum ImageSize {
		_256x256("256x256"), _512x512("512x512"), _1024x1024("1024x1024");

		@JsonValue
		public final String label;

		private ImageSize(String label) {
			this.label = label;
		}
	}

	@NonNull
	String prompt;

	Integer n;
	ImageSize size;
	String responseFormat;
	String user;
}
