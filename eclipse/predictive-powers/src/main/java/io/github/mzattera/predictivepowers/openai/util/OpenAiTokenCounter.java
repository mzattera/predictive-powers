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

import java.util.ArrayList;
import java.util.List;

import io.github.mzattera.predictivepowers.TokenCounter;
import io.github.mzattera.predictivepowers.openai.util.tokeniser.ChatFormatDescriptor;
import io.github.mzattera.predictivepowers.openai.util.tokeniser.GPT3Tokenizer;
import io.github.mzattera.predictivepowers.openai.util.tokeniser.TokenCount;
import io.github.mzattera.predictivepowers.services.ChatMessage;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * This class calculates number of tokens for OpenAI models.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@RequiredArgsConstructor
public class OpenAiTokenCounter implements TokenCounter {

	private final GPT3Tokenizer tokenizer;
	private final ChatFormatDescriptor chatFormat;

	@Override
	public int count(@NonNull String text) {
		return TokenCount.fromString(text, tokenizer);
	}

	@Override
	public int count(@NonNull ChatMessage msg) {
		List<ChatMessage> l = new ArrayList<>();
		l.add(msg);
		return count(l);
	}

	@Override
	public int count(@NonNull List<ChatMessage> msgs) {
		return TokenCount.fromMessages(msgs, tokenizer, chatFormat);
	}
}
