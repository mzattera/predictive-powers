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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.TestConfiguration;
import io.github.mzattera.predictivepowers.huggingface.client.HuggingFaceEndpoint;
import io.github.mzattera.predictivepowers.services.messages.ChatCompletion;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage.Author;
import io.github.mzattera.predictivepowers.services.messages.FinishReason;

/**
 * Test Hugging Face chat API & Service
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class HuggingFaceChatServiceTest {

	@DisplayName("Check completions not affecting history.")
	@Test
	public void test01() {

		if (!TestConfiguration.TEST_HF_SERVICES)
			return;

		try (HuggingFaceEndpoint ep = new HuggingFaceEndpoint(); HuggingFaceChatService cs = ep.getChatService();) {

			// Personality
			String personality = "You are a smart and nice agent.";
			cs.setPersonality(personality);
			assertEquals(cs.getPersonality(), personality);

			// In completion, we do not consider history, but we consider personality.
			String question = "How high is Mt.Everest?";
			ChatCompletion resp = cs.complete(question);
			assertEquals(FinishReason.COMPLETED, resp.getFinishReason());
			assertEquals(0, cs.getHistory().size());
			assertEquals(0, cs.getDefaultReq().getInputs().getPastUserInputs().size());
			assertEquals(0, cs.getDefaultReq().getInputs().getGeneratedResponses().size());
			assertEquals(cs.getDefaultReq().getInputs().getText(), question);
		} // Close endpoint
	}

	@DisplayName("Check chat and history management.")
	@Test
	public void testHistory() {

		if (!TestConfiguration.TEST_HF_SERVICES)
			return;

		try (HuggingFaceEndpoint ep = new HuggingFaceEndpoint(); HuggingFaceChatService cs = ep.getChatService();) {
			
			// Personality, history length and conversation steps limits ////////////

			// Personality
			String personality = "You are a smart and nice agent.";
			cs.setPersonality(personality);
			assertEquals(cs.getPersonality(), personality);

			cs.setMaxHistoryLength(4);
			cs.setMaxConversationSteps(2);

			assertEquals(4, cs.getMaxHistoryLength());
			assertEquals(2, cs.getMaxConversationSteps());
			assertEquals(Integer.MAX_VALUE, cs.getMaxConversationTokens());

			String question = "How high is Mt.Everest?";
			ChatCompletion resp = cs.chat(question);
			assertEquals(resp.getFinishReason(), FinishReason.COMPLETED);
			assertEquals(2, cs.getHistory().size());
			assertEquals(cs.getHistory().get(0).getAuthor(), Author.USER);
			assertEquals(cs.getHistory().get(0).getContent(), question);
			assertEquals(cs.getHistory().get(1).getAuthor(), Author.BOT);
			assertEquals(cs.getHistory().get(1).getContent(), resp.getText());
			assertEquals(0, cs.getDefaultReq().getInputs().getPastUserInputs().size());
			assertEquals(0, cs.getDefaultReq().getInputs().getGeneratedResponses().size());
			assertEquals(cs.getDefaultReq().getInputs().getText(), question);

			// NO personality, history length and conversation steps limits ////////////
			// Also testing maxTokens

			// Fake history
			cs.setPersonality(null);
			cs.setMaxNewTokens(100);
			cs.setMaxHistoryLength(4);
			cs.setMaxConversationSteps(2);

			resp = cs.chat(question);
			assertEquals(resp.getFinishReason(), FinishReason.COMPLETED);
			assertEquals(2, cs.getHistory().size(), 4);
			assertEquals(cs.getHistory().get(0).getAuthor(), Author.USER);
			assertEquals(cs.getHistory().get(0).getContent(), question);
			assertEquals(cs.getHistory().get(1).getAuthor(), Author.BOT);
			assertEquals(cs.getHistory().get(2).getAuthor(), Author.USER);
			assertEquals(cs.getHistory().get(2).getContent(), question);
			assertEquals(cs.getHistory().get(3).getAuthor(), Author.BOT);
			assertEquals(cs.getHistory().get(3).getContent(), resp.getText());
			assertEquals(1, cs.getDefaultReq().getInputs().getPastUserInputs().size());
			assertEquals(question, cs.getDefaultReq().getInputs().getPastUserInputs().get(0));
			assertEquals(1, cs.getDefaultReq().getInputs().getGeneratedResponses().size());
			assertEquals(cs.getDefaultReq().getInputs().getText(), question);
			assertEquals(cs.getMaxNewTokens(), 100);

			// Personality, history length and conversation tokens limits ////////////

			// Fake history
			cs.setPersonality(personality);
			cs.setMaxNewTokens(null);
			cs.setMaxHistoryLength(4);
			cs.setMaxConversationSteps(9999);
			cs.setMaxConversationTokens(1);

			assertThrows(IllegalArgumentException.class, () -> cs.chat(question));

			// Completion
			cs.setPersonality(personality);
			cs.setMaxNewTokens(null);
			cs.setMaxHistoryLength(9999);
			cs.setMaxConversationSteps(9999);
			cs.setMaxConversationTokens(9999);
			cs.clearConversation();
			assertEquals(0, cs.getHistory().size());
			resp = cs.complete(question);
			assertEquals(0, cs.getHistory().size());

		} // Close endpoint
	}

@DisplayName("Getters and setters.")
	@Test
	public void testGettersSetters() {
	

	if (!TestConfiguration.TEST_HF_SERVICES)
		return;

	try (HuggingFaceEndpoint ep = new HuggingFaceEndpoint(); HuggingFaceChatService s = ep.getChatService();) {

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
