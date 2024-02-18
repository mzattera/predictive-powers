/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client.assistants.threads;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.openai.client.threads.Message;
import io.github.mzattera.predictivepowers.openai.client.threads.Message.Role;
import io.github.mzattera.predictivepowers.openai.client.threads.MessagesRequest;
import io.github.mzattera.predictivepowers.openai.client.threads.OpenAiThread;
import io.github.mzattera.predictivepowers.openai.client.threads.ThreadsRequest;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;

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
			assertEquals(1, msg.getContent().size());
			assertEquals(original.getContent(), msg.getContent().get(0).getText().getValue());
		}
	}
}
