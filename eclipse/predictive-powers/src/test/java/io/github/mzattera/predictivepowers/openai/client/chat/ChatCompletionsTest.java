package io.github.mzattera.predictivepowers.openai.client.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.Models;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsResponse;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatMessage;

class ChatCompletionsTest {

	@Test
	void test01() {
		OpenAiEndpoint oai = OpenAiEndpoint.getInstance();
		String model = "gpt-3.5-turbo";
		String prompt = "How high is Mt. Everest?";
		ChatCompletionsRequest cr = new ChatCompletionsRequest();

		cr.setModel(model);
		cr.getMessages().add(ChatMessage.builder().role("user").content(prompt).build());
		cr.setMaxTokens(Models.getContextSize(model) - 15);
		cr.setStop(new String[] { "feet" });

System.out.println(cr.toString());

		ChatCompletionsResponse resp = oai.getClient().createChatCompletion(cr);

System.out.println(resp.toString());
		
		assertEquals(resp.getChoices().length, 1);
		assertEquals(resp.getChoices()[0].getFinishReason(), "stop");
		assertTrue(resp.getChoices()[0].getMessage().getContent().contains("848") || resp.getChoices()[0].getMessage().getContent().contains("029 "));
		assertTrue(resp.getChoices()[0].getMessage().getContent().endsWith("029 "));
	}
}
