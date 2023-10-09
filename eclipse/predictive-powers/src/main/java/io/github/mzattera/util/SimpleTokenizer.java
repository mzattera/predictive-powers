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

import io.github.mzattera.predictivepowers.services.ModelService.Tokenizer;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * This is a {@link Tokenizer} that uses a fixed token-to-char ratio.
 * 
 * This is useful when you do not have a specific tokenizer for a model and you
 * are happy with approximate results.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@RequiredArgsConstructor
public class SimpleTokenizer extends AbstractTokenizer {

	/**
	 * How many characters in a token (on average).
	 */
	@Getter
	private final double ratio;
	
	@Override
	public int count(@NonNull String text) {
		return (int) (text.length() / ratio + 0.5);
	}
}
