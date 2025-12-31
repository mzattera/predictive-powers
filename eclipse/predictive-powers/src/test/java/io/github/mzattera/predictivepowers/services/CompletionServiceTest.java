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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import io.github.mzattera.predictivepowers.BadRequestException;
import io.github.mzattera.predictivepowers.EndpointException;
import io.github.mzattera.predictivepowers.TestConfiguration;
import io.github.mzattera.predictivepowers.ollama.OllamaCompletionService;
import io.github.mzattera.predictivepowers.openai.OpenAiCompletionService;
import io.github.mzattera.predictivepowers.services.messages.FinishReason;
import io.github.mzattera.predictivepowers.services.messages.TextCompletion;

/**
 * Tests generic
 * 
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

	static boolean hasServices() {
		return services().findAny().isPresent();
	}

	@DisplayName("Basic completion.")
	@ParameterizedTest
	@MethodSource("services")
	@EnabledIf("hasServices")
	void test01(Pair<AiEndpoint, String> p) throws Exception {
		try (CompletionService s = p.getLeft().getCompletionService(p.getRight())) {
			s.setTemperature(0.0);

			TextCompletion resp = s.complete("Name a mammal.");
			assertTrue((resp.getFinishReason() == FinishReason.COMPLETED)
					|| (resp.getFinishReason() == FinishReason.TRUNCATED));
		}
	}

	@DisplayName("Getters and setters.")
	@ParameterizedTest
	@MethodSource("services")
	@EnabledIf("hasServices")
	void test02(Pair<AiEndpoint, String> p) throws Exception {
		try (CompletionService s = p.getLeft().getCompletionService(p.getRight())) {
			s.setTemperature(0.0);

			String m = s.getModel();
			assertNotNull(m);
			s.setModel("pippo");
			assertEquals("pippo", s.getModel());
			s.setModel(m);

			assertTrue(p.getLeft() == s.getEndpoint());

			if (s instanceof OpenAiCompletionService) {
				assertThrows(EndpointException.class, () -> s.setTopK(1));
			} else {
				s.setTopK(1);
				assertEquals(1, s.getTopK());
			}
			s.setTopK(null);
			assertNull(s.getTopK());

			s.setTopP(2.0);
			assertEquals(2.0, s.getTopP());
			s.setTopP(null);
			assertNull(s.getTopP());

			s.setTemperature(3.0);
			assertEquals(3.0, Math.round(s.getTemperature() * 10000.0) / 10000.0);
			s.setTemperature(null);
			assertNull(s.getTemperature());
			s.setTemperature(1.0);

			s.setMaxNewTokens(4);
			assertEquals(4, s.getMaxNewTokens());
			s.setMaxNewTokens(null);
			assertNull(s.getMaxNewTokens());

			// We assume there is no echo by default
			assertFalse(s.getEcho());
			if (s instanceof OllamaCompletionService) {
				assertThrows(EndpointException.class, () -> s.setEcho(true));
			} else {
				s.setEcho(true);
				assertTrue(s.getEcho());
			}
			s.setEcho(false);
			assertFalse(s.getEcho());
		}
	}

	@DisplayName("Insertion.")
	@ParameterizedTest
	@MethodSource("services")
	@EnabledIf("hasServices")
	void test03(Pair<AiEndpoint, String> p) throws Exception {
		try (CompletionService s = p.getLeft().getCompletionService(p.getRight())) {
			s.setTemperature(0.0);

			if ((s instanceof OpenAiCompletionService) || (s instanceof OllamaCompletionService)) {
				assertThrows(BadRequestException.class, () -> s.insert("Mount Everest is ", " meters high."));
			} else {
				TextCompletion resp = s.insert("Mount Everest is ", " meters high.");
				assertTrue(resp.getText().trim().startsWith("8"));
			}
		}
	}

	@DisplayName("Call exercising all parameters.")
	@ParameterizedTest
	@MethodSource("services")
	@EnabledIf("hasServices")
	void test04(Pair<AiEndpoint, String> p) throws Exception {

		try (CompletionService s = p.getLeft().getCompletionService(p.getRight())) {
			s.setTemperature(0.0);

			TextCompletion resp = null;

			if (s instanceof OpenAiCompletionService) {
				assertThrows(EndpointException.class, () -> s.setTopK(5));
			} else {
				s.setTopK(5);
			}
			s.setTopP(null);
			s.setTemperature(null);
			s.setMaxNewTokens(40);
			if (s instanceof OllamaCompletionService) {
				assertThrows(EndpointException.class, () -> s.setEcho(true));
			} else {
				s.setEcho(true);
				resp = s.complete("Name a mammal.");
				assertTrue((resp.getFinishReason() == FinishReason.COMPLETED)
						|| (resp.getFinishReason() == FinishReason.TRUNCATED));
			}

			s.setTopK(null);
			s.setTopP(0.2);
			s.setTemperature(null);
			s.setMaxNewTokens(40);
			s.setEcho(false);
			resp = s.complete("Name a mammal.");
			assertTrue((resp.getFinishReason() == FinishReason.COMPLETED)
					|| (resp.getFinishReason() == FinishReason.TRUNCATED));

			s.setTopK(null);
			s.setTopP(null);
			s.setTemperature(20.0);
			s.setMaxNewTokens(40);
			if (s instanceof OllamaCompletionService) {
				assertThrows(EndpointException.class, () -> s.setEcho(true));
			} else {
				s.setEcho(true);
				resp = s.complete("Name a mammal.");
				assertTrue((resp.getFinishReason() == FinishReason.COMPLETED)
						|| (resp.getFinishReason() == FinishReason.TRUNCATED));
			}
		}
	}
}