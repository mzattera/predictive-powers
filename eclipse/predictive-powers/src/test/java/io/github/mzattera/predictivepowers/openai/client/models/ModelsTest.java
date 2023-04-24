package io.github.mzattera.predictivepowers.openai.client.models;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.Models;

class ModelsTest {

	@Test
	void test01() {
		OpenAiEndpoint oai = OpenAiEndpoint.getInstance();
		List<Model> models = oai.getClient().listModels();
		assertTrue(models.size() > 0);
		
		for (Model m : models) {
			int contextSize = Models.getContextSize(m.getId());
			if (contextSize < 0) {
				System.out.println("No context size defined for model: " + m.getId());
			}
			assertTrue(contextSize > 0);
		}		
	}
}
