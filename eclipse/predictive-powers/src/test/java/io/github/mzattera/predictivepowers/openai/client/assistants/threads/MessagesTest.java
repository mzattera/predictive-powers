/*
 * Copyright 2024 Massimiliano "Maxi" Zattera
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

/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client.assistants.threads;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.openai.client.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.threads.Message;
import io.github.mzattera.predictivepowers.openai.client.threads.Message.Role;
import io.github.mzattera.predictivepowers.openai.client.threads.MessagesRequest;
import io.github.mzattera.predictivepowers.openai.client.threads.OpenAiThread;
import io.github.mzattera.predictivepowers.openai.client.threads.ThreadsRequest;

/**
 * @author Massimiliano "Maxi" Zattera
 */
public class MessagesTest {

	// TODO Add tests for (threads) messages API

	@Test
	void testMessageCreation() {
		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {

			// Test creation
			MessagesRequest original = MessagesRequest.builder() //
					.role(Role.USER) //
					.content("Hello") //
					.build();
			OpenAiThread thread = ep.getClient().createThread(ThreadsRequest.builder().build());
			Message msg = ep.getClient().createMessage(thread.getId(), original);

			assertEquals(thread.getId(), msg.getThreadId());
			assertEquals(original.getRole(), msg.getRole());
			assertEquals(1, msg.getTextContent().size());
			assertEquals(original.getTextContent(), msg.getTextContent().get(0).getText().getValue());
		}
	}
}
