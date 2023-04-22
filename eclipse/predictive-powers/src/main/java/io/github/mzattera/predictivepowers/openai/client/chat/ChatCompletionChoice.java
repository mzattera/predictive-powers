package io.github.mzattera.predictivepowers.openai.client.chat;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Response text (choice) from /chat/completions API.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
@Getter
@Setter
@ToString
public class ChatCompletionChoice {
	int index;
	ChatMessage message;
	String finishReason;
}
