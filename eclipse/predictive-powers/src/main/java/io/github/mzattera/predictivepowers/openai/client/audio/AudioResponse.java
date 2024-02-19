package io.github.mzattera.predictivepowers.openai.client.audio;

import java.util.ArrayList;
import java.util.List;

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
 * Response from audio OpenAI API.
 * 
 * @author GPT-4
 * 
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class AudioResponse {

	/**
	 * The task for which the transcription is done.
	 */
	private String task;

	/**
	 * The language of the transcription.
	 */
	private String language;

	/**
	 * The duration of the audio in hours. Optional.
	 */
	private Double duration;

	/**
	 * The transcribed text. Required field.
	 */
	@NonNull
	private String text;

	/**
	 * A list of segments in the transcription.
	 */
	@NonNull
	@Builder.Default
	private List<Segment> segments = new ArrayList<>();

	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@RequiredArgsConstructor
	@AllArgsConstructor
	@Builder
	@Getter
	@Setter
	@ToString
	public static class Segment {

		/**
		 * The identifier for the segment.
		 */
		private int id;

		/**
		 * The position to seek to in seconds. Required field.
		 */
		@NonNull
		private Integer seek;

		/**
		 * The start time of the segment in seconds. Required field.
		 */
		@NonNull
		private Double start;

		/**
		 * The end time of the segment in seconds. Required field.
		 */
		@NonNull
		private Double end;

		/**
		 * The text of the segment. Required field.
		 */
		@NonNull
		private String text;

		/**
		 * A list of tokens in the segment.
		 */
		@NonNull
		@Builder.Default
		private List<Long> tokens = new ArrayList<>();

		/**
		 * The temperature of the segment. Optional.
		 */
		private Double temperature;

		/**
		 * The average log probability of the segment. Optional.
		 */
		private Double avgLogprob;

		/**
		 * The compression ratio of the segment. Optional.
		 */
		private Double compressionRatio;

		/**
		 * The probability that no speech was detected in the segment. Optional.
		 */
		private Double noSpeechProb;
	}
}
