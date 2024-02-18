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

package io.github.mzattera.predictivepowers.openai.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.openai.client.chat.OpenAiTool;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.Capability;
import io.github.mzattera.predictivepowers.services.ToolInitializationException;
import io.github.mzattera.predictivepowers.services.Toolset;
import io.github.mzattera.predictivepowers.services.messages.ChatCompletion;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage;
import io.github.mzattera.predictivepowers.services.messages.FilePart;
import io.github.mzattera.predictivepowers.services.messages.FinishReason;
import io.github.mzattera.util.ResourceUtil;

/**
 * Test the OpenAI chat API & Service
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class OpenAiAssistantTest {

	// ** IMPORTANT ** Make sure agents you create for test are not marked with _isPermanent=true in metadata

	// TODO add tests to check all the methods to manipulate tools
	
	/**
	 * Tests assistants files and retrieval tool.
	 */
	@Test
	public void testRetrieval() throws ToolInitializationException, IOException {
		try (OpenAiEndpoint endpoint = new OpenAiEndpoint()) {

			OpenAiAssistant bot = endpoint.getAgentService().createAgent(//
					"Test " + System.currentTimeMillis(), //
					"A test assistant.", //
					"You are an helpful assistant.", //
					false, false);

			Capability tools = new Toolset();
			tools.putTool("retrieval", () -> {
				return OpenAiTool.RETRIEVAL;
			});
			bot.addCapability(tools);

			bot.addFile(new FilePart(ResourceUtil.getResourceFile("bigglydoos.txt")));

			ChatCompletion answer = bot.chat("What do bigglydoos eat?");
			assertEquals(FinishReason.COMPLETED, answer.getFinishReason());
			assertTrue(answer.getText().contains("fruit"));

			List<? extends FilePart> files = bot.listFiles();
			assertEquals(1, files.size());
			assertTrue(bot.removeFile(files.get(0)));
			files = bot.listFiles();
			assertEquals(0, files.size());
		}
	}

	/**
	 * Tests file attachments.
	 * 
	 * @throws ToolInitializationException
	 */
	@Test
	void testFileAttacch() throws ToolInitializationException {
		try (OpenAiEndpoint endpoint = new OpenAiEndpoint()) {

			OpenAiAssistant bot = endpoint.getAgentService().createAgent(//
					"Test " + System.currentTimeMillis(), //
					"A test assistant.", //
					"You are an helpful assistant.", //
					false, false);

			Capability tools = new Toolset();
			tools.putTool("retrieval", () -> {
				return OpenAiTool.RETRIEVAL;
			});
			bot.addCapability(tools);

			ChatMessage msg = new ChatMessage("What animals are described in the text file attached to this message?");
			msg.getParts().add(new FilePart(ResourceUtil.getResourceFile("bigglydoos.txt")));

			ChatCompletion answer = bot.chat(msg);
			assertEquals(FinishReason.COMPLETED, answer.getFinishReason());
			assertTrue(answer.getText().toLowerCase().contains("bigglydoos"));
		}
	}

	@Test
	void testCascadeDeletion() throws IOException, ToolInitializationException {
		// We do nothing since agents files are deleted automatically and threads cannot
		// be deleted by agent
	}
}