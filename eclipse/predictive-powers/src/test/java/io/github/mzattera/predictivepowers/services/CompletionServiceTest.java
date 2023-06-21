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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.huggingface.endpoint.HuggingFaceEndpoint;
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
			doTests(ep.getCompletionService());
		}

		try (HuggingFaceEndpoint ep = new HuggingFaceEndpoint()) {
			doTests(ep.getCompletionService());
		}
	}

	private void doTests(CompletionService s) {
		testSetGet(s);

		TextCompletion resp = s.complete("Name a mammal.");
		assertTrue((resp.getFinishReason() == FinishReason.OK) || (resp.getFinishReason() == FinishReason.COMPLETED));
		
		System.out.println(resp.getText());
	}

	/**
	 * @param s
	 */
	private static void testSetGet(CompletionService s) {
		String m = s.getModel();
		assertNotNull(m);
		s.setModel("pippo");
		assertEquals("pippo", s.getModel());
		s.setModel(m);

		if (!(s instanceof OpenAiCompletionService)) {
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
	}
}