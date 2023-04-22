package io.github.mzattera.predictivepowers.client.openai.completions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.Models;
import io.github.mzattera.predictivepowers.openai.client.completions.Choice;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsResponse;

class CompletionsTest {

	@Test
	void test01() {
		OpenAiEndpoint oai = OpenAiEndpoint.getInstance();
		String model = "text-davinci-003";
		String prompt = "How high is Mt. Everest?";
		CompletionsRequest cr = new CompletionsRequest();

		cr.setModel(model);
		cr.setPrompt(prompt);
		cr.setMaxTokens(Models.getContextSize(model) - 7);
		cr.setStop(new String[] { "feet" });

		CompletionsResponse resp = oai.getClient().createCompletion(cr);

		assertEquals(resp.getChoices().length, 1);
		assertEquals(resp.getChoices()[0].getFinishReason(), "stop");
		assertTrue(resp.getChoices()[0].getText().contains("848"));
		assertTrue(resp.getChoices()[0].getText().endsWith("029 "));
	}

	@Test
	void test02() {
		OpenAiEndpoint oai = OpenAiEndpoint.getInstance();
		String model = "text-davinci-003";
		String prompt = "How high is Mt. Everest?";
		CompletionsRequest cr = new CompletionsRequest();

		cr.setModel(model);
		cr.setPrompt(prompt);
		cr.setMaxTokens(Models.getContextSize(model) - 7);
		cr.setN(3);

		CompletionsResponse resp = oai.getClient().createCompletion(cr);

		assertEquals(resp.getChoices().length, 3);
		assertEquals(resp.getChoices()[0].getFinishReason(), "stop");

		for (Choice c : resp.getChoices()) {
			assertTrue(c.getText().contains("848"));
		}
	}

	@Test
	void test03() {
		OpenAiEndpoint oai = OpenAiEndpoint.getInstance();
		String model = "text-davinci-003";
		String prompt = "How high is Mt. Everest?";
		CompletionsRequest cr = new CompletionsRequest();

		cr.setModel(model);
		cr.setPrompt(prompt);
		cr.setMaxTokens(Models.getContextSize(model) - 7);
		cr.setLogprobs(2);

		CompletionsResponse resp = oai.getClient().createCompletion(cr);

		assertEquals(resp.getChoices().length, 1);
		assertEquals(resp.getChoices()[0].getFinishReason(), "stop");
	}
}
