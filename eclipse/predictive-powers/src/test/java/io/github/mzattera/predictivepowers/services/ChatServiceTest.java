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

package io.github.mzattera.predictivepowers.services;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.mzattera.predictivepowers.AiEndpoint;
import io.github.mzattera.predictivepowers.TestConfiguration;
import io.github.mzattera.predictivepowers.huggingface.services.HuggingFaceChatService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatService;
import io.github.mzattera.predictivepowers.services.messages.ChatCompletion;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage;
import io.github.mzattera.predictivepowers.services.messages.FilePart;
import io.github.mzattera.predictivepowers.services.messages.FinishReason;
import io.github.mzattera.predictivepowers.util.ResourceUtil;

/**
 * Tests chat services.
 * 
 * @author Massimiliano "Maxi" Zattera.
 */
public class ChatServiceTest {

	private static List<Pair<AiEndpoint, String>> svcs;

	@BeforeAll
	static void init() {
		svcs = TestConfiguration.getChatServices();
	}

	@AfterAll
	static void tearDown() {
		TestConfiguration.close(svcs.stream().map(p -> p.getLeft()).collect(Collectors.toList()));
	}

	// All services planned to be tested
	static Stream<Pair<AiEndpoint, String>> services() {
		return svcs.stream();
	}

	static boolean hasServices() {
		return services().findAny().isPresent();
	}

	// All services planned to be tested
	static Stream<Pair<AiEndpoint, String>> imageServices() {

		List<Pair<AiEndpoint, String>> cs = new ArrayList<>();
		for (Pair<AiEndpoint, String> p : svcs) {
			try (ModelService ms = p.getLeft().getModelService()) {
				if (ms.get(p.getRight()).supportsImageInput())
					cs.add(p);
			}
		}
		return cs.stream();
	}

	static boolean hasImageServices() {
		return imageServices().findAny().isPresent();
	}

	@DisplayName("Basic completion and chat.")
	@ParameterizedTest
	@MethodSource("services")
	@EnabledIf("hasServices")
	public void testBasis(Pair<AiEndpoint, String> p) throws Exception {
		try (ChatService s = p.getLeft().getChatService(p.getRight())) {
			ChatCompletion resp = s.chat("Hi, my name is Maxi.");
			assertTrue(resp.getFinishReason() == FinishReason.COMPLETED);
			resp = s.chat("Can you please repeat my name?");
			assertTrue(resp.getFinishReason() == FinishReason.COMPLETED);
			assertTrue(resp.getText().contains("Maxi"));

			resp = s.complete("Hi, my name is Maxi.");
			assertTrue(resp.getFinishReason() == FinishReason.COMPLETED);
			resp = s.complete("Can you please repeat my name?");
			assertTrue(resp.getFinishReason() == FinishReason.COMPLETED);
			assertFalse(resp.getText().contains("Maxi"));
		}
	}

	@DisplayName("Getters and setters.")
	@ParameterizedTest
	@MethodSource("services")
	@EnabledIf("hasServices")
	public void testGetSet(Pair<AiEndpoint, String> p) throws Exception {
		try (ChatService s = p.getLeft().getChatService(p.getRight())) {
			String m = s.getModel();
			assertNotNull(m);
			s.setModel("pippo");
			assertEquals("pippo", s.getModel());
			s.setModel(m);

			assertTrue(p.getLeft() == s.getEndpoint());

			s.setMaxHistoryLength(7);
			assertEquals(7, s.getMaxHistoryLength());
			// TODO make it nullable
//			s.setMaxHistoryLength(null);
//			assertNull(s.getMaxHistoryLength());

			s.setMaxConversationSteps(5);
			assertEquals(5, s.getMaxConversationSteps());
			// TODO make it nullable
//			s.setMaxConversationSteps(null);
//			assertNull(s.getMaxConversationSteps());

			if ((s instanceof OpenAiChatService) || (s instanceof HuggingFaceChatService)) {
				assertThrows(UnsupportedOperationException.class, () -> s.setTopK(1));
			} else {
				s.setTopK(1);
				assertEquals(1, s.getTopK());
				s.setTopK(null);
				assertNull(s.getTopK());
			}

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

			if (s instanceof OpenAiChatService) {
				@SuppressWarnings("resource")
				OpenAiChatService svc = (OpenAiChatService) s;
				svc.setDefaultRequest(svc.getDefaultRequest().toBuilder().maxCompletionTokens(99).build());
				assertEquals(99, s.getMaxNewTokens());
			}
		}
	}

