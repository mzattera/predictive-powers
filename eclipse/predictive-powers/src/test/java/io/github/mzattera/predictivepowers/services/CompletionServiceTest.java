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

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.huggingface.endpoint.HuggingFaceEndpoint;
import io.github.mzattera.predictivepowers.huggingface.services.HuggingFaceCompletionService;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiCompletionService;
import io.github.mzattera.predictivepowers.services.TextCompletion.FinishReason;

/**
 * @author Massimiliano "Maxi" Zattera.
 */
public class CompletionServiceTest {

	// TODO break it in smaller tests...

	@Test
	public void test01() {

		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {
			test01(ep.getCompletionService());
			test02(ep.getCompletionService());
			test03(ep.getCompletionService());
			test04(ep.getCompletionService());
		}

		try (HuggingFaceEndpoint ep = new HuggingFaceEndpoint()) {
			test01(ep.getCompletionService());
			test02(ep.getCompletionService());
			test03(ep.getCompletionService());
			test04(ep.getCompletionService());
		}
	}

	/**
	 * Basic completion.
	 */
	private void test01(CompletionService s) {
		TextCompletion resp = s.complete("Name a mammal.");
		assertTrue((resp.getFinishReason() == FinishReason.OK) || (resp.getFinishReason() == FinishReason.COMPLETED));

		s.setMaxNewTokens(1);
		resp = s.complete("Name a mammal.");
		if (s instanceof OpenAiCompletionService) {
			assertTrue(resp.getFinishReason() == FinishReason.LENGTH_LIMIT_REACHED);
		} else {
			assertTrue(resp.getFinishReason() == FinishReason.OK);
		}
	}

	/**
	 * Getters and setters
	 */
	private void test02(CompletionService s) {
		String m = s.getModel();
		assertNotNull(m);
		s.setModel("pippo");
		assertEquals("pippo", s.getModel());
		s.setModel(m);

		if (s instanceof OpenAiCompletionService) {
			assertThrows(UnsupportedOperationException.class, () -> s.setTopK(1));
			assertThrows(UnsupportedOperationException.class, () -> s.getTopK());
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
	private void test03(CompletionService s) {
		if (s instanceof HuggingFaceCompletionService) {
			assertThrows(UnsupportedOperationException.class, () -> s.insert("Mount Everest is ", " meters high."));
		} else {
			TextCompletion resp = s.insert("Mount Everest is ", " meters high.");
			assertTrue(
					(resp.getFinishReason() == FinishReason.OK) || (resp.getFinishReason() == FinishReason.COMPLETED));
			assertTrue(resp.getText().contains("8"));
		}
	}

	/**
	 * Getters and setters
	 */
	private void test04(CompletionService s) {

		// Check response contains all parameters?
		
		if (!(s instanceof OpenAiCompletionService)) {
			s.setTopK(5);
		}
		s.setTopP(null);
		s.setTemperature(null);
		s.setMaxNewTokens(40);
		s.setEcho(true);
		TextCompletion resp = s.complete("Name a mammal.");
		assertTrue((resp.getFinishReason() == FinishReason.OK) || (resp.getFinishReason() == FinishReason.COMPLETED));
		
		s.setTopK(null);
		s.setTopP(0.2);
		s.setTemperature(null);
		s.setMaxNewTokens(40);
		s.setEcho(false);
		resp = s.complete("Name a mammal.");
		assertTrue((resp.getFinishReason() == FinishReason.OK) || (resp.getFinishReason() == FinishReason.COMPLETED));

		s.setTopK(null);
		s.setTopP(null);
		s.setTemperature(20.0);
		s.setMaxNewTokens(40);
		s.setEcho(true);
		resp = s.complete("Name a mammal.");
		assertTrue((resp.getFinishReason() == FinishReason.OK) || (resp.getFinishReason() == FinishReason.COMPLETED));
	}
}