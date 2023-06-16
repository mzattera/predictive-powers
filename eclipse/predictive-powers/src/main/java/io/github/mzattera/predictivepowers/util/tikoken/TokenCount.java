/*
 * Copyright (c) 2023 Mariusz Bernacki <info@didalgo.com>
 * SPDX-License-Identifier: MIT
 */

/*
 * MIT License
 * 
 * Copyright (c) 2023 didalgo2
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.mzattera.predictivepowers.util.tikoken;

import java.util.List;
import java.util.stream.StreamSupport;

import io.github.mzattera.predictivepowers.services.ChatMessage;

/**
 * Utility class for calculating token count in text and chat messages.
 * <p>
 * This class provides methods for counting tokens in text strings and lists of
 * {@link ChatMessage} objects using a {@link GPT3Tokenizer}.
 *
 * @author Mariusz Bernacki
 *
 */
public class TokenCount {

	/**
	 * Calculates the total token count from a list of lines using the given
	 * tokenizer, including newline tokens between lines.
	 *
	 * @param lines     an iterable of lines of text (boring)
	 * @param tokenizer the magic thing that tokenizes text
	 * @return the total token count, including newline tokens between lines
	 */
	public static int fromLinesJoined(Iterable<String> lines, GPT3Tokenizer tokenizer) {
		int tokenCount = StreamSupport.stream(lines.spliterator(), false)
				.mapToInt(line -> fromString(line, tokenizer) + 1).sum();
		return Math.max(0, tokenCount - 1); // subtract 1 token for the last newline character
	}

	/**
	 * Calculates the token count for a given text string using the provided
	 * tokenizer.
	 *
	 * @param text      the text string to tokenize (probably lorem ipsum or
	 *                  something)
	 * @param tokenizer the tokenizer to use for token counting
	 * @return the token count for the input text
	 */
	public static int fromString(String text, GPT3Tokenizer tokenizer) {
		return tokenizer.encode(text).size();
	}

	/**
	 * Calculates the token count for a list of chat messages using the provided
	 * tokenizer and chat format descriptor.
	 *
	 * @param messages   a list of chat messages (probably gossip)
	 * @param tokenizer  the tokenizer to use for token counting
	 * @param chatFormat the descriptor defining the chat format
	 * @return the token count for the input chat messages
	 */
	public static int fromMessages(List<ChatMessage> messages, GPT3Tokenizer tokenizer,
			ChatFormatDescriptor chatFormat) {
		int tokenCount = 0;
		for (ChatMessage message : messages) {
			tokenCount += chatFormat.getExtraTokenCountPerMessage();
			if (message.getRole() != null)
				tokenCount += tokenizer.encode(message.getRole().toString()).size();
			if (message.getContent() != null)
				tokenCount += tokenizer.encode(message.getContent()).size();
		}
		tokenCount += chatFormat.getExtraTokenCountPerRequest(); // Every reply is primed with <im_start>assistant\n
		return tokenCount;
	}
}
