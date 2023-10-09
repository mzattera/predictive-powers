/*
 * Copyright 2023 Massimiliano "Maxi" Zattera
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.mzattera.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.mzattera.predictivepowers.services.ModelService.Tokenizer;

/**
 * These are utility methods to chunk text in pieces, typically to embed the
 * pieces at a later stage (see
 * {@link io.github.mzattera.predictivepowers.services.EmbeddingService}).
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
public final class ChunkUtil {

	/**
	 * Same as <code>splitByTokens()</code> but uses number of chars instead of
	 * number of tokens when chunking.
	 */
	public static List<String> splitByChars(String text, int maxChars) {
		return splitByTokens(text, maxChars, CharTokenizer.getInstance());
	}

	/**
	 * Same as <code>splitByTokens()</code> but uses number of chars instead of
	 * number of tokens when chunking.
	 */
	public static List<String> splitByChars(String text, int maxChars, int windowSize, int stride) {
		return splitByTokens(text, maxChars, windowSize, stride, CharTokenizer.getInstance());
	}

	/**
	 * Same as calling <code>splitByTokens(text, chunkSize, 1, 1, tokenizer)</code>.
	 */
	public static List<String> splitByTokens(String text, int chunkSize, Tokenizer tokenizer) {
		return splitByTokens(text, chunkSize, 1, 1, tokenizer);
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
	 * no spaces within. This method is meant to be used with actual (meaningful)
	 * text.
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
	public static List<String> splitByTokens(String text, int chunkSize, int windowSize, int stride,
			Tokenizer tokenizer) {

		if (chunkSize < 1)
			throw new IllegalArgumentException("Chunks need to be of at least 1 token in size: " + chunkSize);
		if (windowSize < 1)
			throw new IllegalArgumentException("Sliding window must be of at least 1 chunk: " + windowSize);
		if (stride < 1)
			throw new IllegalArgumentException("Stride must be of at least 1 chunk: " + stride);
		if (stride > windowSize)
			throw new IllegalArgumentException("Stride bigger than window size, some chunks will be skipped");

		List<String> chunks = new ArrayList<>();
		chunks.add(text);
		if (tokenizer.count(text) <= chunkSize) { // text short enough it can be returned
			return chunks;
		}

		// Now split using decreasingly strong separators
		chunks = split(chunks, chunkSize, "[\\n]{2,}", tokenizer); // empty lines
		chunks = split(chunks, chunkSize, "\\.[\\s]+", tokenizer); // full stop (not in number)
		chunks = split(chunks, chunkSize, ";[\\s]+", tokenizer);
		chunks = split(chunks, chunkSize, ":[\\s]+", tokenizer);
		chunks = split(chunks, chunkSize, "\\,[\\s]+", tokenizer); // other common separators
		chunks = split(chunks, chunkSize, "[\\s]+", tokenizer); // spaces (including newlines)

		// We might have broken up too much, re-join tiny pieces
		chunks = merge(chunks, chunkSize, tokenizer);

		// Apply sliding window
		List<String> result = new ArrayList<>(); // for each window position
		if (windowSize == 1) {
			// No need to stride...
			result.addAll(chunks);
		} else {
			StringBuilder window = new StringBuilder();
			for (int i = 0; i < chunks.size(); i += stride) {
				for (int j = 0; (j < windowSize) && ((i + j) < chunks.size()); j++) {
					window.append(chunks.get(i + j));
				}
				result.add(window.toString());
			}
		}

		// trim
		List<String> trimmed = new ArrayList<>(result.size());
		for (String s : result)
			trimmed.add(s.trim());
		return trimmed;
	}

	/**
	 * Splits pieces bigger than max length into smaller pieces, using given regex
	 * as separator. Notice pieces already small enough are not touched.
	 */
	private static List<String> split(List<String> text, int maxTokens, String regex, Tokenizer counter) {
		List<String> result = new ArrayList<>();
		Pattern p = Pattern.compile(regex);

		for (String s : text) {
			if (counter.count(s) <= maxTokens) { // s short enough it can be ignored
				result.add(s);
				continue;
			}

			// Break down at given separator
			Matcher m = p.matcher(s);
			int start = 0;
			while (m.find()) {
				// Found the separator
				result.add(s.substring(start, m.start()) + m.group());
				start = m.end();
			}
			if (start < s.length()) { // add last unmatched bit
				result.add(s.substring(start));
			}
		}

		return result;
	}

	/**
	 * Merge text back, when possible.
	 */
	private static List<String> merge(List<String> text, int maxTokens, Tokenizer counter) {
		List<String> result = new ArrayList<>();

		StringBuilder tmp = new StringBuilder();
		for (String s : text) {
			int tok = counter.count(tmp.toString() + s);
			if (tok > maxTokens) { // if we add s, we are exceeding max length, output what we have so far
				if (tmp.length() > 0) { // output any merged text
					result.add(tmp.toString());
					tmp = new StringBuilder();
				}
				if (counter.count(s) > maxTokens) { // s is so big that it must stay alone (note tmp was already added)
					result.add(s);
				} else { // otherwise keep it for next batch
					tmp.append(s);
				}
			} else {
				tmp.append(s);
			}
		}

		// Add last piece
		if (tmp.length() > 0) {
			result.add(tmp.toString());
		}

		return result;
	}
}
