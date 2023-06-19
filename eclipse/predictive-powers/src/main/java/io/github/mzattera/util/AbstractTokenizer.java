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

import java.util.List;

import io.github.mzattera.predictivepowers.services.ChatMessage;
import io.github.mzattera.predictivepowers.services.ModelService.Tokenizer;
import lombok.NonNull;

/**
 * Tokenizer that has a default implementation of {@link #count(ChatMessage)}
 * and {@link #count(List)}.
 * 
 * The default implementation simply put all message in a single list of texts,
 * with the role used as prefix.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public abstract class AbstractTokenizer implements Tokenizer {

	@Override
	public int count(@NonNull ChatMessage msg) {
		return count(msg.getRole().toString() + ": " + msg.getContent());
	}

	@Override
	public int count(@NonNull List<ChatMessage> msgs) {
		StringBuilder sb = new StringBuilder();
		for (ChatMessage m : msgs) {
			if (sb.length() > 0)
				sb.append('\n');
			sb.append(m.getRole().toString() + ": " + m.getContent());
		}
		return count(sb.toString());
	}
}
