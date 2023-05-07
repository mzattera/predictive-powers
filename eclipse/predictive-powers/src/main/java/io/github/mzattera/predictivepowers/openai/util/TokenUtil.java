/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.util;

import java.util.Collection;

import io.github.mzattera.predictivepowers.services.ChatMessage;
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
	 * If you have an entire conversation, please notice
	 * {@link TokenUtil#count(Collection)} is more suitable and returns more correct
	 * results.
	 * 
	 * @param text
	 * @return Approximated number of tokens in given chat message.
	 */
	public static int count(@NonNull ChatMessage msg) {
		int c = count(msg.getContent());
		return c + 5; // Takes in account name and message formatting
	}

	/**
	 * 
	 * @param text
	 * @return Approximated number of tokens in given list of chat messages.
	 */
	public static int count(@NonNull Collection<ChatMessage> msgs) {
		int c = 3; // takes in account answer encoding
		for (ChatMessage m : msgs)
			c += count(m);
		return c;
	}
}