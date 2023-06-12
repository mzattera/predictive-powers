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

package io.github.mzattera.predictivepowers;

import java.util.List;

import io.github.mzattera.predictivepowers.services.ChatMessage;
import lombok.NonNull;

/**
 * This is a TokenCounter that counts characters (that is 1 char = 1 token).
 * 
 * This is useful when you do not have a specific tokenizer for a model or you
 * want to count length by chars.
 * 
 * @author mzatt
 *
 */
public final class CharCounter implements TokenCounter {

	private CharCounter() {
	}

	private final static CharCounter instance = new CharCounter();

	/**
	 * 
	 * @return Singleton fo this class.
	 */
	public static CharCounter getInstance() {
		return instance;
	}

	@Override
	public int count(@NonNull String text) {
		return text.length();
	}

	@Override
	public int count(@NonNull ChatMessage msg) {
		return count(msg.getRole()) + count(msg.getContent()) + 1;
	}

	@Override
	public int count(@NonNull List<ChatMessage> msgs) {
		int result = 0;
		for (ChatMessage m : msgs)
			result += count(m);
		return result + msgs.size();
	}
}
