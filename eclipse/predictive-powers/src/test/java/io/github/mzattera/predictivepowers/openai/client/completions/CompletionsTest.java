package io.github.mzattera.predictivepowers.openai.client.completions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.util.ModelUtil;
import io.github.mzattera.predictivepowers.services.CompletionService;
import io.github.mzattera.predictivepowers.services.TextResponse;

class CompletionsTest {

	@Test
	void test01() {
		OpenAiEndpoint oai = OpenAiEndpoint.getInstance();
		String model = "text-davinci-003";
		String prompt = "How high is Mt. Everest?";
		CompletionsRequest cr = new CompletionsRequest();

		cr.setModel(model);
		cr.setPrompt(prompt);
		cr.setMaxTokens(ModelUtil.getContextSize(model) - 7);
		cr.setStop(new ArrayList<>());
		cr.getStop().add("feet");

		CompletionsResponse resp = oai.getClient().createCompletion(cr);

		assertEquals(resp.getChoices().size(), 1);
		assertEquals(resp.getChoices().get(0).getFinishReason(), "stop");
	}

	@Test
	void test02() {
		OpenAiEndpoint oai = OpenAiEndpoint.getInstance();
		String model = "text-davinci-003";
		String prompt = "How high is Mt. Everest?";
		CompletionsRequest cr = new CompletionsRequest();

		cr.setModel(model);
		cr.setPrompt(prompt);
		cr.setMaxTokens(ModelUtil.getContextSize(model) - 7);
		cr.setN(3);

		CompletionsResponse resp = oai.getClient().createCompletion(cr);

		assertEquals(resp.getChoices().size(), 3);
		assertEquals(resp.getChoices().get(0).getFinishReason(), "stop");

		for (CompletionsChoice c : resp.getChoices()) {
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
		cr.setMaxTokens(ModelUtil.getContextSize(model) - 7);
		cr.setLogprobs(2);

		CompletionsResponse resp = oai.getClient().createCompletion(cr);

		assertEquals(resp.getChoices().size(), 1);
		assertEquals(resp.getChoices().get(0).getFinishReason(), "stop");
	}

	@Test
	void test04() {
		OpenAiEndpoint oai = OpenAiEndpoint.getInstance();
		CompletionService cs = oai.getCompletionService();

		cs.getDefaultReq().setEcho(true);
		cs.getDefaultReq().setTemperature(0.0);
		String prompt = "Mt. Everest is";
		TextResponse resp = cs.complete(prompt);
		
		assertTrue(resp.getText().startsWith(prompt));
	}

	@Test
	void test05() {
		OpenAiEndpoint oai = OpenAiEndpoint.getInstance();
		CompletionService cs = oai.getCompletionService();

		cs.getDefaultReq().setEcho(false);
		cs.getDefaultReq().setTemperature(0.0);
		String prompt = "Mt. Everest is";
		String suffix = "meters high.";	
		TextResponse resp = cs.insert(prompt, suffix);
		
		assertTrue(resp.getText().contains("8,848"));
	}
}
