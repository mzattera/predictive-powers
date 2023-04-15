/**
 * 
 */
package io.github.mzattera.predictivepowers.service;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * This describes a question/answer pair, ass extracted by a question service.
 * 
 * @author Massimliano "Maxi" Zattera.
 *
 */
@Getter
@Setter
@ToString
public class QnAPair {

	@NonNull
	private String question;

	@NonNull
	private String answer;

	/** Context from which the pair was extracted, if any. */
	private String context;

	/** For 'multiple-choice' questions, this is the list of choices */
	private String[] options;
}
