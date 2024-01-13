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

package io.github.mzattera.predictivepowers.services;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.mzattera.predictivepowers.huggingface.endpoint.HuggingFaceEndpoint;
import io.github.mzattera.predictivepowers.huggingface.services.HuggingFaceCompletionService;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiCompletionService;
import io.github.mzattera.predictivepowers.services.ChatMessage.Author;
import io.github.mzattera.predictivepowers.services.TextCompletion.FinishReason;

/**
 * @author Massimiliano "Maxi" Zattera.
 */
public class CompletionServiceTest {

	// TODO break it in smaller tests...
	// TODO URGENT test02() messes up params before test03(); change order

	private static OpenAiEndpoint oaiEp;
	private static HuggingFaceEndpoint hfEp;

	@BeforeAll
	static void setUpBeforeAll() {
		oaiEp = new OpenAiEndpoint();
		hfEp = new HuggingFaceEndpoint();
	}

	@AfterAll
	static void tearDownAfterAll() {
		oaiEp.close();
		hfEp.close();
	}

	// Provides the services to test
	static Stream<CompletionService> instanceProvider() {
		return Stream.of(oaiEp.getCompletionService(), hfEp.getCompletionService());
	}

	/**
	 * Basic completion.
	 */
	@ParameterizedTest
	@MethodSource("instanceProvider")
	void test01(CompletionService s) {
		TextCompletion resp = s.complete("Name a mammal.");
		assertTrue(resp.getFinishReason() == FinishReason.COMPLETED);

		s.setMaxNewTokens(1);
		resp = s.complete("Name a mammal.");
		if (s instanceof OpenAiCompletionService) {
			assertTrue(resp.getFinishReason() == FinishReason.TRUNCATED);
		} else {
			assertTrue(resp.getFinishReason() == FinishReason.COMPLETED);
		}
	}

	/**
	 * Getters and setters
	 */
	@ParameterizedTest
	@MethodSource("instanceProvider")
	void test02(CompletionService s) {
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

	/**
	 * insertion.
	 */
	@ParameterizedTest
	@MethodSource("instanceProvider")
	void test03(CompletionService s) {
		assertThrows(UnsupportedOperationException.class, () -> s.insert("Mount Everest is ", " meters high."));
	}

	/**
	 * Getters and setters
	 */
	@ParameterizedTest
	@MethodSource("instanceProvider")
	void test04(CompletionService s) {

		// Check response contains all parameters?

		if (!(s instanceof OpenAiCompletionService)) {
			s.setTopK(5);
		}
		s.setTopP(null);
		s.setTemperature(null);
		s.setMaxNewTokens(40);
		s.setEcho(true);
		TextCompletion resp = s.complete("Name a mammal.");
		assertTrue((resp.getFinishReason() == FinishReason.COMPLETED)||(resp.getFinishReason() == FinishReason.TRUNCATED));

		s.setTopK(null);
		s.setTopP(0.2);
		s.setTemperature(null);
		s.setMaxNewTokens(40);
		s.setEcho(false);
		resp = s.complete("Name a mammal.");
		assertTrue((resp.getFinishReason() == FinishReason.COMPLETED)||(resp.getFinishReason() == FinishReason.TRUNCATED));

		s.setTopK(null);
		s.setTopP(null);
		s.setTemperature(20.0);
		s.setMaxNewTokens(40);
		s.setEcho(true);
		resp = s.complete("Name a mammal.");
		assertTrue((resp.getFinishReason() == FinishReason.COMPLETED)||(resp.getFinishReason() == FinishReason.TRUNCATED));
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
		assertEquals("assistant", CompletionService.fillSlots("{{C}}", params));
		assertEquals(" a.b assistant", CompletionService.fillSlots("{{A}} {{A.B}} {{C}}", params));
		assertEquals(" a.b assistant {{D}}", CompletionService.fillSlots("{{A}} {{A.B}} {{C}} {{D}}", params));
		assertEquals(" a.b assistant {{D}} a.b assistant {{D}}",
				CompletionService.fillSlots("{{A}} {{A.B}} {{C}} {{D}}{{A}} {{A.B}} {{C}} {{D}}", params));
	}
}