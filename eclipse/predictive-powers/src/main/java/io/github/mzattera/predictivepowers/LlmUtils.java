/**
 * 
 */
package io.github.mzattera.predictivepowers;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Some utility methods.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
public final class LlmUtils {

	/**
	 * 
	 * @param text      A text to be split.
	 * @param maxTokens Maximum number of tokens in each piece of the split text.
	 * @return Input text, split in parts smaller than given number of tokens.
	 */
	public static List<String> split(String text, int maxTokens) {

		// Eliminate empty lines
		text = text.replaceAll("[\\n]{2,}", "\n");

		List<String> result = new ArrayList<>();
		result.add(text);

		// Now split using decreasing string separators
		result = split(result, maxTokens, "\n"); // newlines
		result = split(result, maxTokens, "."); // full stop
		result = split(result, maxTokens, ","); // other common separators
		result = split(result, maxTokens, ";");
		result = split(result, maxTokens, ":");
		result = split(result, maxTokens, "\t");
		result = split(result, maxTokens, " "); // spaces

		// We might have broken up too much, re-join tiny pieces
		return merge(result, maxTokens);
	}

	/**
	 * Splits pieces bigger than max length into smaller, using given char as
	 * separator. Notice pieces already small enough are not touched.
	 * 
	 * @param text
	 * @param maxTokens
	 * @return
	 */
	private static List<String> split(List<String> text, int maxTokens, String string) {
		List<String> result = new ArrayList<>();

		for (String s : text) {
			if (TokenCalculator.count(s) <= maxTokens) { // s short enough it can be ignored
				result.add(s);
				continue;
			}

			// Break down at given separator
			String[] parts = s.split(Pattern.quote(string), -1); // keep empty strings when splitting
			for (int i = 0; i < parts.length; ++i) {
				if (i == parts.length - 1)
					result.add(parts[i]);
				else
					result.add(parts[i] + string);
			}
		}

		return result;
	}

	private static List<String> merge(List<String> text, int maxTokens) {
		List<String> result = new ArrayList<>();

		StringBuffer tmp = new StringBuffer();
		int tok = 0;
		for (String s : text) {
			int t = TokenCalculator.count(s);
			if ((t + tok) > maxTokens) { // we are exceeding max length, output what we have so far
				if (tmp.length() > 0) { // output any merged text
					result.add(tmp.toString());
					tmp = new StringBuffer();
					tok = 0;
				}
				if (t > maxTokens) { // s is so big that it must stay alone
					result.add(s);
				} else { // otherwise keep it for next batch
					tmp.append(s);
					tok = t;
				}
			} else {
				tmp.append(s);
				tok += t;
			}
		}

		// Add last piece
		if (tmp.length() > 0) {
			result.add(tmp.toString());
		}

		return result;
	}

	public static void main(String[] args) {
		String t = "nel mezzo del cammin di notstra vita, mi ritrovai in una selva\n oscura,, che la diritta viea era perduta. o qual cosa dura...";

		System.out.println("---------------------");
		for (String s : split(t, 10)) {
			System.out.println(">" + s + "<");
			System.out.println("---------------------");
		}
	}
}
