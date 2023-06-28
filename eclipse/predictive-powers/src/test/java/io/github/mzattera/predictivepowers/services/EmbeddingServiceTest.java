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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.tika.exception.TikaException;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import io.github.mzattera.predictivepowers.AiEndpoint;
import io.github.mzattera.predictivepowers.huggingface.endpoint.HuggingFaceEndpoint;
import io.github.mzattera.predictivepowers.huggingface.services.HuggingFaceEmbeddingService;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.ModelService.Tokenizer;
import io.github.mzattera.util.ExtractionUtil;
import io.github.mzattera.util.ResourceUtil;

/**
 * Test the OpenAI embedding service.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class EmbeddingServiceTest {

	// TODO break it in smaller tests...

	@Test
	public void test00() throws IOException, SAXException, TikaException {
		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {
			test01(ep);
			test02(ep);
			test03(ep);
			test04(ep);
			test05(ep);
			test06(ep);
		}
		try (HuggingFaceEndpoint ep = new HuggingFaceEndpoint()) {
			test01(ep);
			test02(ep);
			test03(ep);
			test04(ep);
			test05(ep);
			test06(ep);
		}
	}

	public void test01(AiEndpoint ep) {
		EmbeddingService es = ep.getEmbeddingService();
		Random rnd = new Random();

		List<String> test = new ArrayList<>();
		test.add("La somma delle parti ep.getEmbeddingService()' maggiore del tutto");
		test.add("Una tigre corre nella foresta.");
		test.add("Non esistono numeri primi minori di 1");
		test.add("Giove ep.getEmbeddingService()' il quinto pianeta del sistema solare");

		List<EmbeddedText> testEmb = new ArrayList<>();
		for (String s : test) {
			List<EmbeddedText> resp = es.embed(s);
			assertEquals(1, resp.size());
			testEmb.add(resp.get(0));
		}

		List<String> txt = new ArrayList<>(test);
		for (int i = 0; i < 10; i++)
			txt.add("Frase numero " + i);
		for (int i = 0; i < txt.size(); ++i) {
			txt.add(txt.remove(rnd.nextInt(txt.size())));
		}

		List<EmbeddedText> resp = es.embed(txt);
		assertEquals(txt.size(), resp.size());

		for (int i = 0; i < testEmb.size(); ++i) {

			int bestFit = -1;
			double mostSim = -1.0;
			for (int j = 0; j < txt.size(); ++j) {
				double sim = resp.get(j).similarity(testEmb.get(i));
				if (sim > mostSim) {
					mostSim = sim;
					bestFit = j;
				}
			}

			assertEquals(testEmb.get(i).getText(), resp.get(bestFit).getText());
		}
	}

	public void test02(AiEndpoint ep) {
		EmbeddingService es = ep.getEmbeddingService();
		if (es instanceof HuggingFaceEmbeddingService)
			return; // TODO it seems the tokenizer always returns 128, regardless input size; this
					// might affect other aspects, to be investigated

		Tokenizer counter = es.getEndpoint().getModelService().getTokenizer(es.getModel());
		es.setMaxTextTokens(10);

		StringBuilder txt = new StringBuilder();
		while (counter.count(txt.toString()) <= es.getMaxTextTokens()) {
//			System.out.println(counter.count(txt.toString()) + " " + es.getMaxTextTokens());
			txt.append("Banana! ");
		}

		List<EmbeddedText> resp = es.embed(txt.toString());
		assertTrue(resp.size() > 1);
	}

	/**
	 * Tests that embedding a file content works.
	 * 
	 * @throws IOException
	 * @throws SAXException
	 * @throws TikaException
	 */
	public void test03(AiEndpoint ep) throws IOException, SAXException, TikaException {
		EmbeddingService es = ep.getEmbeddingService();

		final String banana = "banana";

		List<EmbeddedText> base = es.embed(banana);
		assertEquals(1, base.size());

		File f = ResourceUtil.getResourceFile("banana.txt");
		assertEquals(banana, ExtractionUtil.fromFile(f));

		List<EmbeddedText> test = es.embedFile(f);
		assertEquals(1, test.size());

		assertEquals(base.get(0).getModel(), test.get(0).getModel());
		assertEquals(base.get(0).getEmbedding().size(), test.get(0).getEmbedding().size());
		for (int i = 0; i < base.get(0).getEmbedding().size(); ++i) {
			// TODO: SOMETIMES, not always returned vectors are slightly different
			// org.opentest4j.AssertionFailedError: expected: <-0.013906941> but was:
			// <-0.013921019>
//				 assertEquals(base.get(0).getEmbedding().get(i), test.get(0).getEmbedding().get(i));
		}

		// Checks similarity considering rounding
		double similarity = EmbeddedText.similarity(base.get(0), test.get(0));
		assertTrue(Math.abs(1.0d - similarity) < 10e-5);
	}

	/**
	 * Tests embedding folders recursively.
	 * 
	 * @throws TikaException
	 * @throws SAXException
	 * @throws IOException
	 */
	public void test04(AiEndpoint ep) throws IOException, SAXException, TikaException {
		EmbeddingService es = ep.getEmbeddingService();
		Map<File, List<EmbeddedText>> base = es.embedFolder(ResourceUtil.getResourceFile("recursion"));
		assertEquals(3, base.size());
	}

	/**
	 * Tests embedding URL.
	 * 
	 * @throws MalformedURLException
	 * 
	 * @throws TikaException
	 * @throws SAXException
	 * @throws IOException
	 */
	public void test05(AiEndpoint ep) throws MalformedURLException, IOException, SAXException, TikaException {
		EmbeddingService es = ep.getEmbeddingService();
		es.embedURL("https://en.wikipedia.org/wiki/Alan_Turing");
	}

	/**
	 * Getters and setters
	 */
	public void test06(AiEndpoint ep) {
		EmbeddingService s = ep.getEmbeddingService();

		String m = s.getModel();
		assertNotNull(m);
		s.setModel("pippo");
		assertEquals("pippo", s.getModel());
		s.setModel(m);
	}
}