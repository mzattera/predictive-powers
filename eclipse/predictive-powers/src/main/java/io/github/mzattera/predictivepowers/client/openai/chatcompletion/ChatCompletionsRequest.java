package io.github.mzattera.predictivepowers.client.openai.chatcompletion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
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
@AllArgsConstructor
@ToString
public class ChatCompletionsRequest implements Cloneable {
	@NonNull
	@Builder.Default
	String model = "gpt-3.5-turbo";

	@NonNull
	@Builder.Default
	List<ChatMessage> messages = new ArrayList<>();;

	Double temperature;
	Double topP;
	Integer n;
	final Boolean stream = false;
	List<ChatMessage> stop;

	@Builder.Default
	Integer maxTokens = 256;

	Double presencePenalty;
	Double frequencyPenalty;
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
