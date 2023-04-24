package io.github.mzattera.predictivepowers.openai.client.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Parameters for a request to /chat/completions API.
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
public class ChatCompletionsRequest implements Cloneable {

	@NonNull
	String model;

	@NonNull
	@Builder.Default
	List<ChatMessage> messages = new ArrayList<>();

	Double temperature;
	Double topP;
	Integer n;

	// TODO: Add support
	final boolean stream = false;

	List<String> stop;

	/**
	 * Capabilities in the library will try to calculate this automatically if it is
	 * null when submitting a request.
	 */
	Integer maxTokens;

	Double presencePenalty;
	Double frequencyPenalty;
	Map<String, Integer> logitBias;
	String user;
}
