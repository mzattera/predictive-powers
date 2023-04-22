/**
 * 
 */
package io.github.mzattera.predictivepowers.service;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

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

	/** Context from which the pair was extracted, if it was provided as a simple text. */
	private String simpleContext;

	/** Context from which the pair was extracted, if it was provided as a list of embeddings (typically from a knowledge base). */
	private List<Pair<EmbeddedText, Double>> kbContext;

	/** For 'multiple-choice' questions, this is the list of choices */
	private String[] options;
}
