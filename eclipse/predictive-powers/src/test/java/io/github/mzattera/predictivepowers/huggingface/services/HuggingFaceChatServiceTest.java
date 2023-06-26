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

package io.github.mzattera.predictivepowers.huggingface.services;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.huggingface.endpoint.HuggingFaceEndpoint;
import io.github.mzattera.predictivepowers.services.ChatMessage;
import io.github.mzattera.predictivepowers.services.ChatMessage.Role;
import io.github.mzattera.predictivepowers.services.TextCompletion;
import io.github.mzattera.predictivepowers.services.TextCompletion.FinishReason;

/**
 * Test Hugging Face chat API & Service
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class HuggingFaceChatServiceTest {

	/**
	 * Check completions not affecting history.
	 */
	@Test
	public void test01() {
		try (HuggingFaceEndpoint ep = new HuggingFaceEndpoint()) {
			HuggingFaceChatService cs = ep.getChatService();

			// Personality
			String personality = "You are a smart and nice agent.";
			cs.setPersonality(personality);
			assertEquals(cs.getPersonality(), personality);

			// In completion, we do not consider history, but we consider personality.
			cs.getHistory().add(new ChatMessage(ChatMessage.Role.USER, "test"));
			assertEquals(1, cs.getHistory().size());
			String question = "How high is Mt.Everest?";
			TextCompletion resp = cs.complete(question);
			assertEquals(TextCompletion.FinishReason.OK, resp.getFinishReason());
			assertEquals(1, cs.getHistory().size());
			assertEquals(cs.getHistory().get(0).getContent(), "test");
			assertEquals(0, cs.getDefaultReq().getInputs().getPastUserInputs().size());
			assertEquals(0, cs.getDefaultReq().getInputs().getGeneratedResponses().size());
			assertEquals(cs.getDefaultReq().getInputs().getText(), question);

			cs.clearConversation();
			assertEquals(cs.getHistory().size(), 0);
		} // Close endpoint
	}

	/**
	 * Check chat and history management.
	 */
	@Test
	public void test02() {
		try (HuggingFaceEndpoint ep = new HuggingFaceEndpoint()) {
			HuggingFaceChatService cs = ep.getChatService();

			// Personality, history length and conversation steps limits ////////////

			// Personality
			String personality = "You are a smart and nice agent.";
			cs.setPersonality(personality);
			assertEquals(cs.getPersonality(), personality);

			// Fake history
			for (int i = 0; i < 10; ++i) {
				cs.getHistory().add(new ChatMessage(ChatMessage.Role.USER, "user_" + i));
				cs.getHistory().add(new ChatMessage(ChatMessage.Role.BOT, "bot_" + i));
			}

			cs.setMaxHistoryLength(4);
			cs.setMaxConversationSteps(2);

			assertEquals(4, cs.getMaxHistoryLength());
			assertEquals(2, cs.getMaxConversationSteps());
			assertEquals(Integer.MAX_VALUE, cs.getMaxConversationTokens());

			String question = "How high is Mt.Everest?";
			TextCompletion resp = cs.chat(question);
			assertEquals(resp.getFinishReason(), FinishReason.OK);
			assertEquals(cs.getHistory().size(), 4);
			assertEquals(cs.getHistory().get(0).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getHistory().get(0).getContent(), "user_" + 9);
			assertEquals(cs.getHistory().get(1).getRole(), ChatMessage.Role.BOT);
			assertEquals(cs.getHistory().get(1).getContent(), "bot_" + 9);
			assertEquals(cs.getHistory().get(2).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getHistory().get(2).getContent(), question);
			assertEquals(cs.getHistory().get(3).getRole(), ChatMessage.Role.BOT);
			assertEquals(cs.getHistory().get(3).getContent(), resp.getText());
			assertEquals(cs.getDefaultReq().getInputs().getPastUserInputs().size(), 1);
			assertEquals(cs.getDefaultReq().getInputs().getPastUserInputs().get(0), "user_" + 9);
			assertEquals(cs.getDefaultReq().getInputs().getGeneratedResponses().size(), 1);
			assertEquals(cs.getDefaultReq().getInputs().getGeneratedResponses().get(0), "bot_" + 9);
			assertEquals(cs.getDefaultReq().getInputs().getText(), question);

			// NO personality, history length and conversation steps limits ////////////
			// Also testing maxTokens

			// Fake history
			cs.clearConversation();
			for (int i = 0; i < 10; ++i) {
				cs.getHistory().add(new ChatMessage(ChatMessage.Role.USER, "user_" + i));
				cs.getHistory().add(new ChatMessage(ChatMessage.Role.BOT, "bot_" + i));
			}
			cs.setPersonality(null);
			cs.setMaxNewTokens(100);

			resp = cs.chat(question);
			assertEquals(resp.getFinishReason(), FinishReason.OK);
			assertEquals(cs.getHistory().size(), 4);
			assertEquals(cs.getHistory().get(0).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getHistory().get(0).getContent(), "user_" + 9);
			assertEquals(cs.getHistory().get(1).getRole(), ChatMessage.Role.BOT);
			assertEquals(cs.getHistory().get(1).getContent(), "bot_" + 9);
			assertEquals(cs.getHistory().get(2).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getHistory().get(2).getContent(), question);
			assertEquals(cs.getHistory().get(3).getRole(), ChatMessage.Role.BOT);
			assertEquals(cs.getHistory().get(3).getContent(), resp.getText());
			assertEquals(cs.getDefaultReq().getInputs().getPastUserInputs().size(), 1);
			assertEquals(cs.getDefaultReq().getInputs().getPastUserInputs().get(0), "user_" + 9);
			assertEquals(cs.getDefaultReq().getInputs().getGeneratedResponses().size(), 1);
			assertEquals(cs.getDefaultReq().getInputs().getGeneratedResponses().get(0), "bot_" + 9);
			assertEquals(cs.getDefaultReq().getInputs().getText(), question);
			assertEquals(cs.getMaxNewTokens(), 100);

			// Personality, history length and conversation tokens limits ////////////

			// Fake history
			cs.clearConversation();
			for (int i = 0; i < 10; ++i) {
				cs.getHistory().add(new ChatMessage(ChatMessage.Role.USER, "user_" + i));
				cs.getHistory().add(new ChatMessage(ChatMessage.Role.BOT, "bot_" + i));
			}
			cs.setPersonality(personality);
			cs.setMaxNewTokens(null);
			cs.setMaxHistoryLength(4);
			cs.setMaxConversationSteps(9999);
			cs.setMaxConversationTokens(1);

			resp = cs.chat(question);
			assertEquals(resp.getFinishReason(), FinishReason.OK);
			assertEquals(cs.getHistory().size(), 4);
			assertEquals(cs.getHistory().get(0).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getHistory().get(0).getContent(), "user_" + 9);
			assertEquals(cs.getHistory().get(1).getRole(), ChatMessage.Role.BOT);
			assertEquals(cs.getHistory().get(1).getContent(), "bot_" + 9);
			assertEquals(cs.getHistory().get(2).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getHistory().get(2).getContent(), question);
			assertEquals(cs.getHistory().get(3).getRole(), ChatMessage.Role.BOT);
			assertEquals(cs.getHistory().get(3).getContent(), resp.getText());
			assertEquals(cs.getDefaultReq().getInputs().getPastUserInputs().size(), 0);
			assertEquals(cs.getDefaultReq().getInputs().getGeneratedResponses().size(), 0);
			assertEquals(cs.getDefaultReq().getInputs().getText(), question);
			assertEquals(cs.getMaxNewTokens(), null);

			// Completion

			cs.setPersonality(personality);
			cs.setMaxNewTokens(null);
			cs.setMaxHistoryLength(4);
			cs.setMaxConversationSteps(9999);
			cs.setMaxConversationTokens(1);

			List<ChatMessage> l = new ArrayList<>();
			l.add(new ChatMessage(Role.USER, question));
			resp = cs.complete(l);
			assertEquals(resp.getFinishReason(), FinishReason.OK);
			assertEquals(cs.getHistory().size(), 4);
			assertEquals(cs.getHistory().get(0).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getHistory().get(0).getContent(), "user_" + 9);
			assertEquals(cs.getHistory().get(1).getRole(), ChatMessage.Role.BOT);
			assertEquals(cs.getHistory().get(1).getContent(), "bot_" + 9);
			assertEquals(cs.getHistory().get(2).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getHistory().get(2).getContent(), question);
			assertEquals(cs.getHistory().get(3).getRole(), ChatMessage.Role.BOT);
			assertEquals(cs.getHistory().get(3).getContent(), resp.getText());
			assertEquals(cs.getDefaultReq().getInputs().getPastUserInputs().size(), 0);
			assertEquals(cs.getDefaultReq().getInputs().getGeneratedResponses().size(), 0);
			assertEquals(cs.getDefaultReq().getInputs().getText(), question);
			assertEquals(cs.getMaxNewTokens(), null);
		} // Close endpoint
	}

	/**
	 * Test special HF way of building conversation hstory.
	 */
	@Test
	public void test05() {
		try (HuggingFaceEndpoint ep = new HuggingFaceEndpoint()) {
			HuggingFaceChatService cs = ep.getChatService();
			assertEquals(Integer.MAX_VALUE, cs.getMaxConversationTokens());
			String question = "How high is Mt.Everest?";

			// Double chat message per role 
			
			// Fake history
			for (int i = 0; i < 10; ++i) {
				cs.getHistory().add(new ChatMessage(ChatMessage.Role.USER, "U1_" + i));
				cs.getHistory().add(new ChatMessage(ChatMessage.Role.USER, "U2_" + i));
				cs.getHistory().add(new ChatMessage(ChatMessage.Role.BOT, "B1_" + i));
				cs.getHistory().add(new ChatMessage(ChatMessage.Role.BOT, "B2_" + i));
			}

			cs.setMaxHistoryLength(4);
			cs.setMaxConversationSteps(4);

			TextCompletion resp = cs.chat(question);
			assertEquals(resp.getFinishReason(), FinishReason.OK);
			assertEquals(cs.getHistory().size(), 4);
			assertEquals(cs.getHistory().get(0).getRole(), ChatMessage.Role.BOT);
			assertEquals(cs.getHistory().get(0).getContent(), "B1_" + 9);
			assertEquals(cs.getHistory().get(1).getRole(), ChatMessage.Role.BOT);
			assertEquals(cs.getHistory().get(1).getContent(), "B2_" + 9);
			assertEquals(cs.getHistory().get(2).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getHistory().get(2).getContent(), question);
			assertEquals(cs.getHistory().get(3).getRole(), ChatMessage.Role.BOT);
			assertEquals(cs.getHistory().get(3).getContent(), resp.getText());
			assertEquals(cs.getDefaultReq().getInputs().getPastUserInputs().size(), 1);
			assertEquals(cs.getDefaultReq().getInputs().getPastUserInputs().get(0), "U1_9\nU2_9");
			assertEquals(cs.getDefaultReq().getInputs().getGeneratedResponses().size(), 1);
			assertEquals(cs.getDefaultReq().getInputs().getGeneratedResponses().get(0), "B1_9\nB2_9");
			assertEquals(cs.getDefaultReq().getInputs().getText(), question);

			// Double chat message per role, very short conversation steps 
			
			// Fake history
			for (int i = 0; i < 10; ++i) {
				cs.getHistory().add(new ChatMessage(ChatMessage.Role.USER, "U1_" + i));
				cs.getHistory().add(new ChatMessage(ChatMessage.Role.USER, "U2_" + i));
				cs.getHistory().add(new ChatMessage(ChatMessage.Role.BOT, "B1_" + i));
				cs.getHistory().add(new ChatMessage(ChatMessage.Role.BOT, "B2_" + i));
			}

			cs.setMaxHistoryLength(4);
			cs.setMaxConversationSteps(2); // Should consider only last 2 bot message, which are ingored

			resp = cs.chat(question);
			assertEquals(resp.getFinishReason(), FinishReason.OK);
			assertEquals(cs.getHistory().size(), 4);
			assertEquals(cs.getHistory().get(0).getRole(), ChatMessage.Role.BOT);
			assertEquals(cs.getHistory().get(0).getContent(), "B1_" + 9);
			assertEquals(cs.getHistory().get(1).getRole(), ChatMessage.Role.BOT);
			assertEquals(cs.getHistory().get(1).getContent(), "B2_" + 9);
			assertEquals(cs.getHistory().get(2).getRole(), ChatMessage.Role.USER);
			assertEquals(cs.getHistory().get(2).getContent(), question);
			assertEquals(cs.getHistory().get(3).getRole(), ChatMessage.Role.BOT);
			assertEquals(cs.getHistory().get(3).getContent(), resp.getText());
			assertEquals(cs.getDefaultReq().getInputs().getPastUserInputs().size(), 0);
			assertEquals(cs.getDefaultReq().getInputs().getGeneratedResponses().size(), 0);
			assertEquals(cs.getDefaultReq().getInputs().getText(), question);

			// Double chat message per role, starting with SYSTEM AND BOT
			
			// Fake history
			cs.getHistory().add(new ChatMessage(ChatMessage.Role.SYSTEM, "Should be ignored"));
			cs.getHistory().add(new ChatMessage(ChatMessage.Role.BOT, "Should be ignored"));
			for (int i = 0; i < 1; ++i) {
				cs.getHistory().add(new ChatMessage(ChatMessage.Role.USER, "U1_" + i));
				cs.getHistory().add(new ChatMessage(ChatMessage.Role.USER, "U2_" + i));
				cs.getHistory().add(new ChatMessage(ChatMessage.Role.BOT, "B1_" + i));
				cs.getHistory().add(new ChatMessage(ChatMessage.Role.BOT, "B2_" + i));
			}

			cs.setMaxHistoryLength(6);
			cs.setMaxConversationSteps(6);

			resp = cs.chat(question);
			assertEquals(resp.getFinishReason(), FinishReason.OK);
			assertEquals(cs.getDefaultReq().getInputs().getPastUserInputs().size(), 1);
			assertEquals(cs.getDefaultReq().getInputs().getPastUserInputs().get(0), "U1_0\nU2_0");
			assertEquals(cs.getDefaultReq().getInputs().getGeneratedResponses().size(), 1);
			assertEquals(cs.getDefaultReq().getInputs().getGeneratedResponses().get(0), "B1_0\nB2_0");
			assertEquals(cs.getDefaultReq().getInputs().getText(), question);

			// Empty list
			resp = cs.complete(new ArrayList<>());
		}
	}

	/**
	 * Check chat and history management with exception.
	 */
	@Test
	public void test03() {
		try (HuggingFaceEndpoint ep = new HuggingFaceEndpoint()) {
			HuggingFaceChatService cs = ep.getChatService();

			// Personality, history length and conversation steps limits ////////////

			// Personality
			cs.setPersonality(null);

			// Fake history
			for (int i = 0; i < 10; ++i) {
				cs.getHistory().add(new ChatMessage(ChatMessage.Role.USER, "user_" + i));
				cs.getHistory().add(new ChatMessage(ChatMessage.Role.BOT, "bot_" + i));
			}

			cs.setMaxHistoryLength(1);
			cs.setMaxConversationTokens(999_999);

			String question = "How high is Mt.Everest?";

			try {
				cs.chat(question);
			} catch (Exception e) {
			}

			assertEquals(cs.getHistory().size(), 1);
			assertEquals(ChatMessage.Role.BOT, cs.getHistory().get(0).getRole());
		} // Close endpoint
	}

	/**
	 * Getters and setters
	 */
	@Test
	public void test04() {
		try (HuggingFaceEndpoint ep = new HuggingFaceEndpoint()) {
			HuggingFaceChatService s = ep.getChatService();
			String m = s.getModel();
			assertNotNull(m);
			s.setModel("pippo");
			assertEquals("pippo", s.getModel());
			s.setModel(m);

			s.setTopK(1);
			assertEquals(1, s.getTopK());
			s.setTopK(null);
			assertNull(s.getTopK());

			s.setTopP(2.0);
			assertEquals(2.0, s.getTopP());
			s.setTopP(null);
			assertNull(s.getTopP());

			s.setTemperature(3.0);
			assertEquals(3.0, s.getTemperature());
			s.setTemperature(null);
			assertNull(s.getTemperature());
			s.setTemperature(1.0);

			s.setMaxNewTokens(4);
			assertEquals(4, s.getMaxNewTokens());
			s.setMaxNewTokens(null);
			assertNull(s.getMaxNewTokens());
			s.setMaxNewTokens(15);
		}
	}

}
