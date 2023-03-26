/**
 * 
 */
package io.github.mzattera.predictivepowers.client.openai.completions;

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

	/**
	 * This is the max_tokens parameter used by OpenAI API. Services typically
	 * change this, lowering it if needed, based on model prompt Size, to avoid API
	 * errors. If you use it for direct API calls, change it accordingly.
	 */
	@Builder.Default
	Integer maxTokens = 4096;

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
	Map<String, Integer> logitBias;
	String user;

	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) { // shall never happen
			return null;
		}
	}
}
