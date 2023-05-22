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

/**
 * 
 */
package io.github.mzattera.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.mzattera.predictivepowers.openai.util.TokenUtil;

/**
 * Some utility methods to deal with Large Language Models.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
public final class LlmUtil {

	/**
	 * 
	 * @param text      A text to be split.
	 * @param maxTokens Maximum number of tokens in each piece of the split text.
	 * @return Input text, split in parts smaller than given number of tokens. As we
	 *         do not have an exact way of calculating tokens, this is approximated.
	 */
	public static List<String> split(String text, int maxTokens) {

		List<String> result = new ArrayList<>();
		result.add(text);
		if (TokenUtil.count(text) <= maxTokens) { // s short enough it can be ignored
			return result;
		}

		// Now split using decreasingly strong separators
		result = split(result, maxTokens, "[\\n]{2,}"); // empty lines
		result = split(result, maxTokens, "\\.[\\s]+"); // full stop (not in number)
		result = split(result, maxTokens, "\\,[\\s]+"); // other common separators
		result = split(result, maxTokens, ";[\\s]+");
		result = split(result, maxTokens, ":[\\s]+");
		result = split(result, maxTokens, "[\\s]+"); // spaces (including newlines)

		// We might have broken up too much, re-join tiny pieces
		result = merge(result, maxTokens);

		// trim
		List<String> l = new ArrayList<>(result.size());
		for (String s : result)
			l.add(s.trim());
		return l;
	}

	/**
	 * Splits pieces bigger than max length into smaller pieces, using given regex as
	 * separator. Notice pieces already small enough are not touched.
	 * 
	 * @param text
	 * @param maxTokens
	 * @return
	 */
	private static List<String> split(List<String> text, int maxTokens, String regex) {
		List<String> result = new ArrayList<>();
		Pattern p = Pattern.compile(regex);

		for (String s : text) {
			if (TokenUtil.count(s) <= maxTokens) { // s short enough it can be ignored
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

	private static List<String> merge(List<String> text, int maxTokens) {
		List<String> result = new ArrayList<>();

		StringBuffer tmp = new StringBuffer();
		int tok = 0;
		for (String s : text) {
			int t = TokenUtil.count(s);
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
//		String t = "nel mezzo del cammin di notstra vita, mi ritrovai in una selva\n oscura,, che la diritta viea era perduta. o qual cosa dura...";
		String t = "nel mezzo del cammin di notstra vita, mi ritrovai in una selva\n oscura,, che la diritta viea era perduta. o qual cosa dura...";
		t = t+"\n\n"+t+"\n\n"+t;
		//		String t = TestKB.TEXT01;

		System.out.println(t);
		System.out.println("---------------------");
		for (String s : split(t, 20)) {
			System.out.println(">" + s + "<");
			System.out.println("---------------------");
		}
	}
}
