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

import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService;
import io.github.mzattera.predictivepowers.services.ChatMessage;
import io.github.mzattera.predictivepowers.services.ModelService.Tokenizer;
import xyz.felh.openai.jtokkit.api.Encoding;
import xyz.felh.openai.jtokkit.utils.TikTokenUtils;

public class JtokkitTest {

	private final static OpenAiModelService svc = (new OpenAiEndpoint()).getModelService();

	private final static String TEXT = "banana are great things to eat, really!";

	@Test
	void test01() {
		Encoding tokenizer = TikTokenUtils.getEncoding("gpt-3.5-turbo");
		assertEquals(9, tokenizer.countTokens(TEXT));

		tokenizer = TikTokenUtils.getEncoding("text-davinci-003");
		assertEquals(10, tokenizer.countTokens(TEXT));
	}

	@Test
	void test02() {
		Encoding tokenizer = TikTokenUtils.getEncoding("gpt-3.5-turbo"); // CL100K_BASE
		List<Integer> tokens = tokenizer.encode(TEXT);
		String text = tokenizer.decode(tokens);
		assertEquals(TEXT, text);

		tokenizer = TikTokenUtils.getEncoding("text-davinci-003"); // P50K_BASE
		tokens = tokenizer.encode(TEXT);
		text = tokenizer.decode(tokens);
		assertEquals(TEXT, text);

//		tokenizer = new Encoding(Encoding.P50K_EDIT);
//		tokens = tokenizer.encode(TEXT);
//		text = tokenizer.decode(tokens);
//		assertEquals(TEXT, text);

		tokenizer = TikTokenUtils.getEncoding("davinci"); // R50K_BASE
		tokens = tokenizer.encode(TEXT);
		text = tokenizer.decode(tokens);
		assertEquals(TEXT, text);
	}

	@Test
	void test03() {
		List<ChatMessage> l = new ArrayList<>();
		l.add(new ChatMessage(ChatMessage.Role.SYSTEM, "You are a usefl assistant"));
		l.add(new ChatMessage(ChatMessage.Role.USER, "Hello"));
		l.add(new ChatMessage(ChatMessage.Role.BOT, "Hi, how cna I help?"));

		Tokenizer tokenizer = svc.getTokenizer("gpt-3.5-turbo");
		assertEquals(33, tokenizer.count(l));
	}
}
