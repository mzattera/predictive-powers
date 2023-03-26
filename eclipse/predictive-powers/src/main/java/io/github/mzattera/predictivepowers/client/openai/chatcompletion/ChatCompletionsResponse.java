package io.github.mzattera.predictivepowers.client.openai.chatcompletion;

import java.util.List;

import io.github.mzattera.predictivepowers.client.openai.completions.Usage;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Response from /chat/completions API.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
@Getter
@Setter
@ToString
public class ChatCompletionsResponse {
	String id;
	String object;
	Long created;
	String model;
	List<ChatCompletionChoice> choices;
	Usage usage;
}
