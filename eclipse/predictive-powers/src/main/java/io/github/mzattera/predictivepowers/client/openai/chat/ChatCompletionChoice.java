package io.github.mzattera.predictivepowers.client.openai.chat;

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
	Integer index;
	ChatMessage message;
	String finishReason;
}
