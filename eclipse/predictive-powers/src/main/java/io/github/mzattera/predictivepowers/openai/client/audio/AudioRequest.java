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
 * Request ofr /audio OpneAi API.
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
public class AudioRequest {

	/**
	 * The format of the transcript output, in one of these options: json, text,
	 * srt, verbose_json, or vtt.
	 */
	public enum ResponseFormat {
		JSON("json"), TEXT("text"), SRT("srt"), VERBOSE_JSON("verbose_json"), VTT("vtt");

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
	
	String prompt;
	ResponseFormat responseFormat;
	Double temperature;
	String language;
}
