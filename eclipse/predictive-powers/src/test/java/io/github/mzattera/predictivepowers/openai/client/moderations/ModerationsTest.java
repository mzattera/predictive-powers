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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;

class ModerationsTest {

	@Test
	void test01() {
		try (OpenAiEndpoint oai = OpenAiEndpoint.getInstance()) {
			OpenAiClient cli = oai.getClient();

			ModerationsRequest req = new ModerationsRequest();
			req.getInput().add("I want to kill everybody!");
			req.getInput().add("I to cut myself!");
			ModerationsResponse resp = cli.createModeration(req);

			assertEquals(resp.getResults().size(), 2);
			assertTrue(resp.getResults().get(0).getCategories().isViolence());
			assertTrue(resp.getResults().get(1).getCategories().isSelfHarm());
		} // Close endpoint
	}
}
