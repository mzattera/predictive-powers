package io.github.mzattera.predictivepowers.client.openai;

import lombok.Getter;
import lombok.NonNull;

/**
 * Thjis exception is thrown when an error is returned by the OpenAI API in form
 * of an {@link OpenAiError}
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class OpenAiException extends RuntimeException {

	private static final long serialVersionUID = 2872745638990379630L;

	@Getter
	@NonNull
	private final OpenAiError error;

	public OpenAiException(@NonNull OpenAiError error, Throwable rootCause) {
		super(error.getError().message, rootCause);
		this.error = error;
	}
}
