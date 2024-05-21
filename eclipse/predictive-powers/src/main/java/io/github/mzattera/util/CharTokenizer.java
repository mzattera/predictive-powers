/*
 * Copyright 2023-2024 Massimiliano "Maxi" Zattera
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

import io.github.mzattera.predictivepowers.services.ModelService.Tokenizer;
import lombok.NonNull;

/**
 * This is a {@link Tokenizer} that counts characters (that is 1 char = 1
 * token).
 * 
 * This is useful when you do not have a specific tokenizer for a model or you
 * want to count length by chars.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class CharTokenizer implements Tokenizer {

	private CharTokenizer() {
	}

	private final static CharTokenizer instance = new CharTokenizer();

	/**
	 * 
	 * @return Singleton for this class.
	 */
	public static CharTokenizer getInstance() {
		return instance;
	}

	@Override
	public int count(@NonNull String text) {
		return text.length();
	}
}
