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
package io.github.mzattera.predictivepowers.openai.util;

import java.util.HashMap;
import java.util.Map;

/**
 * This class provides method to get information about standard OpenAI models.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
public final class ModelUtil {

	private ModelUtil() {
	}

	private final static Map<String, Integer> contextSize = new HashMap<>();
	static {
		contextSize.put("ada", 2049);
		contextSize.put("babbage", 2049);
		contextSize.put("code-cushman-001", 2048);
		contextSize.put("code-cushman-002", 2048);
		contextSize.put("code-davinci-001", 8001);
		contextSize.put("code-davinci-002", 8001);
		contextSize.put("code-search-ada-code-001", 2046);
		contextSize.put("code-search-ada-text-001", 2046);
		contextSize.put("code-search-babbage-code-001", 2046);
		contextSize.put("code-search-babbage-text-001", 2046);
		contextSize.put("curie", 2049);
		contextSize.put("davinci", 2049);
		contextSize.put("gpt-3.5-turbo", 4096);
		contextSize.put("gpt-3.5-turbo-0301", 4096);
		contextSize.put("gpt-4", 8192);
		contextSize.put("gpt-4-32k", 32768);
		contextSize.put("text-ada-001", 2049);
		contextSize.put("text-babbage-001", 2049);
		contextSize.put("text-curie-001", 2049);
		contextSize.put("text-davinci-002", 4097);
		contextSize.put("text-davinci-003", 4097);
		contextSize.put("text-embedding-ada-002", 8191);
		contextSize.put("text-search-ada-doc-001", 2046);
		contextSize.put("text-search-ada-query-001", 2046);
		contextSize.put("text-search-babbage-doc-001", 2046);
		contextSize.put("text-search-babbage-query-001", 2046);
		contextSize.put("text-search-curie-doc-001", 2046);
		contextSize.put("text-search-curie-query-001", 2046);
		contextSize.put("text-search-davinci-doc-001", 2046);
		contextSize.put("text-search-davinci-query-001", 2046);
		contextSize.put("text-similarity-ada-001", 2046);
		contextSize.put("text-similarity-babbage-001", 2046);
		contextSize.put("text-similarity-curie-001", 2046);
		contextSize.put("text-similarity-davinci-001", 2046);
	};
	
	/**
	 * 
	 * @param model
	 * @return Context size in token for given model, or -1 if the size is unknown.
	 */
	public static int getContextSize(String model) {
		return contextSize.getOrDefault(model, -1);
	}
}
