/**
 * 
 */
package io.github.mzattera.predictivepowers.services;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * This describes a question/answer pair, ass extracted by a question service.
 * 
 * @author Massimliano "Maxi" Zattera.
 *
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@RequiredArgsConstructor
public class QnAPair {

	@NonNull
	private String question;

	@NonNull
	private String answer;

	/**
	 * Explanation for the question, if any, otherwise null. This is typically
	 * provided if the answer is generated from a context.
	 */
	private String explanation;

	/** For 'multiple-choice' questions, this is the list of choices */
	@NonNull
	@Builder.Default
	private List<String> options = new ArrayList<>();

	/** Context, as simple text, from which the pair was created. */
	@NonNull
	@Builder.Default
	private List<String> context = new ArrayList<>();

	/** Context, as list of embeddings, from which the pair was created. */
	@NonNull
	@Builder.Default
	private List<EmbeddedText> embeddingContext = new ArrayList<>();

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Question: ").append(question).append('\n');
		if (options.size() == 0) {
			builder.append("Answer:   ").append(answer);
		} else {
			int a = -1;
			try {
				a = Integer.parseInt(answer) - 1;
			} catch (NumberFormatException e) {
			}
			for (int i = 0; i < options.size(); ++i) {
				if (i == a)
					builder.append(" [X] ");
				else
					builder.append(" [ ] ");
				builder.append(options.get(i));
				if (i < (options.size() - 1))
					builder.append('\n');
			}
		}
		return builder.toString();
	}

}
