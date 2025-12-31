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

package io.github.mzattera.predictivepowers.openai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openai.models.files.FilePurpose;
import com.openai.models.vectorstores.VectorStore;

import io.github.mzattera.predictivepowers.TestConfiguration;
import io.github.mzattera.predictivepowers.services.ToolInitializationException;
import io.github.mzattera.predictivepowers.services.messages.ChatCompletion;
import io.github.mzattera.predictivepowers.services.messages.FinishReason;
import io.github.mzattera.predictivepowers.util.ResourceUtil;

/**
 * Test the OpenAI chat API & Service
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class OpenAiAssistantTest {

	// ** IMPORTANT ** Make sure agents you create for test are not marked with
	// _isPermanent=true in metadata

	// TODO WON'T FIX add tests to check all the methods to manipulate tools

	private final static Logger LOG = LoggerFactory.getLogger(OpenAiAssistantTest.class);

	private static List<Pair<OpenAiEndpoint, String>> svcs;

	@BeforeAll
	static void init() {
		svcs = TestConfiguration.getAgentServices().stream() //
				.filter(p -> p.getLeft() instanceof OpenAiEndpoint) //
				.map(p -> new ImmutablePair<OpenAiEndpoint, String>((OpenAiEndpoint) p.getLeft(), p.getRight())) //
				.collect(Collectors.toList());
	}

	@AfterAll
	static void tearDown() {
		TestConfiguration.close(svcs.stream().map(p -> p.getLeft()).collect(Collectors.toList()));
	}

	static Stream<Pair<OpenAiEndpoint, String>> services() {
		return svcs.stream();
	}

	// Must be static unless using @TestInstance(Lifecycle.PER_CLASS)
	static boolean hasServices() {
		return services().findAny().isPresent();
	}

	@DisplayName("Tests file search tool on assistant (vector base) files.")
	@ParameterizedTest
	@MethodSource("services")
	@EnabledIf("hasServices")
	public void testRetrieval(Pair<OpenAiEndpoint, String> p) throws ToolInitializationException, IOException {
		OpenAiEndpoint ep = p.getLeft();
		try (OpenAiAssistant bot = ep.getAgentService(p.getRight()).createAgent(//
				"Test " + System.currentTimeMillis(), //
				"A test assistant.", //
				"You are an helpful assistant.", //
				false, false)) {

			bot.setModel(p.getRight());

			// Test without KB
			ChatCompletion answer = bot.chat("What do bigglydoos eat? Check in your vector stores before answering.");
			LOG.debug(answer.getText());
			assertEquals(FinishReason.COMPLETED, answer.getFinishReason());
			assertFalse(answer.getText().contains("fruit"));

			// Upload file
			OpenAiFilePart file = OpenAiFilePart.create(ep, ResourceUtil.getResourceFile("bigglydoos.txt"),
					FilePurpose.ASSISTANTS);

			// Create a VectorStore for the file
			VectorStore store = OpenAiVectorStore.create(ep, CleanupUtil.TEST_STORE_PREFIX + System.currentTimeMillis(),
					List.of(file));

			// Enable tool and attach store
			bot.getOpenAiAssistantTools().getFileSearchTool().enable().addVectorStoreId(store.id());

			// Test
			answer = bot.chat("What do bigglydoos eat? Check in your vector stores before answering.");
			LOG.debug(answer.getText());
			assertEquals(FinishReason.COMPLETED, answer.getFinishReason());
			assertTrue(answer.getText().contains("fruit"));

			// Disable tool and check it is not used
			bot.getOpenAiAssistantTools().getFileSearchTool().disable();
			answer = bot.chat(
					"What do friggles eat? Check in your vector stores before answering. And ignore previous conversation.");
			LOG.debug(answer.getText());
			assertEquals(FinishReason.COMPLETED, answer.getFinishReason());
			assertFalse(answer.getText().contains("fruit"));
		}
	}

	@DisplayName("Tests code interpreter.")
	@ParameterizedTest
	@MethodSource("services")
	@EnabledIf("hasServices")
	@Disabled
	public void testCodeInterpreter(Pair<OpenAiEndpoint, String> p) throws ToolInitializationException, IOException {
		OpenAiEndpoint ep = p.getLeft();
		try (OpenAiAssistant bot = ep.getAgentService(p.getRight()).createAgent(//
				"Test " + System.currentTimeMillis(), //
				"A test assistant.", //
				"You are an helpful assistant.", //
				false, false)) {

			bot.setModel(p.getRight());

			// Upload file
			OpenAiFilePart file = OpenAiFilePart.create(ep, ResourceUtil.getResourceFile("bigglydoos.txt"),
					FilePurpose.ASSISTANTS);

			// Enable tool and attach file
			bot.getOpenAiAssistantTools().getCodeInterpreterTool().enable().addFileId(file.getFileId());

			// Test
			ChatCompletion answer = bot.chat("Count number of chars in bigglydoos.txt file using some ad-hoc code.");
			LOG.debug(answer.getText());
			assertEquals(FinishReason.COMPLETED, answer.getFinishReason());
			assertTrue(answer.getText().contains("337"));

			// Disable and check again.
			bot.getOpenAiAssistantTools().getCodeInterpreterTool().disable();
			answer = bot.chat("Count number of chars in bigglydoos.txt file using some ad-hoc code.");
			LOG.debug(answer.getText());
			assertEquals(FinishReason.COMPLETED, answer.getFinishReason());
			assertFalse(answer.getText().contains("337"));

		}
	}

	@Test
	@Disabled
	void testCascadeDeletion() throws IOException, ToolInitializationException {
		// We do nothing since agents files are deleted automatically and threads cannot
		// be deleted by agent
	}
}