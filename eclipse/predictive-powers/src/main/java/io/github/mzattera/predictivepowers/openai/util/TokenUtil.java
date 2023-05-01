/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.util;

import java.util.Collection;

import io.github.mzattera.predictivepowers.service.ChatMessage;
import lombok.NonNull;

/**
 * Utility class to calculate number of tokens in strings.
 * 
 * These calculations are approximated at the moment.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public final class TokenUtil {

	// TODO improve using
	// https://github.com/openai/openai-python/blob/main/chatml.md

	/**
	 * 
	 * @param text
	 * @return Approximated number of tokens in given text.
	 */
	public static int count(@NonNull String text) {
		return (int) ((text.length() + 2) / 3);
	}

	/**
	 * If you have an entire conversation, please notice {@link TokenUtil#count(Collection)} is more suitable and returns more correct
	 * results.
	 * 
	 * @param text
	 * @return Approximated number of tokens in given chat message.
	 */
	public static int count(@NonNull ChatMessage msg) {
		int c = count(msg.getContent());
		if (msg.getName() != null)
			c++;
		return c + 4;
	}

	/**
	 * 
	 * @param text
	 * @return Approximated number of tokens in given list of chat messages.
	 */
	public static int count(@NonNull Collection<ChatMessage> msgs) {
		int c = 0;
		for (ChatMessage m : msgs)
			c += count(m);
		return c + 3;
	}
}
