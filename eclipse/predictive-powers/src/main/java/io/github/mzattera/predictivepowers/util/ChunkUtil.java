/*
 * Copyright 2023-2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */

package io.github.mzattera.predictivepowers.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.mzattera.predictivepowers.services.ModelService.Tokenizer;

/**
 * Faster drop-in replacement for {@code ChunkUtil}. Public behavior is
 * identical, but internal implementation allocates less, re-uses compiled
 * patterns and minimizes token counting.
 *
 * @author Luna
 */
public final class ChunkUtil {

	/* Pre-compiled patterns, ordered from strongest to weakest separator */
	private static final Pattern[] SPLIT_PATTERNS = { Pattern.compile("[\\n]{2,}"), Pattern.compile("\\.[\\s]+"),
			Pattern.compile(";[\\s]+"), Pattern.compile(":[\\s]+"), Pattern.compile("\\,[\\s]+"),
			Pattern.compile("[\\s]+") };

	private ChunkUtil() {
	}

	/**
	 * Chunks given text using char count. This is same as calling
	 * <code>split(text, chunkSize, 1, 1, CharTokenizer.getInstance())</code>.
	 */
	public static List<String> split(String text, int maxChars) {
		return split(text, maxChars, CharTokenizer.getInstance());
	}

	/**
	 * Chunks given text using char count. This is same as calling
	 * <code>split(text, maxChars, windowSize, stride, CharTokenizer.getInstance())</code>.
	 */
	public static List<String> split(String text, int maxChars, int windowSize, int stride) {
		return split(text, maxChars, windowSize, stride, CharTokenizer.getInstance());
	}

	/**
	 * Same as calling <code>split(text, chunkSize, 1, 1, tokenizer)</code>.
	 */
	public static List<String> split(String text, int chunkSize, Tokenizer tokenizer) {
		return split(text, chunkSize, 1, 1, tokenizer);
	}

	/**
	 * This method chunks given text accordingly to the below steps.
	 * 
	 * 1. The text is initially split in chunks of at most 'chunkSize' tokens.
	 * 
	 * 2. A sliding window is created, of size 'windowSize' chunks and is positioned
	 * at the first chunk.
	 * 
	 * 3. All chunks in the sliding window are merged together, the merged text is
	 * added to the result of the call.
	 * 
	 * 4. The window is moved forward by 'stride' chunks. If it did not move outside
	 * the list of chunks, go back to step 3.
	 * 
	 * This process allows you to split a text in chunks of arbitrary size, allowing
	 * some overlapping of their text, if desired.
	 * 
	 * Notice that, in order to optimize results, this splits the text at common
	 * text separators (newlines, columns, etc.) therefore it might not be able to
	 * split the text if the file is, for example, a long sequence of letters with
	 * no spaces within.
	 * 
	 * @param text       A text to be split into chunks.
	 * @param chunkSize  Maximum number of tokens in each chunk of the split text.
	 * @param windowSize Size of the moving window (in chunks).
	 * @param stride     Chunks skipped each time the sliding window moves.
	 * @param tokenizer  {@link Tokenizer} used to count tokens.
	 * 
	 * @return Input text, chunked accordingly the above algorithm. Notice each text
	 *         in the list being returned is therefore of size chunkSize *
	 *         windowSize (at most).
	 */
	public static List<String> split(String text, int chunkSize, int windowSize, int stride, Tokenizer tokenizer) {

		if (chunkSize < 1)
			throw new IllegalArgumentException("Chunks must be at least 1 token: " + chunkSize);
		if (windowSize < 1)
			throw new IllegalArgumentException("Window must be at least 1 chunk: " + windowSize);
		if (stride < 1)
			throw new IllegalArgumentException("Stride must be at least 1 chunk: " + stride);
		if (stride > windowSize)
			throw new IllegalArgumentException("Stride larger than window; chunks may be skipped");

		text = text == null ? "" : text.trim();
		if (text.isEmpty())
			return List.of();

		if (tokenizer.count(text) <= chunkSize)
			return List.of(text);

		List<String> chunks = new ArrayList<>(8);
		chunks.add(text);

		/* Progressive splitting; stop early if every piece fits */
		for (Pattern p : SPLIT_PATTERNS) {
			boolean oversizedFound = false;
			List<String> next = new ArrayList<>(chunks.size());

			for (String s : chunks) {
				if (tokenizer.count(s) <= chunkSize) {
					next.add(s);
					continue;
				}
				/* Split on current pattern */
				Matcher m = p.matcher(s);
				int start = 0;
				while (m.find()) {
					next.add(s.substring(start, m.start()) + m.group());
					start = m.end();
				}
				if (start < s.length())
					next.add(s.substring(start));

				oversizedFound = true;
			}
			chunks = next;
			if (!oversizedFound)
				break;
		}

		chunks = merge(chunks, chunkSize, tokenizer);

		/* Sliding window */
		List<String> result = new ArrayList<>(chunks.size());
		if (windowSize == 1) {
			result.addAll(chunks);
		} else {
			for (int i = 0; i < chunks.size(); i += stride) {
				StringBuilder sb = new StringBuilder();
				for (int j = 0; j < windowSize && i + j < chunks.size(); j++) {
					sb.append(chunks.get(i + j));
				}
				result.add(sb.toString());
			}
		}

		/* Final trim */
		List<String> trimmed = new ArrayList<>(result.size());
		for (String s : result) {
			String t = s.trim();
			if (!t.isEmpty())
				trimmed.add(t);
		}
		return trimmed;
	}

	/**
	 * Re-merge adjacent tiny pieces without exceeding {@code maxTokens}. If a
	 * single piece already exceeds {@code maxTokens}, it is kept as-is.
	 */
	private static List<String> merge(List<String> parts, int maxTokens, Tokenizer tokenizer) {
		List<String> result = new ArrayList<>(parts.size());

		StringBuilder buffer = new StringBuilder();

		for (String part : parts) {
			// Keep oversize piece as-is (rule requested by you)
			if (tokenizer.count(part) > maxTokens) {
				if (buffer.length() > 0) { // flush what we have
					result.add(buffer.toString());
					buffer.setLength(0);
				}
				result.add(part);
				continue;
			}

			// Check real tokens after concatenation (no additive shortcut)
			if (tokenizer.count(buffer.toString() + part) > maxTokens) {
				result.add(buffer.toString()); // flush, start new buffer
				buffer.setLength(0);
			}

			buffer.append(part);
		}

		if (buffer.length() > 0)
			result.add(buffer.toString());

		return result;
	}
}