	@DisplayName("Call exercising all parameters.")
	@ParameterizedTest
	@MethodSource("services")
	@EnabledIf("hasServices")
	public void testParams(Pair<AiEndpoint, String> p) throws Exception {

		try (ChatService s = p.getLeft().getChatService(p.getRight())) {

			ChatCompletion resp = null;

			if ((s instanceof OpenAiChatService) || (s instanceof HuggingFaceChatService)) {
				assertThrows(UnsupportedOperationException.class, () -> s.setTopK(5));
			} else {
				s.setTopK(5);
			}
			s.setTopP(null);
			s.setTemperature(null);
			s.setMaxNewTokens(40);
			resp = s.complete("Name a mammal.");
			assertTrue((FinishReason.COMPLETED == resp.getFinishReason())
					|| (FinishReason.TRUNCATED == resp.getFinishReason()));

			s.setTopK(null);
			s.setTopP(0.2);
			s.setTemperature(null);
			s.setMaxNewTokens(40);
			resp = s.chat("Name a mammal.");
			assertTrue((FinishReason.COMPLETED == resp.getFinishReason())
					|| (FinishReason.TRUNCATED == resp.getFinishReason()));

			s.setTopK(null);
			s.setTopP(null);
			s.setTemperature(20.0);
			s.setMaxNewTokens(40);
			resp = s.complete("Name a mammal.");
			assertTrue((FinishReason.COMPLETED == resp.getFinishReason())
					|| (FinishReason.TRUNCATED == resp.getFinishReason()));
		}
	}

	@DisplayName("Test image URLs in messages.")
	@ParameterizedTest
	@MethodSource("imageServices")
	@EnabledIf("hasImageServices")
	public void testImgUrls(Pair<AiEndpoint, String> p) throws Exception {

		try (ChatService svc = p.getLeft().getChatService(p.getRight())) {

			// Uses an image as input.
			ChatMessage msg = new ChatMessage("Is there any grass in this image?");
			msg.addPart(FilePart.fromUrl(
					"https://upload.wikimedia.org/wikipedia/commons/thumb/d/dd/Gfp-wisconsin-madison-the-nature-boardwalk.jpg/2560px-Gfp-wisconsin-madison-the-nature-boardwalk.jpg",
					"image/jpeg"));
			ChatCompletion resp = svc.chat(msg);
			System.out.println(resp.getText());
			assertEquals(FinishReason.COMPLETED, resp.getFinishReason());
			assertTrue(resp.getText().toUpperCase().contains("YES"));

			// Sends another message to show that the image is kept in history correctly
			resp = svc.chat("What material the pathway in the picture is made of?");
			System.out.println(resp.getText());
			assertEquals(FinishReason.COMPLETED, resp.getFinishReason());
			assertTrue(resp.getText().toUpperCase().contains("WOOD"));

		} // Close endpoint
	}

	@DisplayName("Test image in messages.")
	@ParameterizedTest
	@MethodSource("imageServices")
	@EnabledIf("hasImageServices")
	public void testImgFiles(Pair<AiEndpoint, String> p) throws Exception {

		try (ChatService svc = p.getLeft().getChatService(p.getRight())) {

			// Uses an image as input.
			ChatMessage msg = new ChatMessage("Is there any grass in this image?");
			msg.addPart(new FilePart(ResourceUtil.getResourceFile("Gfp-wisconsin-madison-the-nature-boardwalk-LOW.png"),
					"image/png"));
			ChatCompletion resp = svc.chat(msg);
			System.out.println(resp.getText());
			assertEquals(FinishReason.COMPLETED, resp.getFinishReason());
			assertTrue(resp.getText().toUpperCase().contains("YES"));

			// Sends another message to show that the image is kept in history correctly
			resp = svc.chat("What material the pathway in the picture is made of?");
			System.out.println(resp.getText());
			assertEquals(FinishReason.COMPLETED, resp.getFinishReason());
			assertTrue(resp.getText().toUpperCase().contains("WOOD"));

		} // Close endpoint
	}

}