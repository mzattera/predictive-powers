package io.github.mzattera.predictivepowers.openai.client.edits;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;

class EditsTest {

	@Test
	void test01() {
		OpenAiEndpoint oai = OpenAiEndpoint.getInstance();
		String model = "text-davinci-edit-001";
		String prompt = "Put all text in uppercase.";
		String input = "How high is Mt. Everest?";
		EditsRequest req = new EditsRequest();

		req.setModel(model);
		req.setInstruction(prompt);
		req.setInput(input);
		req.setTemperature(0.0);

		EditsResponse resp = oai.getClient().createEdit(req);
		assertEquals(resp.getChoices().size(), 1);
		assertEquals(resp.getChoices().get(0).getText().trim(), "HOW HIGH IS MT. EVEREST?");
	}
}
