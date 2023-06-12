
package io.github.mzattera.predictivepowers;

import java.util.Collection;
import java.util.List;

import io.github.mzattera.predictivepowers.services.ChatMessage;
import lombok.NonNull;

/**
 * A TokenCounter counts "tokens" composing a text or a set of messages.
 * 
 * This is required because some models (namely OpenAI GPT) have constrains on
 * maximum number of input and output tokens.
 * 
 * A TokenCounter normally relies on an underlying tokenizer, which is
 * model-specific.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public interface TokenCounter {

	/**
	 * 
	 * @param text
	 * @return Number of tokens in given text.
	 */
	int count(@NonNull String text);

	/**
	 * If you have an entire conversation, please notice {@link #count(Collection)}
	 * is more suitable and returns more correct results.
	 * 
	 * @param text
	 * @return Number of tokens in given chat message.
	 */
	int count(@NonNull ChatMessage msg);

	/**
	 * 
	 * @param msgs Messages in a conversation.
	 * @return Number of tokens in given set of chat messages (conversation).
	 */
	int count(@NonNull List<ChatMessage> msgs);
}
