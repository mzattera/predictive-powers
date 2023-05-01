/**
 * 
 */
package io.github.mzattera.predictivepowers.service;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
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
@Builder
@ToString
public class QnAPair {

	@NonNull
	private String question;

	@NonNull
	private String answer;

	/** For 'multiple-choice' questions, this is the list of choices */
	private List<String> options;

	/** Context from which the pair was created. */
	@NonNull
	@Builder.Default
	private List<String> context = new ArrayList<>();
}
