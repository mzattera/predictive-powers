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
package io.github.mzattera.predictivepowers.util;

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
	 * 
	 * @author Luna
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
}
