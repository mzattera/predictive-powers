/**
 * 
 */
package io.github.mzattera.predictivepowers.client.openai.completions;

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
public class LogProbResult {
	List<String> tokens;
	List<Double> tokenLogprobs;
	List<Map<String, Double>> topLogprobs;
	List<Integer> textOffset;
}
