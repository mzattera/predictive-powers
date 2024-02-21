package io.github.mzattera.predictivepowers.openai.client.chat;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * @author GPT-4
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class LogProbs {

	/**
	 * A list of message content tokens with log probability information. Can be
	 * null.
	 */
	@Builder.Default
	private List<ContentToken> content = new ArrayList<>();

	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@AllArgsConstructor
	@Builder
	@Getter
	@Setter
	@ToString
	public static class ContentToken {

		/**
		 * The token.
		 */
		@NonNull
		private String token;

		/**
		 * The log probability of this token.
		 */
		private double logprob;

		/**
		 * A list of integers representing the UTF-8 bytes representation of the token.
		 * Can be null if there is no bytes representation for the token.
		 */
		@Builder.Default
		private List<Integer> bytes = new ArrayList<>();

		/**
		 * List of the most likely tokens and their log probability, at this token
		 * position.
		 */
		// This is non null only for LogProbs.content, ContentTokens inside
		// ContentToken.topLogprobs do not have this field
		private List<ContentToken> topLogprobs;
	}
}
