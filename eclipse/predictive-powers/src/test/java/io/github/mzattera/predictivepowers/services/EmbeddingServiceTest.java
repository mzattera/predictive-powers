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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

import org.apache.tika.exception.TikaException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.github.mzattera.predictivepowers.AiEndpoint;
import io.github.mzattera.predictivepowers.huggingface.endpoint.HuggingFaceEndpoint;
import io.github.mzattera.predictivepowers.huggingface.services.HuggingFaceEmbeddingService;
import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;
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

	private static AiEndpoint[] a;

	@BeforeAll
	static void init() {
		a = new AiEndpoint[] { new OpenAiEndpoint(), new HuggingFaceEndpoint() };
	}

	@AfterAll
	static void tearDown() {
		for (AiEndpoint ep : a)
			try {
				ep.close();
			} catch (Exception e) {
			}
	}

	/** @return List of endpoints which services must be tested. */
	static Stream<AiEndpoint> endpoints() {
		return Arrays.stream(a);
	}

	/**
	 * Tests embedding serialization.
	 * 
	 * @throws JsonProcessingException
	 */
	@Test
	public void testEmbeddedText() throws JsonProcessingException {
		List<Double> d = new ArrayList<>();
		d.add(1.0);
		d.add(2.0);
		d.add(3.0);
		EmbeddedText e = EmbeddedText.builder().text("This is the text for the embedding.").embedding(d)
				.model("banana_model").build();
		e.set("string", "a string");
		e.set("number", 42);

		String json = OpenAiClient.getJsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(e);
		e = OpenAiClient.getJsonMapper().readValue(json, EmbeddedText.class);
		String json2 = OpenAiClient.getJsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(e);
		assertEquals(json, json2);
	}

	// TODO URGENT ? add tests using embedding windows, check length and size of the
	// returned pieces too

	@ParameterizedTest
	@MethodSource("endpoints")
	public void test01(AiEndpoint ep) {
		EmbeddingService es = ep.getEmbeddingService();
		Random rnd = new Random();

		List<String> test = new ArrayList<>();
		test.add("La somma delle parti e' maggiore del tutto");
		test.add("Una tigre corre nella foresta.");
		test.add("Non esistono numeri primi minori di 1");
		test.add("Giove e' il quinto pianeta del sistema solare");

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

	@ParameterizedTest
	@MethodSource("endpoints")
	public void test02(AiEndpoint ep) {
		EmbeddingService es = ep.getEmbeddingService();
		if (es instanceof HuggingFaceEmbeddingService)
			return; // TODO it seems the tokenizer always returns 128, regardless input size; this
					// might affect other aspects, to be investigated

		Tokenizer counter = es.getEndpoint().getModelService().getTokenizer(es.getModel());
		es.setDefaultTextTokens(10);

		StringBuilder txt = new StringBuilder();
		while (counter.count(txt.toString()) <= es.getDefaultTextTokens()) {
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
	@ParameterizedTest
	@MethodSource("endpoints")
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
	@ParameterizedTest
	@MethodSource("endpoints")
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
	 * @throws URISyntaxException
	 */
	@ParameterizedTest
	@MethodSource("endpoints")
	public void test05(AiEndpoint ep)
			throws MalformedURLException, IOException, SAXException, TikaException, URISyntaxException {
		EmbeddingService es = ep.getEmbeddingService();
		es.embedURL("https://en.wikipedia.org/wiki/Alan_Turing");
	}

	/**
	 * Getters and setters
	 */
	@ParameterizedTest
	@MethodSource("endpoints")
	public void test06(AiEndpoint ep) {
		EmbeddingService s = ep.getEmbeddingService();

		String m = s.getModel();
		assertNotNull(m);
		s.setModel("pippo");
		assertEquals("pippo", s.getModel());
		s.setModel(m);
	}
}