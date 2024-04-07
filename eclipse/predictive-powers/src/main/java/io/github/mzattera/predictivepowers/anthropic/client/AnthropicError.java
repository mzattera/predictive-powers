package io.github.mzattera.predictivepowers.anthropic.client;

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

@NoArgsConstructor
@RequiredArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class AnthropicError {

	public static enum ErrorType {
		ERROR("error");

		private final String value;

		ErrorType(String value) {
			this.value = value;
		}

		@Override
		@JsonValue
		public String toString() {
			return value;
		}
	}

	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@AllArgsConstructor
	@Builder
	@Getter
	@Setter
	@ToString
	public static class ErrorDetails {

		@NonNull
		private String type;

		@NonNull
		private String message;
	}

	@NonNull
	private ErrorType type;

	@NonNull
	private ErrorDetails error;
}
