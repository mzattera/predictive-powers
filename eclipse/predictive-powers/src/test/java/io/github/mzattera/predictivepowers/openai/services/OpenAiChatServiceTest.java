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

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.mzattera.predictivepowers.TestConfiguration;
import io.github.mzattera.predictivepowers.openai.client.DirectOpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatMessage.Role;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiModelMetaData.SupportedApi;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiModelMetaData.SupportedCallType;
import io.github.mzattera.predictivepowers.services.ToolInitializationException;
import io.github.mzattera.predictivepowers.services.Toolset;
import io.github.mzattera.predictivepowers.services.messages.ChatCompletion;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage.Author;
import io.github.mzattera.predictivepowers.services.messages.FilePart;
import io.github.mzattera.predictivepowers.services.messages.FinishReason;
import io.github.mzattera.predictivepowers.services.messages.ToolCallResult;
import io.github.mzattera.util.ResourceUtil;

/**
 * Test the OpenAI chat API & Service
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class OpenAiChatServiceTest {

	// TODO add tests to check all the methods to manipulate tools

	private static List<ImmutablePair<OpenAiEndpoint, String>> svcs;

	@BeforeAll
	static void init() {
		svcs = TestConfiguration.getChatServices().stream() //
				.filter(p -> p.getLeft() instanceof OpenAiEndpoint) //
				.map(p -> new ImmutablePair<OpenAiEndpoint, String>((OpenAiEndpoint) p.getLeft(), p.getRight())) //
				.collect(Collectors.toList());
	}

	@AfterAll
	static void tearDown() {
		TestConfiguration.close(svcs.stream().map(p -> p.getLeft()).collect(Collectors.toList()));
	}

	static Stream<ImmutablePair<OpenAiEndpoint, String>> services() {
		return svcs.stream();
	}

	@DisplayName("Check completions not affecting history.")
	@ParameterizedTest
	@MethodSource("services")
	public void test01(Pair<OpenAiEndpoint, String> p) throws Exception {
		try (OpenAiChatService cs = p.getLeft().getChatService(p.getRight())) {
			assertEquals(0, cs.getModifiableHistory().size());

			// Personality
			String personality = "You are a smart and nice 	agent.";
			cs.setPersonality(personality);
			assertEquals(cs.getPersonality(), personality);

			// In completion, we do not consider history, but we consider personality.
			String question = "How high is Mt.Everest?";
			ChatCompletion resp = cs.complete(question);
			assertEquals(resp.getFinishReason(), FinishReason.COMPLETED);
			assertEquals(0, cs.getModifiableHistory().size());
			assertEquals(cs.getDefaultReq().getMessages().size(), 2);
			assertEquals(cs.getDefaultReq().getMessages().get(0).getRole(), Role.SYSTEM);
			assertEquals(cs.getDefaultReq().getMessages().get(0).getContent(), personality);
			assertEquals(cs.getDefaultReq().getMessages().get(1).getRole(), Role.USER);
			assertEquals(cs.getDefaultReq().getMessages().get(1).getContent(), question);
			assertEquals(cs.getDefaultReq().getMaxTokens(), null);
		}
	}

	@DisplayName("Check chat and history management.")
	@ParameterizedTest
	@MethodSource("services")
	public void test02(Pair<OpenAiEndpoint, String> p) throws Exception {
		OpenAiEndpoint ep = p.getLeft();
		String model = p.getRight();
		try (OpenAiChatService cs = ep.getChatService(model); OpenAiModelService modelService = ep.getModelService();) {

			// Personality, history length and conversation steps limits ////////////

			// Personality
			String personality = "You are a smart and nice agent.";
			cs.setPersonality(personality);
			assertEquals(cs.getPersonality(), personality);

			// Fake history
			for (int i = 0; i < 10; ++i) {
				cs.getModifiableHistory().add(new OpenAiChatMessage(Role.USER, "" + i));
			}

			cs.setMaxHistoryLength(3);
			cs.setMaxConversationSteps(2);
			assertEquals(cs.getMaxConversationTokens(), modelService.getContextSize(model)
					- Math.min(modelService.getContextSize(model) / 4, modelService.getMaxNewTokens(model)));

			String question = "How high is Mt.Everest?";
			ChatCompletion resp = cs.chat(question);
			assertEquals(resp.getFinishReason(), FinishReason.COMPLETED);
			assertEquals(3, cs.getModifiableHistory().size());
			assertEquals(cs.getModifiableHistory().get(0).getRole(), Role.USER);
			assertEquals(cs.getModifiableHistory().get(0).getContent(), "" + 9);
			assertEquals(cs.getModifiableHistory().get(1).getRole(), Role.USER);
			assertEquals(cs.getModifiableHistory().get(1).getContent(), question);
			assertEquals(cs.getModifiableHistory().get(2).getRole(), Role.ASSISTANT);
			assertEquals(cs.getModifiableHistory().get(2).getContent(), resp.getText());
			assertEquals(3, cs.getDefaultReq().getMessages().size());
			assertEquals(cs.getDefaultReq().getMessages().get(0).getRole(), Role.SYSTEM);
			assertEquals(cs.getDefaultReq().getMessages().get(0).getContent(), personality);
			assertEquals(cs.getDefaultReq().getMessages().get(1).getRole(), Role.USER);
			assertEquals(cs.getDefaultReq().getMessages().get(1).getContent(), "" + 9);
			assertEquals(cs.getDefaultReq().getMessages().get(2).getRole(), Role.USER);
			assertEquals(cs.getDefaultReq().getMessages().get(2).getContent(), question);
			assertEquals(cs.getDefaultReq().getMaxTokens(), null);

			// NO personality, history length and conversation steps limits ////////////
			// Also testing maxTokens

			// Fake history
			cs.clearConversation();
			assertEquals(cs.getModifiableHistory().size(), 0);
			for (int i = 0; i < 10; ++i) {
				cs.getModifiableHistory().add(new OpenAiChatMessage(Role.USER, "" + i));
			}
			cs.setPersonality(null);
			cs.setMaxNewTokens(100);

			resp = cs.chat(question);
			assertEquals(resp.getFinishReason(), FinishReason.COMPLETED);
			assertEquals(cs.getModifiableHistory().size(), 3);
			assertEquals(cs.getModifiableHistory().get(0).getRole(), Role.USER);
			assertEquals(cs.getModifiableHistory().get(0).getContent(), "" + 9);
			assertEquals(cs.getModifiableHistory().get(1).getRole(), Role.USER);
			assertEquals(cs.getModifiableHistory().get(1).getContent(), question);
			assertEquals(cs.getModifiableHistory().get(2).getRole(), Role.ASSISTANT);
			assertEquals(cs.getModifiableHistory().get(2).getContent(), resp.getText());
			assertEquals(2, cs.getDefaultReq().getMessages().size());
			assertEquals(cs.getDefaultReq().getMessages().get(0).getRole(), Role.USER);
			assertEquals(cs.getDefaultReq().getMessages().get(0).getContent(), "" + 9);
			assertEquals(cs.getDefaultReq().getMessages().get(1).getRole(), Role.USER);
			assertEquals(cs.getDefaultReq().getMessages().get(1).getContent(), question);
			assertEquals(cs.getDefaultReq().getMaxTokens(), 100);

			// Personality, history length and conversation tokens limits ////////////

			// Fake history
			cs.clearConversation();
			for (int i = 0; i < 10; ++i) {
				cs.getModifiableHistory().add(new OpenAiChatMessage(Role.USER, "" + i));
			}
			cs.setPersonality(personality);
			cs.setMaxNewTokens(null);
			cs.setMaxHistoryLength(3);
			cs.setMaxConversationSteps(9999);
			cs.setMaxConversationTokens(12); // this should accomodate personality and question

			resp = cs.chat(question);
			assertEquals(resp.getFinishReason(), FinishReason.COMPLETED);
			assertEquals(cs.getModifiableHistory().size(), 3);
			assertEquals(cs.getModifiableHistory().get(0).getRole(), Role.USER);
			assertEquals(cs.getModifiableHistory().get(0).getContent(), "" + 9);
			assertEquals(cs.getModifiableHistory().get(1).getRole(), Role.USER);
			assertEquals(cs.getModifiableHistory().get(1).getContent(), question);
			assertEquals(cs.getModifiableHistory().get(2).getRole(), Role.ASSISTANT);
			assertEquals(cs.getModifiableHistory().get(2).getContent(), resp.getText());
			assertEquals(2, cs.getDefaultReq().getMessages().size());
			assertEquals(cs.getDefaultReq().getMessages().get(0).getRole(), Role.SYSTEM);
			assertEquals(cs.getDefaultReq().getMessages().get(0).getContent(), personality);
			assertEquals(cs.getDefaultReq().getMessages().get(1).getRole(), Role.USER);
			assertEquals(cs.getDefaultReq().getMessages().get(1).getContent(), question);
			assertEquals(cs.getDefaultReq().getMaxTokens(), null);

			// NO personality, history length and conversation tokens limits ////////////

			// Fake history
			cs.clearConversation();
			for (int i = 0; i < 10; ++i) {
				cs.getModifiableHistory().add(new OpenAiChatMessage(Role.USER, "" + i));
			}
			cs.setPersonality(null);
			cs.setMaxNewTokens(null);
			cs.setMaxHistoryLength(3);
			cs.setMaxConversationSteps(9999);
			cs.setMaxConversationTokens(12);

			resp = cs.chat(question);
			assertEquals(resp.getFinishReason(), FinishReason.COMPLETED);
			assertEquals(cs.getModifiableHistory().size(), 3);
			assertEquals(cs.getModifiableHistory().get(0).getRole(), Role.USER);
			assertEquals(cs.getModifiableHistory().get(0).getContent(), "" + 9);
			assertEquals(cs.getModifiableHistory().get(1).getRole(), Role.USER);
			assertEquals(cs.getModifiableHistory().get(1).getContent(), question);
			assertEquals(cs.getModifiableHistory().get(2).getRole(), Role.ASSISTANT);
			assertEquals(cs.getModifiableHistory().get(2).getContent(), resp.getText());
			assertEquals(1, cs.getDefaultReq().getMessages().size());
			assertEquals(cs.getDefaultReq().getMessages().get(0).getRole(), Role.USER);
			assertEquals(cs.getDefaultReq().getMessages().get(0).getContent(), question);
			assertEquals(cs.getDefaultReq().getMaxTokens(), null);

			// Completion with no personality
			cs.setPersonality(null);
			cs.setMaxNewTokens(null);
			resp = cs.complete(question);
			assertEquals(resp.getFinishReason(), FinishReason.COMPLETED);
			assertEquals(cs.getModifiableHistory().size(), 3);
			assertEquals(1, cs.getDefaultReq().getMessages().size());
			assertEquals(cs.getDefaultReq().getMessages().get(0).getRole(), Role.USER);
			assertEquals(cs.getDefaultReq().getMessages().get(0).getContent(), question);
			assertEquals(cs.getDefaultReq().getMaxTokens(), null);

		} // Close endpoint
	}

	@DisplayName("Check chat and history management with exception.")
	@ParameterizedTest
	@MethodSource("services")
	public void test03(Pair<OpenAiEndpoint, String> p) throws Exception {
		OpenAiEndpoint ep = p.getLeft();
		String model = p.getRight();
		try (OpenAiChatService cs = ep.getChatService(model)) {

			// Personality, history length and conversation steps limits ////////////

			// Personality
			cs.setPersonality(null);

			// Fake history
			for (int i = 0; i < 10; ++i) {
				cs.getModifiableHistory().add(new OpenAiChatMessage(Role.USER, "" + i));
			}

			cs.setMaxHistoryLength(1);
			cs.setModel("gpt-bananas");

			String question = "How high is Mt.Everest?";

			try {
				cs.chat(question);
			} catch (Exception e) {
				// Should fail because context wrong model name
			}

			// If chat fails, history is not changed
			assertEquals(10, cs.getModifiableHistory().size());
		}
	}

	@DisplayName("Getters and setters.")
	@Test
	public void testGettersAndSetters() {
		try (OpenAiEndpoint ep = new DirectOpenAiEndpoint()) {
			OpenAiChatService s = ep.getChatService();
			String m = s.getModel();
			assertNotNull(m);
			s.setModel("pippo");
			assertEquals("pippo", s.getModel());
			s.setModel(m);

			assertNull(s.getTopK());
			s.setTopK(null);
			assertNull(s.getTopK());
			assertThrows(UnsupportedOperationException.class, () -> s.setTopK(1));

			s.setTopP(2.0);
			assertEquals(2.0, s.getTopP());
			s.setTopP(null);
			assertNull(s.getTopP());

			s.setTemperature(3.0);
			assertEquals(3.0, s.getTemperature());
			s.setTemperature(null);
			assertNull(s.getTemperature());
			s.setTemperature(1.0);

			s.setMaxNewTokens(4);
			assertEquals(4, s.getMaxNewTokens());
			s.setMaxNewTokens(null);
			assertNull(s.getMaxNewTokens());
			s.setMaxNewTokens(15);
		}
	}

	@DisplayName("Test image URLs in messages.")
	@Test
	void testImgUrls() throws MalformedURLException, URISyntaxException {

		// TODO test with Azure as well
		if (!TestConfiguration.TEST_DIRECT_OPENAI_SERVICES)
			return;

		try (OpenAiEndpoint endpoint = new DirectOpenAiEndpoint(); OpenAiChatService svc = endpoint.getChatService();) {
			svc.setModel("gpt-4-vision-preview");

			ChatMessage msg = new ChatMessage(Author.USER, "Is there any grass in this image?");
			msg.getParts().add(FilePart.fromUrl(
					"https://upload.wikimedia.org/wikipedia/commons/thumb/d/dd/Gfp-wisconsin-madison-the-nature-boardwalk.jpg/2560px-Gfp-wisconsin-madison-the-nature-boardwalk.jpg",
					"image/jpeg"));
			ChatCompletion resp = svc.chat(msg);
			System.out.println(resp.getText());
			assertEquals(FinishReason.COMPLETED, resp.getFinishReason());
			assertTrue(resp.getText().toUpperCase().contains("YES"));
		} // Close endpoint
	}

	@DisplayName("Test image URLs in messages.")
	@Test
	void testImgFiles() throws MalformedURLException, URISyntaxException {

		if (!TestConfiguration.TEST_DIRECT_OPENAI_SERVICES)
			return;

		try (OpenAiEndpoint endpoint = new DirectOpenAiEndpoint(); //
				OpenAiChatService svc = endpoint.getChatService();) {
			svc.setModel("gpt-4-vision-preview");

			ChatMessage msg = new ChatMessage(Author.USER, "Is there any grass in this image?");
			msg.getParts().add(new FilePart(
					ResourceUtil.getResourceFile("Gfp-wisconsin-madison-the-nature-boardwalk.jpg"), "image/jpeg"));
			ChatCompletion resp = svc.chat(msg);
			assertEquals(FinishReason.COMPLETED, resp.getFinishReason());
			assertTrue(resp.getText().toUpperCase().contains("YES"));
		} // Close endpoint
	}

	/**
	 * Test removal of tool call results at beginning of conversations without
	 * corresponding calls, which causes errors.
	 * 
	 * @throws ToolInitializationException
	 */
	@Test
	void testCallResultsOnTop() throws ToolInitializationException {
		try (DirectOpenAiEndpoint endpoint = new DirectOpenAiEndpoint();
				OpenAiChatService svc = endpoint.getChatService();
				OpenAiModelService modelSvc = endpoint.getModelService();) {

			Toolset tools = new Toolset();
			tools.putTool("aTool", FunctionCallTest.GetCurrentWeatherTool.class);
			svc.addCapability(tools);

			// Tests with functions
			String model = null;
			for (String modelId : modelSvc.listModels())
				if ((modelSvc.getSupportedCallType(modelId) == SupportedCallType.FUNCTIONS)
						&& (modelSvc.getSupportedApi(modelId) == SupportedApi.CHAT)) {
					model = modelId;
					break;
				}
			svc.setModel(model);
			svc.clearConversation();
			svc.getModifiableHistory()
					.add(new OpenAiChatMessage(Role.FUNCTION, new ToolCallResult("callID", "aTool", "result")));
			svc.getModifiableHistory().add(new OpenAiChatMessage(Role.USER, "Hi!"));

			ChatCompletion resp = svc.chat("Hi");
			assertEquals(FinishReason.COMPLETED, resp.getFinishReason());

			// Tests with tools
			model = null;
			for (String modelId : modelSvc.listModels())
				if ((modelSvc.getSupportedCallType(modelId) == SupportedCallType.TOOLS)
						&& (modelSvc.getSupportedApi(modelId) == SupportedApi.CHAT)) {
					model = modelId;
					break;
				}
			svc.setModel(model);
			svc.clearConversation();
			svc.getModifiableHistory()
					.add(new OpenAiChatMessage(Role.TOOL, new ToolCallResult("callID", "aTool", "result")));
			svc.getModifiableHistory().add(new OpenAiChatMessage(Role.USER, "Hi!"));

			resp = svc.chat("Hi");
			assertEquals(FinishReason.COMPLETED, resp.getFinishReason());
		}
	}
}
