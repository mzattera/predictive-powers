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
 */package io.github.mzattera.predictivepowers.openai.client.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.util.ModelUtil;
import io.github.mzattera.predictivepowers.services.ChatMessage;

class ChatCompletionsTest {

	@Test
	void test01() {
		OpenAiEndpoint oai = OpenAiEndpoint.getInstance();
		String model = "gpt-3.5-turbo";
		String prompt = "How high is Mt. Everest?";
		ChatCompletionsRequest cr = new ChatCompletionsRequest();

		cr.setModel(model);
		cr.getMessages().add(ChatMessage.builder().role("user").content(prompt).build());
		cr.setMaxTokens(ModelUtil.getContextSize(model) - 15);
		cr.setStop(new ArrayList<>());
		cr.getStop().add("feet");

		ChatCompletionsResponse resp = oai.getClient().createChatCompletion(cr);

		assertEquals(resp.getChoices().size(), 1);
		assertEquals(resp.getChoices().get(0).getFinishReason(), "stop");
		assertTrue(resp.getChoices().get(0).getMessage().getContent().contains("848")
				|| resp.getChoices().get(0).getMessage().getContent().contains("029 "));
		assertTrue(resp.getChoices().get(0).getMessage().getContent().endsWith("029 "));
	}
}
