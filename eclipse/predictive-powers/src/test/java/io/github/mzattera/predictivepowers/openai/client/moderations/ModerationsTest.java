package io.github.mzattera.predictivepowers.openai.client.moderations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;

class ModerationsTest {

	@Test
	void test01() {
		OpenAiEndpoint oai = OpenAiEndpoint.getInstance();
		OpenAiClient cli = oai.getClient();

		ModerationsRequest req = new ModerationsRequest();
		req.getInput().add("I want to kill everybody!");
		req.getInput().add("I to cut myself!");
		ModerationsResponse resp = cli.createModeration(req);

		assertEquals(resp.getResults().size(), 2);
		assertTrue(resp.getResults().get(0).getCategories().isViolence());
		assertTrue(resp.getResults().get(1).getCategories().isSelfHarm());

		System.out.println(resp);
	}
}
