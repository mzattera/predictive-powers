package io.github.mzattera.predictivepowers.openai.client.chat;

import java.util.List;

import io.github.mzattera.predictivepowers.openai.client.Usage;
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
	List<ChatCompletionsChoice> choices;
	Usage usage;
}