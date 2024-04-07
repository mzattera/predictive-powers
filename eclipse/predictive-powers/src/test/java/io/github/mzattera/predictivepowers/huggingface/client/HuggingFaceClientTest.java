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
package io.github.mzattera.predictivepowers.huggingface.client;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.TestConfiguration;
import io.github.mzattera.predictivepowers.huggingface.client.nlp.TextGenerationRequest;
import io.github.mzattera.predictivepowers.huggingface.client.nlp.TextGenerationResponse;

public class HuggingFaceClientTest {

	@Test
	public void testTextGeneration() {

		if (!TestConfiguration.TEST_HF_SERVICES)
			return;

		try (HuggingFaceClient cli = new HuggingFaceClient()) {

			TextGenerationRequest req = new TextGenerationRequest();
			req.getInputs().add("How high is Mt. Everest?");
			req.getParameters().setMaxNewTokens(15);
			req.getParameters().setReturnFullText(false);

			// Prompt completion
			List<List<TextGenerationResponse>> resp = cli.textGeneration("gpt2", req);
			for (List<TextGenerationResponse> lsa : resp) {
				System.out.println("---");
				for (TextGenerationResponse ls : lsa) {
					System.out.println("\t" + ls.toString());
				}
			}
		}
	}
}
