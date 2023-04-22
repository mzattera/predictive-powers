/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client.completions;

import java.util.Map;

import io.github.mzattera.predictivepowers.openai.client.chat.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
@Builder
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
@ToString
public class CompletionsRequest implements Cloneable {

	@NonNull
	String model;

	String prompt;
	String suffix;

	/**
	 * Capabilities in the library will try to calculate this automatically if it is
	 * null when submitting a request.
	 */
	Integer maxTokens;

	Double temperature;
	Double topP;
	Integer n;

	// TODO: add support for this
	final boolean stream = false;

	Integer logprobs;
	Boolean echo;
	String[] stop;
	Double presencePenalty;
	Double frequencyPenalty;
	Integer bestOf;
	Map<String, Integer> logitBias;
	String user;
}
