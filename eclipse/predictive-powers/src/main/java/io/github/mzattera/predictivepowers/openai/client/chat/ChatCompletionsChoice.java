package io.github.mzattera.predictivepowers.openai.client.chat;

import io.github.mzattera.predictivepowers.service.ChatMessage;
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
public class ChatCompletionsChoice {
	
	int index;
	ChatMessage message;
	String finishReason;
}
