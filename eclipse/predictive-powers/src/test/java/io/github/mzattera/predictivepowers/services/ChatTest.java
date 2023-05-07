/**
 * 
 */
package io.github.mzattera.predictivepowers.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.util.ModelUtil;

/**
 * Test the OpenAi chat API & Service
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class ChatTest {

	/**
	 * Check completions not affecting history.
	 */
	@Test
	public void test01() {
		OpenAiEndpoint ep = OpenAiEndpoint.getInstance();
		ChatService cs = ep.getChatService();

		// Personality
		String personality = "You are a smart and nice agent.";
		cs.setPersonality(personality);
		assertEquals(cs.getPersonality(), personality);

		// In completion, we do not consider history, but we consider personality.
		cs.getHistory().add(new ChatMessage("user", "test"));
		assertEquals(cs.getHistory().size(), 1);
		String question = "How high is Mt.Everest?";
		TextResponse resp = cs.complete(question);
		assertEquals(resp.getFinishReason(), TextResponse.FinishReason.COMPLETED);
		assertEquals(cs.getHistory().size(), 1);
		assertEquals(cs.getHistory().get(0).getContent(), "test");
		assertEquals(cs.getDefaultReq().getMessages().size(), 2);
		assertEquals(cs.getDefaultReq().getMessages().get(0).getRole(), "system");
		assertEquals(cs.getDefaultReq().getMessages().get(0).getContent(), personality);
		assertEquals(cs.getDefaultReq().getMessages().get(1).getRole(), "user");
		assertEquals(cs.getDefaultReq().getMessages().get(1).getContent(), question);
		assertEquals(cs.getDefaultReq().getMaxTokens(), null);

		cs.getHistory().clear();
		assertEquals(cs.getHistory().size(), 0);
	}

	/**
	 * Check chat and history management.
	 */
	// TODO potentially split in many tasks
	@Test
	public void test02() {
		OpenAiEndpoint ep = OpenAiEndpoint.getInstance();
		ChatService cs = ep.getChatService();

		// Personality, history length and conversation steps limits ////////////

		// Personality
		String personality = "You are a smart and nice agent.";
		cs.setPersonality(personality);
		assertEquals(cs.getPersonality(), personality);

		// Fake history
		for (int i = 0; i < 10; ++i) {
			cs.getHistory().add(new ChatMessage("user", "" + i));
		}

		cs.setMaxHistoryLength(3);
		cs.setMaxConversationSteps(2);
		assertEquals(cs.getMaxConversationTokens(),
				Math.max(ModelUtil.getContextSize(cs.getDefaultReq().getModel()), 2046) * 3 / 4);

		String question = "How high is Mt.Everest?";
		TextResponse resp = cs.chat(question);
		assertEquals(resp.getFinishReason(), TextResponse.FinishReason.COMPLETED);
		assertEquals(cs.getHistory().size(), 3);
		assertEquals(cs.getHistory().get(0).getRole(), "user");
		assertEquals(cs.getHistory().get(0).getContent(), "" + 9);
		assertEquals(cs.getHistory().get(1).getRole(), "user");
		assertEquals(cs.getHistory().get(1).getContent(), question);
		assertEquals(cs.getHistory().get(2).getRole(), "assistant");
		assertEquals(cs.getHistory().get(2).getContent(), resp.getText());
		assertEquals(cs.getDefaultReq().getMessages().size(), 3);
		assertEquals(cs.getDefaultReq().getMessages().get(0).getRole(), "system");
		assertEquals(cs.getDefaultReq().getMessages().get(0).getContent(), personality);
		assertEquals(cs.getDefaultReq().getMessages().get(1).getRole(), "user");
		assertEquals(cs.getDefaultReq().getMessages().get(1).getContent(), "" + 9);
		assertEquals(cs.getDefaultReq().getMessages().get(2).getRole(), "user");
		assertEquals(cs.getDefaultReq().getMessages().get(2).getContent(), question);
		assertEquals(cs.getDefaultReq().getMaxTokens(), null);

		// NO personality, history length and conversation steps limits ////////////
		// Also testing maxTokens

		// Fake history
		cs.clearConversation();
		for (int i = 0; i < 10; ++i) {
			cs.getHistory().add(new ChatMessage("user", "" + i));
		}
		cs.setPersonality(null);
		cs.getDefaultReq().setMaxTokens(100);

		resp = cs.chat(question);
		assertEquals(resp.getFinishReason(), TextResponse.FinishReason.COMPLETED);
		assertEquals(cs.getHistory().size(), 3);
		assertEquals(cs.getHistory().get(0).getRole(), "user");
		assertEquals(cs.getHistory().get(0).getContent(), "" + 9);
		assertEquals(cs.getHistory().get(1).getRole(), "user");
		assertEquals(cs.getHistory().get(1).getContent(), question);
		assertEquals(cs.getHistory().get(2).getRole(), "assistant");
		assertEquals(cs.getHistory().get(2).getContent(), resp.getText());
		assertEquals(cs.getDefaultReq().getMessages().size(), 2);
		assertEquals(cs.getDefaultReq().getMessages().get(0).getRole(), "user");
		assertEquals(cs.getDefaultReq().getMessages().get(0).getContent(), "" + 9);
		assertEquals(cs.getDefaultReq().getMessages().get(1).getRole(), "user");
		assertEquals(cs.getDefaultReq().getMessages().get(1).getContent(), question);
		assertEquals(cs.getDefaultReq().getMaxTokens(), 100);

		// Personality, history length and conversation tokens limits ////////////

		// Fake history
		cs.clearConversation();
		for (int i = 0; i < 10; ++i) {
			cs.getHistory().add(new ChatMessage("user", "" + i));
		}
		cs.setPersonality(personality);
		cs.getDefaultReq().setMaxTokens(null);
		cs.setMaxHistoryLength(3);
		cs.setMaxConversationSteps(9999);
		cs.setMaxConversationTokens(1);

		resp = cs.chat(question);
		assertEquals(resp.getFinishReason(), TextResponse.FinishReason.COMPLETED);
		assertEquals(cs.getHistory().size(), 3);
		assertEquals(cs.getHistory().get(0).getRole(), "user");
		assertEquals(cs.getHistory().get(0).getContent(), "" + 9);
		assertEquals(cs.getHistory().get(1).getRole(), "user");
		assertEquals(cs.getHistory().get(1).getContent(), question);
		assertEquals(cs.getHistory().get(2).getRole(), "assistant");
		assertEquals(cs.getHistory().get(2).getContent(), resp.getText());
		assertEquals(cs.getDefaultReq().getMessages().size(), 2);
		assertEquals(cs.getDefaultReq().getMessages().get(0).getRole(), "system");
		assertEquals(cs.getDefaultReq().getMessages().get(0).getContent(), personality);
		assertEquals(cs.getDefaultReq().getMessages().get(1).getRole(), "user");
		assertEquals(cs.getDefaultReq().getMessages().get(1).getContent(), question);
		assertEquals(cs.getDefaultReq().getMaxTokens(), null);

		// NO personality, history length and conversation tokens limits ////////////

		// Fake history
		cs.clearConversation();
		for (int i = 0; i < 10; ++i) {
			cs.getHistory().add(new ChatMessage("user", "" + i));
		}
		cs.setPersonality(null);
		cs.getDefaultReq().setMaxTokens(null);
		cs.setMaxHistoryLength(3);
		cs.setMaxConversationSteps(9999);
		cs.setMaxConversationTokens(1);

		resp = cs.chat(question);
		assertEquals(resp.getFinishReason(), TextResponse.FinishReason.COMPLETED);
		assertEquals(cs.getHistory().size(), 3);
		assertEquals(cs.getHistory().get(0).getRole(), "user");
		assertEquals(cs.getHistory().get(0).getContent(), "" + 9);
		assertEquals(cs.getHistory().get(1).getRole(), "user");
		assertEquals(cs.getHistory().get(1).getContent(), question);
		assertEquals(cs.getHistory().get(2).getRole(), "assistant");
		assertEquals(cs.getHistory().get(2).getContent(), resp.getText());
		assertEquals(cs.getDefaultReq().getMessages().size(), 1);
		assertEquals(cs.getDefaultReq().getMessages().get(0).getRole(), "user");
		assertEquals(cs.getDefaultReq().getMessages().get(0).getContent(), question);
		assertEquals(cs.getDefaultReq().getMaxTokens(), null);
	}

	/**
	 * Check chat and history management with exception.
	 */
	// TODO potentially split in many tasks
	@Test
	public void test03() {
		OpenAiEndpoint ep = OpenAiEndpoint.getInstance();
		ChatService cs = ep.getChatService();

		// Personality, history length and conversation steps limits ////////////

		// Personality
		cs.setPersonality(null);

		// Fake history
		for (int i = 0; i < 10; ++i) {
			cs.getHistory().add(new ChatMessage("user", "" + i));
		}

		cs.setMaxHistoryLength(1);
		cs.getDefaultReq().setMaxTokens(999_999);

		String question = "How high is Mt.Everest?";
		TextResponse resp = null;
		try {
			resp = cs.chat(question);
		} catch (Exception e) {
		}
		assertEquals(cs.getHistory().size(), 1);
		assertEquals(cs.getHistory().get(0).getRole(), "user");
		assertEquals(cs.getHistory().get(0).getContent(), "" + 9);
	}

}
