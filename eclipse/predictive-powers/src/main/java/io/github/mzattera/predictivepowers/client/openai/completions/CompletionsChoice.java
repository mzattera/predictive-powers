/**
 * 
 */
package io.github.mzattera.predictivepowers.client.openai.completions;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Choices returned within a /completions response.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@Getter
@Setter
@ToString
public class CompletionsChoice {
	String text;
	Integer index;
	LogProbResult logprobs;
	String finishReason;
}
