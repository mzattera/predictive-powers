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

package io.github.mzattera.predictivepowers.openai.util;

import java.util.HashMap;
import java.util.Map;

import io.github.mzattera.predictivepowers.TokenCounter;
import io.github.mzattera.predictivepowers.openai.util.tokeniser.ChatFormatDescriptor;
import io.github.mzattera.predictivepowers.openai.util.tokeniser.Encoding;
import io.github.mzattera.predictivepowers.openai.util.tokeniser.GPT3Tokenizer;
import lombok.NonNull;

/**
 * This class provides method to get information about standard OpenAI models.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
public final class ModelUtil {

	private ModelUtil() {
	}

	private final static Map<String, Integer> CONTEXT_SIZES = new HashMap<>();
	static {
		CONTEXT_SIZES.put("ada", 2049);
		CONTEXT_SIZES.put("babbage", 2049);
		CONTEXT_SIZES.put("code-cushman-001", 2048);
		CONTEXT_SIZES.put("code-cushman-002", 2048);
		CONTEXT_SIZES.put("code-davinci-001", 8001);
		CONTEXT_SIZES.put("code-davinci-002", 8001);
		CONTEXT_SIZES.put("code-search-ada-code-001", 2046);
		CONTEXT_SIZES.put("code-search-ada-text-001", 2046);
		CONTEXT_SIZES.put("code-search-babbage-code-001", 2046);
		CONTEXT_SIZES.put("code-search-babbage-text-001", 2046);
		CONTEXT_SIZES.put("curie", 2049);
		CONTEXT_SIZES.put("davinci", 2049);
		CONTEXT_SIZES.put("gpt-3.5-turbo", 4096);
		CONTEXT_SIZES.put("gpt-3.5-turbo-0301", 4096);
		CONTEXT_SIZES.put("gpt-4", 8192);
		CONTEXT_SIZES.put("gpt-4-32k", 32768);
		CONTEXT_SIZES.put("text-ada-001", 2049);
		CONTEXT_SIZES.put("text-babbage-001", 2049);
		CONTEXT_SIZES.put("text-curie-001", 2049);
		CONTEXT_SIZES.put("text-davinci-002", 4093); // Documentation says 4097 but it is incorrect
		CONTEXT_SIZES.put("text-davinci-003", 4093);
		CONTEXT_SIZES.put("text-embedding-ada-002", 8191);
		CONTEXT_SIZES.put("text-search-ada-doc-001", 2046);
		CONTEXT_SIZES.put("text-search-ada-query-001", 2046);
		CONTEXT_SIZES.put("text-search-babbage-doc-001", 2046);
		CONTEXT_SIZES.put("text-search-babbage-query-001", 2046);
		CONTEXT_SIZES.put("text-search-curie-doc-001", 2046);
		CONTEXT_SIZES.put("text-search-curie-query-001", 2046);
		CONTEXT_SIZES.put("text-search-davinci-doc-001", 2046);
		CONTEXT_SIZES.put("text-search-davinci-query-001", 2046);
		CONTEXT_SIZES.put("text-similarity-ada-001", 2046);
		CONTEXT_SIZES.put("text-similarity-babbage-001", 2046);
		CONTEXT_SIZES.put("text-similarity-curie-001", 2046);
		CONTEXT_SIZES.put("text-similarity-davinci-001", 2046);
	};

	/**
	 * 
	 * @param model
	 * @return Context size in token for given model, or -1 if the size is unknown.
	 */
	public static int getContextSize(@NonNull String model) {
		return CONTEXT_SIZES.getOrDefault(model, -1);
	}

	/**
	 * Maps each model in corresponding TokenCounter. Lazy loading.
	 */
	private final static Map<String, TokenCounter> COUNTERS = new HashMap<>();

	/**
	 * 
	 * @param model Model for which the TikenCounter should work.
	 * @return Singleton TokenCounter instance suitable for given model.
	 */
	public static TokenCounter getTokenCounter(@NonNull String model) {
		TokenCounter result = COUNTERS.get(model);
		if (result == null) {
			synchronized (COUNTERS) {
				GPT3Tokenizer tokenizer = new GPT3Tokenizer(Encoding.forModel(model));
				ChatFormatDescriptor chatFormat = ChatFormatDescriptor.forModel(model);
				result = new OpenAiTokenCounter(tokenizer, chatFormat);
				COUNTERS.put(model, result);
			}
		}

		return result;
	}
}
