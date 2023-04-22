package io.github.mzattera.predictivepowers.openai.client.chat;

import io.github.mzattera.predictivepowers.openai.client.completions.Usage;
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
	long created;
	String model;
	ChatCompletionsChoice[] choices;
	Usage usage;
}
