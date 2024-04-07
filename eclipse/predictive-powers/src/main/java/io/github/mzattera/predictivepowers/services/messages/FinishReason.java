/**
 * 
 */
package io.github.mzattera.predictivepowers.services.messages;

/**
 * This enumeration describes possible ways in which a language model completed
 * its output.
 * 
 * @author Massimiliano "Maxi" Zattera.
 */
public enum FinishReason {

	/**
	 * Text generation is not yet completed, model might be returning a partial
	 * result (e.g. to allow streaming).
	 */
	IN_PROGRESS,

	/**
	 * Text generation has successfully terminated and the text is complete.
	 */
	COMPLETED,

	/**
	 * Text generation is finished, but the text was truncated, probably for
	 * limitations in model output length.
	 */
	TRUNCATED,

	/**
	 * Text content was in part or completely omitted due to content filters (e.g.
	 * profanity filter)
	 */
	INAPPROPRIATE,

	/** All finish reasons that do not fit in any other value */
	OTHER;

	public static FinishReason fromOpenAiApi(String reason) {
		switch (reason) {
		case "stop":
			return FinishReason.COMPLETED;
		case "length":
			return FinishReason.TRUNCATED;
		case "content_filter":
			return FinishReason.INAPPROPRIATE;
		default:
			return FinishReason.OTHER;
		}
	}

	public static FinishReason fromAnthropicApi(String reason) {
		switch (reason) {
		case "end_turn":
		case "stop_sequence":
			return FinishReason.COMPLETED;
		case "max_tokens":
			return FinishReason.TRUNCATED;
		default:
			return FinishReason.OTHER;
		}
	}
}
