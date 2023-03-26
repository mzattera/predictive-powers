package io.github.mzattera.predictivepowers.client.openai.chatcompletion;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A single message in the /chat/completions API.
 * 
 * @author Massmiliano "Maxi" Zattera.
 *
 */
@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
@ToString
public class ChatMessage {
	@NonNull
	String role;

	@NonNull
	String content;
}
