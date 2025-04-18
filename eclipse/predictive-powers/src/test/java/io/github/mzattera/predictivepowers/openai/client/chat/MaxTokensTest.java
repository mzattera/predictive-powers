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

package io.github.mzattera.predictivepowers.openai.client.chat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.openai.client.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatMessage;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatMessage.Role;

public class MaxTokensTest {

	@DisplayName("Check if max_tokens works.")
	@Test
	void testLogProbs() {
		try (OpenAiEndpoint ep = new OpenAiEndpoint();) {

			ChatCompletionsRequest req = ChatCompletionsRequest.builder().model("o1").build();
			req.getMessages().add(new OpenAiChatMessage(Role.USER, "Hello, how are you today?"));

//			req.setMaxTokens(3);
			req.setMaxCompletionTokens(3);

			ChatCompletionsResponse resp = ep.getClient().createChatCompletion(req);
			System.out.println(OpenAiClient.getJsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(resp));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
