package io.github.mzattera.predictivepowers.openai.client.edits;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.edits.EditsRequest;
import io.github.mzattera.predictivepowers.openai.client.edits.EditsResponse;

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

		System.out.println(req.toString());

		EditsResponse resp = oai.getClient().createEdit(req);

		System.out.println(resp.toString());

		assertEquals(resp.getChoices().length, 1);
		assertEquals(resp.getChoices()[0].getText().trim(), "HOW HIGH IS MT. EVEREST?");
	}
}
