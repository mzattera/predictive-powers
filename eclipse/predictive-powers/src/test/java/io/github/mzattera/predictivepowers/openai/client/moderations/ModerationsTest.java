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
package io.github.mzattera.predictivepowers.openai.client.moderations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;

class ModerationsTest {

	@Test
	void test01() {
		try (OpenAiEndpoint oai = new OpenAiEndpoint()) {
			OpenAiClient cli = oai.getClient();

			ModerationsRequest req = new ModerationsRequest();
			req.getInput().add("I want to kill everybody!");
			req.getInput().add("I want to cut myself!");
			ModerationsResponse resp = cli.createModeration(req);

			assertEquals(resp.getResults().size(), 2);
			assertFalse(resp.getResults().get(0).getCategories().isHate());
			assertFalse(resp.getResults().get(0).getCategories().isHateThreatening());
			assertFalse(resp.getResults().get(0).getCategories().isHarassment());
			assertFalse(resp.getResults().get(0).getCategories().isHarassmentThreatening());
			assertFalse(resp.getResults().get(0).getCategories().isSelfHarm());
			assertFalse(resp.getResults().get(0).getCategories().isSelfHarmIntent());
			assertFalse(resp.getResults().get(0).getCategories().isSelfHarmInstructions());
			assertFalse(resp.getResults().get(0).getCategories().isSexual());
			assertFalse(resp.getResults().get(0).getCategories().isSexualMinors());
			assertTrue(resp.getResults().get(0).getCategories().isViolence());
			assertFalse(resp.getResults().get(0).getCategories().isViolenceGraphic());

			assertTrue(resp.getResults().get(0).getCategoryScores().getHate() >= 0.0);
			assertTrue(resp.getResults().get(0).getCategoryScores().getHateThreatening() >= 0.0);
			assertTrue(resp.getResults().get(0).getCategoryScores().getHarassment() >= 0.0);
			assertTrue(resp.getResults().get(0).getCategoryScores().getHarassmentThreatening() >= 0.0);
			assertTrue(resp.getResults().get(0).getCategoryScores().getSelfHarm() >= 0.0);
			assertTrue(resp.getResults().get(0).getCategoryScores().getSelfHarmIntent() >= 0.0);
			assertTrue(resp.getResults().get(0).getCategoryScores().getSelfHarmInstructions() >= 0.0);
			assertTrue(resp.getResults().get(0).getCategoryScores().getSexual() >= 0.0);
			assertTrue(resp.getResults().get(0).getCategoryScores().getSexualMinors() >= 0.0);
			assertTrue(resp.getResults().get(0).getCategoryScores().getViolence() >= 0.0);
			assertTrue(resp.getResults().get(0).getCategoryScores().getViolenceGraphic() >= 0.0);

		} // Close endpoint
	}
}
