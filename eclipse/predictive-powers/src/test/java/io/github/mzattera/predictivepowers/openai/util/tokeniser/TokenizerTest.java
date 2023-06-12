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

package io.github.mzattera.predictivepowers.openai.util.tokeniser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.services.ChatMessage;

public class TokenizerTest {

	private final static String TEXT = "banana are great things to eat, really!";

	@Test
	void test01() {
		GPT3Tokenizer tokenizer = new GPT3Tokenizer(Encoding.forModel("gpt-3.5-turbo"));
		assertEquals(9, TokenCount.fromString(TEXT, tokenizer));

		tokenizer = new GPT3Tokenizer(Encoding.forModel("text-davinci-003"));
		assertEquals(10, TokenCount.fromString(TEXT, tokenizer));
	}

	@Test
	void test02() {
		GPT3Tokenizer tokenizer = new GPT3Tokenizer(Encoding.CL100K_BASE);
		List<Integer> tokens = tokenizer.encode(TEXT);
		String text = tokenizer.decode(tokens);
		assertEquals(TEXT, text);

		tokenizer = new GPT3Tokenizer(Encoding.P50K_BASE);
		tokens = tokenizer.encode(TEXT);
		text = tokenizer.decode(tokens);
		assertEquals(TEXT, text);

		tokenizer = new GPT3Tokenizer(Encoding.P50K_EDIT);
		tokens = tokenizer.encode(TEXT);
		text = tokenizer.decode(tokens);
		assertEquals(TEXT, text);

		tokenizer = new GPT3Tokenizer(Encoding.R50K_BASE);
		tokens = tokenizer.encode(TEXT);
		text = tokenizer.decode(tokens);
		assertEquals(TEXT, text);
	}

	@Test
	void test03() {
		List<ChatMessage> l = new ArrayList<>();
		l.add(new ChatMessage("system", "You are a usefl assistant"));
		l.add(new ChatMessage("user", "Hello"));
		l.add(new ChatMessage("assistant", "Hi, how cna I help?"));

		GPT3Tokenizer tokenizer = new GPT3Tokenizer(Encoding.forModel("gpt-3.5-turbo"));
		assertEquals(33, TokenCount.fromMessages(l, tokenizer, ChatFormatDescriptor.forModel("gpt-3.5-turbo")));
	}

	@Test
	void fromLinesJoined_gives_total_token_count_including_newlines() {
		GPT3Tokenizer tokenizer = new GPT3Tokenizer(Encoding.CL100K_BASE);
		assertEquals(0, TokenCount.fromLinesJoined(List.of(), tokenizer));
		assertEquals(1, TokenCount.fromLinesJoined(List.of("1"), tokenizer));
		assertEquals(3, TokenCount.fromLinesJoined(List.of("1", "2"), tokenizer));
		assertEquals(5, TokenCount.fromLinesJoined(List.of("1", "2", "3"), tokenizer));
	}
}
