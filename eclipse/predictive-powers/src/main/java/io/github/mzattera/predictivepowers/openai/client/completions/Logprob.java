/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client.completions;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Probabilities returned within a /completions response.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@Getter
@Setter
@ToString
public class Logprob {

	List<String> tokens;
	List<Double> tokenLogprobs;
	List<Map<String, Double>> topLogprobs;
	List<Integer> textOffset;
}
