/**
 * 
 */
package io.github.mzattera.predictivepowers.client.openai.completions;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
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
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CompletionsRequest implements Cloneable {
	@NonNull
	@Builder.Default
	String model = "text-davinci-003";

	String prompt;

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

	@Builder.Default
	Map<String, Integer> logitBias = new HashMap<>();

	String user;

	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) { // shall never happen
			return null;
		}
	}
}
