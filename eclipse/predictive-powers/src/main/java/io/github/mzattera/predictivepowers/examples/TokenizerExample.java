/*
 * Copyright 2023-2025 Massimiliano "Maxi" Zattera
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

import io.github.mzattera.predictivepowers.openai.OpenAiChatService;
import io.github.mzattera.predictivepowers.openai.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.OpenAiModelService;

public class TokenizerExample {

	public static void main(String[] args) throws Exception {

		// Get chat and model service
		try (OpenAiEndpoint endpoint = new OpenAiEndpoint();
				OpenAiChatService bot = endpoint.getChatService();
				OpenAiModelService modelService = endpoint.getModelService();) {

			// Set bot personality (instructions - system message)
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
			bot.setMaxConversationTokens(ctxSize - bot.getBaseTokens() - maxNewTokens);
			bot.setMaxNewTokens(maxNewTokens);

			// Optionally, you can limit the number of messages
			// kept in the conversation context; at most these many messages
			// from conversation history will be sent to the API at each
			// conversation exchange
			bot.setMaxConversationSteps(50);

			// From now on, service will manage conversation to respect those limits

			// ...

		} // Close resources
	}
}
