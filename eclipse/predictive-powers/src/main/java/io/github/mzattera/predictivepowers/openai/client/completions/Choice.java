/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client.completions;

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
public class Choice {
	String text;
	int index;
	Logprob logprobs;
	String finishReason;
}
