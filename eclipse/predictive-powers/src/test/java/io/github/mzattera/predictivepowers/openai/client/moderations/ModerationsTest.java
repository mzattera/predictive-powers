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

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;

class ModerationsTest {

	@Test
	void test01() {
		try (OpenAiEndpoint oai = new OpenAiEndpoint()) {
			OpenAiClient cli = oai.getClient();

			ModerationsRequest req = new ModerationsRequest();
			req.getInput().add("I want to see unicorns!");
			ModerationsResponse resp = cli.createModeration(req);

			// The below have all been tested with sentences that is better not to put in GitHub :-) and they work.
			assertEquals(1, resp.getResults().size());
//			assertEquals(11, resp.getResults().size());
//			assertTrue(resp.getResults().get(0).getCategories().isHate());
//			assertTrue(resp.getResults().get(1).getCategories().isHateThreatening());
//			assertTrue(resp.getResults().get(2).getCategories().isHarassment());
//			assertTrue(resp.getResults().get(3).getCategories().isHarassmentThreatening());
//			assertTrue(resp.getResults().get(4).getCategories().isSelfHarm());
//			assertTrue(resp.getResults().get(5).getCategories().isSelfHarmIntent());
//			assertTrue(resp.getResults().get(6).getCategories().isSelfHarmInstructions());
//			assertTrue(resp.getResults().get(7).getCategories().isSexual());
//			assertTrue(resp.getResults().get(8).getCategories().isSexualMinors());
//			assertTrue(resp.getResults().get(9).getCategories().isViolence());
//			assertTrue(resp.getResults().get(10).getCategories().isViolenceGraphic());

//			assertTrue(resp.getResults().get(0).getCategoryScores().getHate() >= 0.0);
//			assertTrue(resp.getResults().get(0).getCategoryScores().getHateThreatening() >= 0.0);
//			assertTrue(resp.getResults().get(0).getCategoryScores().getHarassment() >= 0.0);
//			assertTrue(resp.getResults().get(0).getCategoryScores().getHarassmentThreatening() >= 0.0);
//			assertTrue(resp.getResults().get(0).getCategoryScores().getSelfHarm() >= 0.0);
//			assertTrue(resp.getResults().get(0).getCategoryScores().getSelfHarmIntent() >= 0.0);
//			assertTrue(resp.getResults().get(0).getCategoryScores().getSelfHarmInstructions() >= 0.0);
//			assertTrue(resp.getResults().get(0).getCategoryScores().getSexual() >= 0.0);
//			assertTrue(resp.getResults().get(0).getCategoryScores().getSexualMinors() >= 0.0);
//			assertTrue(resp.getResults().get(0).getCategoryScores().getViolence() >= 0.0);
//			assertTrue(resp.getResults().get(0).getCategoryScores().getViolenceGraphic() >= 0.0);

		} // Close endpoint
	}
}
