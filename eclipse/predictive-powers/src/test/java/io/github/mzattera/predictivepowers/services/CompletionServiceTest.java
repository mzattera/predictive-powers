/*
 * Copyright 2023-2024 Massimiliano "Maxi" Zattera
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.mzattera.predictivepowers.AiEndpoint;
import io.github.mzattera.predictivepowers.TestConfiguration;
import io.github.mzattera.predictivepowers.huggingface.services.HuggingFaceCompletionService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiCompletionService;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage.Author;
import io.github.mzattera.predictivepowers.services.messages.FinishReason;
import io.github.mzattera.predictivepowers.services.messages.TextCompletion;

/**
 * @author Massimiliano "Maxi" Zattera.
 */
public class CompletionServiceTest {

	private static List<Pair<AiEndpoint, String>> svcs;

	@BeforeAll
	static void init() {
		svcs = TestConfiguration.getCompletionServices();
	}

	@AfterAll
	static void tearDown() {
		TestConfiguration.close(svcs.stream().map(p -> p.getLeft()).collect(Collectors.toList()));
	}

	static Stream<Pair<AiEndpoint, String>> services() {
		return svcs.stream();
	}

	@DisplayName("Basic completion.")
	@ParameterizedTest
	@MethodSource("services")
	void test01(Pair<AiEndpoint, String> p) throws Exception {
		try (CompletionService s = p.getLeft().getCompletionService(p.getRight())) {
			TextCompletion resp = s.complete("Name a mammal.");
			assertTrue(resp.getStatus() == FinishReason.COMPLETED);

			s.setMaxNewTokens(1);
			resp = s.complete("Name a mammal.");
			if (s instanceof HuggingFaceCompletionService)
				assertTrue(resp.getStatus() == FinishReason.COMPLETED);
			else
				assertTrue(resp.getStatus() == FinishReason.TRUNCATED);
		}
	}

	@DisplayName("Getters and setters.")
	@ParameterizedTest
	@MethodSource("services")
	void test02(Pair<AiEndpoint, String> p) throws Exception {
		try (CompletionService s = p.getLeft().getCompletionService(p.getRight())) {
			String m = s.getModel();
			assertNotNull(m);
			s.setModel("pippo");
			assertEquals("pippo", s.getModel());
			s.setModel(m);

			if (s instanceof OpenAiCompletionService) {
				assertNull(s.getTopK());
				s.setTopK(null);
				assertNull(s.getTopK());
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
			s.setMaxNewTokens(15);

			assertFalse(s.getEcho());
			s.setEcho(true);
			assertTrue(s.getEcho());
			s.setEcho(false);
			assertFalse(s.getEcho());

			if (s instanceof OpenAiCompletionService) {
				((OpenAiCompletionService) s).getDefaultReq().setEcho(null);
				assertFalse(s.getEcho());
			}
			if (s instanceof HuggingFaceCompletionService) {
				((HuggingFaceCompletionService) s).getDefaultReq().getParameters().setReturnFullText(null);
				assertFalse(s.getEcho());
			}
		}
	}

	@DisplayName("Insertion.")
	@ParameterizedTest
	@MethodSource("services")
	void test03(Pair<AiEndpoint, String> p) throws Exception {
		try (CompletionService s = p.getLeft().getCompletionService(p.getRight())) {
			assertThrows(UnsupportedOperationException.class, () -> s.insert("Mount Everest is ", " meters high."));
		}
	}

	@DisplayName("Getters and setters.")
	@ParameterizedTest
	@MethodSource("services")
	void test04(Pair<AiEndpoint, String> p) throws Exception {

		try (CompletionService s = p.getLeft().getCompletionService(p.getRight())) {

			// TODO Check response contains all parameters?

			if (!(s instanceof OpenAiCompletionService)) {
				s.setTopK(5);
			}
			s.setTopP(null);
			s.setTemperature(null);
			s.setMaxNewTokens(40);
			s.setEcho(true);
			TextCompletion resp = s.complete("Name a mammal.");
			assertTrue((resp.getStatus() == FinishReason.COMPLETED) || (resp.getStatus() == FinishReason.TRUNCATED));

			s.setTopK(null);
			s.setTopP(0.2);
			s.setTemperature(null);
			s.setMaxNewTokens(40);
			s.setEcho(false);
			resp = s.complete("Name a mammal.");
			assertTrue((resp.getStatus() == FinishReason.COMPLETED) || (resp.getStatus() == FinishReason.TRUNCATED));

			s.setTopK(null);
			s.setTopP(null);
			s.setTemperature(20.0);
			s.setMaxNewTokens(40);
			s.setEcho(true);
			resp = s.complete("Name a mammal.");
			assertTrue((resp.getStatus() == FinishReason.COMPLETED) || (resp.getStatus() == FinishReason.TRUNCATED));
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/// Slot filling
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////// //////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test
	void test90() {
		Map<String, Object> params = new HashMap<>();
		params.put("A", null);
		params.put("A.B", "a.b");
		params.put("C", Author.BOT);

		assertEquals(null, CompletionService.fillSlots(null, new HashMap<>()));
		assertEquals("banana", CompletionService.fillSlots("banana", null));
		assertEquals("", CompletionService.fillSlots("{{A}}", params));
		assertEquals("a.b", CompletionService.fillSlots("{{A.B}}", params));
		assertEquals("bot", CompletionService.fillSlots("{{C}}", params));
		assertEquals(" a.b bot", CompletionService.fillSlots("{{A}} {{A.B}} {{C}}", params));
		assertEquals(" a.b bot {{D}}", CompletionService.fillSlots("{{A}} {{A.B}} {{C}} {{D}}", params));
		assertEquals(" a.b bot {{D}} a.b bot {{D}}",
				CompletionService.fillSlots("{{A}} {{A.B}} {{C}} {{D}}{{A}} {{A.B}} {{C}} {{D}}", params));
	}
}