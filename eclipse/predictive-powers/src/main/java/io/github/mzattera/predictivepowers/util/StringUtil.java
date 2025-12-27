/*
 * Copyright 2023-2025 Massimiliano "Maxi" Zattera
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
package io.github.mzattera.predictivepowers.util;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Utilities to manipulate strings.
 */
public final class StringUtil {

	private StringUtil() {}
	
	/**
	 * Converts a CamelCase string into a space-separated string.
	 * Sequences of uppercase letters are treated as a single word.
	 *
	 * Examples:
	 *  - "TestCase" → "Test Case"
	 *  - "getURL" → "Get URL"
	 *  - "ToURL" → "To URL"
	 *
	 * @param input the CamelCase input string
	 * @return a space-separated string
	 */
	public static String camelCaseToWords(String input) {
	    if (input == null || input.isEmpty()) {
	        return input;
	    }

	    // Use a regular expression to insert a space before each uppercase letter
	    // that is preceded by a lowercase letter or a digit
	    String result = input.replaceAll(
	        "(?<=[a-z0-9])(?=[A-Z])", " "
	    );

	    // Handle the case where an uppercase letter is followed by another uppercase and a lowercase:
	    // e.g., "XMLParser" → "XML Parser"
	    result = result.replaceAll(
	        "(?<!^)(?=[A-Z][a-z])(?<=[A-Z])", " "
	    );

	    // Capitalize first letter
	    return result.substring(0, 1).toUpperCase() + result.substring(1);
	}

	/**
	 * Replaces 'slots' in a prompt.
	 * 
	 * Slots are placeholders inserted in the prompt using a syntax like {{name}}
	 * where 'name' is a key in the provided Map; these placeholders will be
	 * replaced with corresponding map value (using {@link Object#toString()}).
	 * 
	 * Parameters with a null value will result in a deleted slot, slots without
	 * corresponding parameters in the map will be ignored (and not replaced).
	 * 
	 * @param prompt
	 * @param parameters
	 * @return
	 */
	public static String fillSlots(String prompt, Map<String, ? extends Object> parameters) {
		if ((prompt == null) || (parameters == null))
			return prompt;
	
		for (Entry<String, ? extends Object> e : parameters.entrySet()) {
			String regex = "{{" + e.getKey() + "}}"; // No need to Pattern.quote()
			if (e.getValue() == null)
				prompt = prompt.replace(regex, "");
			else
				prompt = prompt.replace(regex, e.getValue().toString());
		}
		return prompt;
	}
}
