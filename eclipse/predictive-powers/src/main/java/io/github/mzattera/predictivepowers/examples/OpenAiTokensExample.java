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

package io.github.mzattera.predictivepowers.examples;

import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService;

public class OpenAiTokensExample {

	public static void main(String[] args) {

		// Get chat service and set bot personality
		try (OpenAiEndpoint endpoint = new OpenAiEndpoint();
				OpenAiChatService bot = endpoint.getChatService();
				OpenAiModelService modelService = endpoint.getModelService();) {

			bot.setPersonality("You are an helpful and kind assistant.");

			// Number of tokens in bot context
			String model = bot.getModel();
			int ctxSize = modelService.getContextSize(model);

			// Let's keep 1/4th of the tokens for bot replies
			// Notice that some models have a limit on
			// maximum number of generated tokens that can be smaller
			int maxNewTokens = Math.min(ctxSize / 4, modelService.getMaxNewTokens(model));

			// Set the maximum number of tokens for conversation history and bot reply
			// Notice in the calculation we consider tokens used by the bot personality
			bot.setMaxNewTokens(maxNewTokens);
			bot.setMaxConversationTokens(ctxSize - bot.getBaseTokens() - maxNewTokens);

			// From now on, service will manage conversation history to respect those limits

			// ...

		} // Close resources
	}
}
