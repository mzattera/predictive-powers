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

package io.github.mzattera.predictivepowers.openai.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.TestConfiguration;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsRequest;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatMessage;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatMessage.Role;

public class OpenAiExceptionTest {

	@DisplayName("Checks OpenAIException properly created when exceeding context size")
	@Test
	public void testCompletion() {

		if (!TestConfiguration.TEST_OPENAI_SERVICES)
			return;

		try (OpenAiClient cli = new OpenAiClient()) {

			CompletionsRequest req = CompletionsRequest.builder().model("davinci-002").maxTokens(20_000).prompt("Ciao!")
					.build();
			OpenAiException e = assertThrows(OpenAiException.class, () -> cli.createCompletion(req));
			assertEquals(400, e.code());
			assertTrue(e.isContextLengthExceeded());
			assertEquals(3, e.getPromptLength());
			assertEquals(20000, e.getCompletionLength());
			assertEquals(20003, e.getRequestLength());
			assertEquals(16385, e.getMaxContextLength());
		}
	}

	@DisplayName("Checks OpenAIException properly created when exceeding context size")
	@Test
	public void testChat() {

		if (!TestConfiguration.TEST_OPENAI_SERVICES)
			return;

		try (OpenAiClient cli = new OpenAiClient()) {

			OpenAiChatMessage msg = new OpenAiChatMessage(Role.USER, "Ciao!");
			List<OpenAiChatMessage> msgs = new ArrayList<>();
			msgs.add(msg);
			ChatCompletionsRequest req = ChatCompletionsRequest.builder().model("gpt-3.5-turbo")
					.maxCompletionTokens(10_000).messages(msgs).build();
			OpenAiException e = assertThrows(OpenAiException.class, () -> cli.createChatCompletion(req));
			assertEquals(400, e.code());
			assertTrue(e.isContextLengthExceeded());
			assertEquals(10000, e.getRequestLength());
			assertEquals(4096, e.getCompletionLength());
		}
	}
}
