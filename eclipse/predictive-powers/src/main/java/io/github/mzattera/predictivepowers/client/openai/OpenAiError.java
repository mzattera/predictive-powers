package io.github.mzattera.predictivepowers.client.openai;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents the error body when an OpenAI request fails
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@Setter
@Getter
@ToString
public class OpenAiError {

	ErrorDetails error;

	@Getter
	@Setter
	@ToString
	public static class ErrorDetails {
		String message;
		String type;
		String param;
		String code;
	}
}
