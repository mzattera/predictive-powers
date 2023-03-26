/**
 * 
 */
package io.github.mzattera.predictivepowers;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * This class encapsulates a text response from the language model.
 * 
 * In addition to providing the returned text, this also contains a reason why
 * the response terminated, which allows the developer to take corrective
 * measures, eventually.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
@RequiredArgsConstructor
@ToString
public final class TextResponse {

	/** Reason why the language model finished responding. */
	public enum FinishReason {
		/** API returned complete model output */
		COMPLETED,

		/** Incomplete model output due to max_tokens parameter or token limit */
		LENGTH_LIMIT_REACHED,

		/** API response still in progress or incomplete */
		INCOMPLETE,

		/** Omitted content due to content filters */
		INAPPROPRIATE,

		UNKNOWN
	}

	@Getter
	@NonNull
	private String text;

	@Getter
	@NonNull
	private FinishReason finishReason;

	public static TextResponse fromGptApi(String text, String reason) {
		if (reason.equals("stop"))
			return new TextResponse(text, FinishReason.COMPLETED);
		if (reason.equals("length"))
			return new TextResponse(text, FinishReason.LENGTH_LIMIT_REACHED);
		if (reason.equals("content_filter"))
			return new TextResponse(text, FinishReason.INAPPROPRIATE);
		if (reason.equals("null"))
			return new TextResponse(text, FinishReason.INCOMPLETE);
		return new TextResponse(text, FinishReason.UNKNOWN);
	}
}
