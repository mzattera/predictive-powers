/**
 * 
 */
package io.github.mzattera.predictivepowers.client.openai.completions;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Parameters for a request to /completions API.
 * 
 * @author Massmiliano "Maxi" Zattera.
 *
 */
@Getter
@Setter
@ToString
public class CompletionsParameters {
	Integer maxTokens;
	Double temperature;
	Double topP;
	Integer n;
	final Boolean stream = false;
	Integer logProbs;
	Boolean echo;
	String stop;
	Double presencePenalty;
	Double frequencyPenalty;
	Integer bestOf;
	Map<String, Integer> logitBias = new HashMap<>();
	String user;
}
