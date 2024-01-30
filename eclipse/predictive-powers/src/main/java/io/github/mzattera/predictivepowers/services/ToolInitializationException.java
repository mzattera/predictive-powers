/**
 * 
 */
package io.github.mzattera.predictivepowers.services;

/**
 * This exception is thrown when a {@link Tool} fails to initialize. 
 */
public class ToolInitializationException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public ToolInitializationException() {
	}

	/**
	 * @param message
	 */
	public ToolInitializationException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public ToolInitializationException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public ToolInitializationException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public ToolInitializationException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
