package io.github.mzattera.predictivepowers.client;

/**
 * Represents the error body when an OpenAI request fails
 */
public class OpenAiException extends Exception {

	private static final long serialVersionUID = -3652774591466857969L;
	
	public ErrorDetails error;

    public static class ErrorDetails {
        public String message;
        public String type;
        public String param;
        public String code;
    }
}
