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

package io.github.mzattera.predictivepowers.openai.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.ChatMessage;
import io.github.mzattera.predictivepowers.services.TextCompletion;

/**
 * Test the OpenAI chat API & Service
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class OpenAiChatServiceTest {

	/**
	 * Check completions not affecting history.
	 */
	@Test
	public void test01() {
		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {
			OpenAiChatService cs = ep.getChatService();

			// Personality
			String personality = "You are a smart and nice agent.";
			cs.setPersonality(personality);
			assertEquals(cs.getPersonality(), personality);

			// In completion, we do not consider history, but we consider personality.
			cs.getHistory().add(new ChatMessage(ChatMessage.Role.USER, "test"));
			assertEquals(1, cs.getHistory().size());
			String question = "How high is Mt.Everest?";
			TextCompletion resp = cs.complete(question);
			assertEquals(resp.getFinishReason(), TextCompletion.FinishReason.COMPLETED);
			assertEquals(1, cs.getHistory().size());
			assertEquals(cs.getHistory().get(0).getContent(), "test");
			assertEquals(cs.getDefaultReq().getMessages().size(), 2);
			assertEquals(cs.getDefaultReq().getMessages().get(0).getRole(), ChatMessage.Role.SYSTEM);
			assertEquals(cs.getDefaultReq().getMessages().get(0).getContent(), personality);
			assertEquals(cs.getDefaultReq().getMessages().get(1).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getDefaultReq().getMessages().get(1).getContent(), question);
			assertEquals(cs.getDefaultReq().getMaxTokens(), null);

			cs.clearConversation();
			assertEquals(cs.getHistory().size(), 0);
		} // Close endpoint
	}

	/**
	 * Check chat and history management.
	 */
	@Test
	public void test02() {
		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {
			OpenAiChatService cs = ep.getChatService();

			// Personality, history length and conversation steps limits ////////////

			// Personality
			String personality = "You are a smart and nice agent.";
			cs.setPersonality(personality);
			assertEquals(cs.getPersonality(), personality);

			// Fake history
			for (int i = 0; i < 10; ++i) {
				cs.getHistory().add(new ChatMessage(ChatMessage.Role.USER, "" + i));
			}

			cs.setMaxHistoryLength(3);
			cs.setMaxConversationSteps(2);
			assertEquals(cs.getMaxConversationTokens(),
					Math.max(ep.getModelService().getContextSize(cs.getDefaultReq().getModel()), 2046) * 3 / 4);

			String question = "How high is Mt.Everest?";
			TextCompletion resp = cs.chat(question);
			assertEquals(resp.getFinishReason(), TextCompletion.FinishReason.COMPLETED);
			assertEquals(cs.getHistory().size(), 3);
			assertEquals(cs.getHistory().get(0).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getHistory().get(0).getContent(), "" + 9);
			assertEquals(cs.getHistory().get(1).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getHistory().get(1).getContent(), question);
			assertEquals(cs.getHistory().get(2).getRole(), ChatMessage.Role.BOT);
			assertEquals(cs.getHistory().get(2).getContent(), resp.getText());
			assertEquals(4, cs.getDefaultReq().getMessages().size());
			assertEquals(cs.getDefaultReq().getMessages().get(0).getRole(), ChatMessage.Role.SYSTEM);
			assertEquals(cs.getDefaultReq().getMessages().get(0).getContent(), personality);
			assertEquals(cs.getDefaultReq().getMessages().get(1).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getDefaultReq().getMessages().get(1).getContent(), "" + 8);
			assertEquals(cs.getDefaultReq().getMessages().get(2).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getDefaultReq().getMessages().get(2).getContent(), "" + 9);
			assertEquals(cs.getDefaultReq().getMessages().get(3).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getDefaultReq().getMessages().get(3).getContent(), question);
			assertEquals(cs.getDefaultReq().getMaxTokens(), null);

			// NO personality, history length and conversation steps limits ////////////
			// Also testing maxTokens

			// Fake history
			cs.clearConversation();
			for (int i = 0; i < 10; ++i) {
				cs.getHistory().add(new ChatMessage(ChatMessage.Role.USER, "" + i));
			}
			cs.setPersonality(null);
			cs.getDefaultReq().setMaxTokens(100);

			resp = cs.chat(question);
			assertEquals(resp.getFinishReason(), TextCompletion.FinishReason.COMPLETED);
			assertEquals(cs.getHistory().size(), 3);
			assertEquals(cs.getHistory().get(0).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getHistory().get(0).getContent(), "" + 9);
			assertEquals(cs.getHistory().get(1).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getHistory().get(1).getContent(), question);
			assertEquals(cs.getHistory().get(2).getRole(), ChatMessage.Role.BOT);
			assertEquals(cs.getHistory().get(2).getContent(), resp.getText());
			assertEquals(3, cs.getDefaultReq().getMessages().size());
			assertEquals(cs.getDefaultReq().getMessages().get(0).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getDefaultReq().getMessages().get(0).getContent(), "" + 8);
			assertEquals(cs.getDefaultReq().getMessages().get(1).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getDefaultReq().getMessages().get(1).getContent(), "" + 9);
			assertEquals(cs.getDefaultReq().getMessages().get(2).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getDefaultReq().getMessages().get(2).getContent(), question);
			assertEquals(cs.getDefaultReq().getMaxTokens(), 100);

			// Personality, history length and conversation tokens limits ////////////

			// Fake history
			cs.clearConversation();
			for (int i = 0; i < 10; ++i) {
				cs.getHistory().add(new ChatMessage(ChatMessage.Role.USER, "" + i));
			}
			cs.setPersonality(personality);
			cs.getDefaultReq().setMaxTokens(null);
			cs.setMaxHistoryLength(3);
			cs.setMaxConversationSteps(9999);
			cs.setMaxConversationTokens(1);

			resp = cs.chat(question);
			assertEquals(resp.getFinishReason(), TextCompletion.FinishReason.COMPLETED);
			assertEquals(cs.getHistory().size(), 3);
			assertEquals(cs.getHistory().get(0).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getHistory().get(0).getContent(), "" + 9);
			assertEquals(cs.getHistory().get(1).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getHistory().get(1).getContent(), question);
			assertEquals(cs.getHistory().get(2).getRole(), ChatMessage.Role.BOT);
			assertEquals(cs.getHistory().get(2).getContent(), resp.getText());
			assertEquals(2, cs.getDefaultReq().getMessages().size());
			assertEquals(cs.getDefaultReq().getMessages().get(0).getRole(), ChatMessage.Role.SYSTEM);
			assertEquals(cs.getDefaultReq().getMessages().get(0).getContent(), personality);
			assertEquals(cs.getDefaultReq().getMessages().get(1).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getDefaultReq().getMessages().get(1).getContent(), question);
			assertEquals(cs.getDefaultReq().getMaxTokens(), null);

			// NO personality, history length and conversation tokens limits ////////////

			// Fake history
			cs.clearConversation();
			for (int i = 0; i < 10; ++i) {
				cs.getHistory().add(new ChatMessage(ChatMessage.Role.USER, "" + i));
			}
			cs.setPersonality(null);
			cs.getDefaultReq().setMaxTokens(null);
			cs.setMaxHistoryLength(3);
			cs.setMaxConversationSteps(9999);
			cs.setMaxConversationTokens(1);

			resp = cs.chat(question);
			assertEquals(resp.getFinishReason(), TextCompletion.FinishReason.COMPLETED);
			assertEquals(cs.getHistory().size(), 3);
			assertEquals(cs.getHistory().get(0).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getHistory().get(0).getContent(), "" + 9);
			assertEquals(cs.getHistory().get(1).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getHistory().get(1).getContent(), question);
			assertEquals(cs.getHistory().get(2).getRole(), ChatMessage.Role.BOT);
			assertEquals(cs.getHistory().get(2).getContent(), resp.getText());
			assertEquals(1, cs.getDefaultReq().getMessages().size());
			assertEquals(cs.getDefaultReq().getMessages().get(0).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getDefaultReq().getMessages().get(0).getContent(), question);
			assertEquals(cs.getDefaultReq().getMaxTokens(), null);
		} // Close endpoint
	}

	/**
	 * Check chat and history management with exception.
	 */
	@Test
	public void test03() {
		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {
			OpenAiChatService cs = ep.getChatService();

			// Personality, history length and conversation steps limits ////////////

			// Personality
			cs.setPersonality(null);

			// Fake history
			for (int i = 0; i < 10; ++i) {
				cs.getHistory().add(new ChatMessage(ChatMessage.Role.USER, "" + i));
			}

			cs.setMaxHistoryLength(1);
			cs.getDefaultReq().setMaxTokens(999_999);

			String question = "How high is Mt.Everest?";

			try {
				cs.chat(question);
			} catch (Exception e) {
			}

			assertEquals(cs.getHistory().size(), 1);
			assertEquals(cs.getHistory().get(0).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getHistory().get(0).getContent(), "" + 9);
		} // Close endpoint
	}
}
