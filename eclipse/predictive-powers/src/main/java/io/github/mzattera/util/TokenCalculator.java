/**
 * 
 */
package io.github.mzattera.util;

import io.github.mzattera.predictivepowers.openai.client.chat.ChatMessage;
import lombok.NonNull;

/**
 * Utility class to calculate number of tokens in strings.
 * 
 * These calculations are approximated at the moment.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public final class TokenCalculator {

	// TODO improve using
	// https://github.com/openai/openai-python/blob/main/chatml.md

	/**
	 * 
	 * @param text
	 * @return Approximated number of tokens in given text.
	 */
	public static int count(@NonNull String text) {
		return (text.length()+2) / 4;
	}

	/**
	 * 
	 * @param text
	 * @return Approximated number of tokens in given chat message.
	 */
	public static int count(@NonNull ChatMessage msg) {
		int c = count(msg.getContent());
		if (msg.getName() != null)
			c++;
		return c;
	}
}
