/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Error in the OpenAI API (for Runs API at the moment).
 * 
 * @author Massimiliano "Maxi" Zattera/
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
@SuperBuilder
@Getter
@Setter
@ToString
public class Error {

	public enum Code {
		SERVER_ERROR("server_error"), RATE_LIMIT_EXCEEDED("rate_limit_exceeded");

		private final String label;

		private Code(String label) {
			this.label = label;
		}

		@Override
		public String toString() {
			return label;
		}
	}

	@NonNull
	private Code code;

	@NonNull
	private String message;
}
