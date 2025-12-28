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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mzattera.predictivepowers.AiEndpoint;
import io.github.mzattera.predictivepowers.EndpointException;
import io.github.mzattera.predictivepowers.TestConfiguration;
import io.github.mzattera.predictivepowers.huggingface.HuggingFaceEmbeddingService;
import io.github.mzattera.predictivepowers.services.ModelService.Tokenizer;
import io.github.mzattera.predictivepowers.util.ExtractionUtil;
import io.github.mzattera.predictivepowers.util.ResourceUtil;

/**
 * Test the OpenAI embedding service.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class EmbeddingServiceTest {

	private final static Logger LOG = LoggerFactory.getLogger(EmbeddingServiceTest.class);

	private static List<Pair<AiEndpoint, String>> svcs;

	@BeforeAll
	static void init() {
		svcs = TestConfiguration.getEmbeddingServices();
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

	// TODO add tests using embedding windows, check length and size of the returned
	// pieces too

	@ParameterizedTest
	@MethodSource("services")
	@EnabledIf("hasServices")
	@DisplayName("Tests retrieval based on embeddings.")
	public void test01(Pair<AiEndpoint, String> p) throws Exception {
		try (EmbeddingService es = p.getLeft().getEmbeddingService(p.getRight())) {
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

			for (int i = 0; i < test.size(); ++i) {

				int bestFit = -1;
				double mostSim = -1.0;
				for (int j = 0; j < testEmb.size(); ++j) {
					double sim = testEmb.get(j).similarity(testEmb.get(i));
					if (sim > mostSim) {
						mostSim = sim;
						bestFit = j;
					}
				}

				assertEquals(test.get(i), testEmb.get(bestFit).getText());
			}
		}
	}

	@ParameterizedTest
	@MethodSource("services")
	@EnabledIf("hasServices")
	@DisplayName("Chunking on overflow.")
	public void test02(Pair<AiEndpoint, String> p) throws Exception {
		try (EmbeddingService es = p.getLeft().getEmbeddingService(p.getRight())) {
			if (es instanceof HuggingFaceEmbeddingService)
				return; // TODO it seems the tokenizer always returns 128, regardless input size; this
						// might affect other aspects, to be investigated

			Tokenizer counter = es.getEndpoint().getModelService().getTokenizer(es.getModel());
			es.setDefaultChunkTokens(10);

			// Writes loooong strings of bananas
			StringBuilder txt = new StringBuilder();
			while (counter.count(txt.toString()) <= es.getDefaultChunkTokens()) {
				txt.append("Banana! ");
			}

			List<EmbeddedText> resp = es.embed(txt.toString());
			assertTrue(resp.size() > 1);
		}
	}

	@DisplayName("Tests that embedding a file content works.")
	@ParameterizedTest
	@MethodSource("services")
	@EnabledIf("hasServices")
	public void test03(Pair<AiEndpoint, String> p) throws Exception {
		try (EmbeddingService es = p.getLeft().getEmbeddingService(p.getRight())) {

			final String banana = "banana";

			List<EmbeddedText> base = es.embed(banana);
			assertEquals(1, base.size());

			File f = ResourceUtil.getResourceFile("banana.txt");
			assertEquals(banana, ExtractionUtil.fromFile(f));

			List<EmbeddedText> test = es.embedFile(f);
			assertEquals(1, test.size());

			assertEquals(base.get(0).getModel(), test.get(0).getModel());
			assertEquals(base.get(0).getEmbedding().size(), test.get(0).getEmbedding().size());
//			for (int i = 0; i < base.get(0).getEmbedding().size(); ++i) {
//				// TODO: SOMETIMES, not always returned vectors are slightly different
//				// org.opentest4j.AssertionFailedError: expected: <-0.013906941> but was:
//				// <-0.013921019>
//				 assertEquals(base.get(0).getEmbedding().get(i), test.get(0).getEmbedding().get(i));
//			}

			// Checks similarity considering rounding
			double similarity = EmbeddedText.similarity(base.get(0), test.get(0));
			LOG.info("Difference in embedding: " + Math.abs(1.0d - similarity));
			assertTrue(Math.abs(1.0d - similarity) < 10e-2);
		}
	}

	@DisplayName("Tests embedding folders recursively.")
	@ParameterizedTest
	@MethodSource("services")
	@EnabledIf("hasServices")
	public void test04(Pair<AiEndpoint, String> p) throws Exception {
		try (EmbeddingService es = p.getLeft().getEmbeddingService(p.getRight())) {

			// Fails when using a file
			assertThrows(EndpointException.class, () -> es.embedFolder(ResourceUtil.getResourceFile("banana.txt")));

			// Fails when using an unexisting older
			assertThrows(EndpointException.class, () -> es.embedFolder(new File("/i-do-not-exist")));

			Map<File, List<EmbeddedText>> base = es.embedFolder(ResourceUtil.getResourceFile("recursion"));
			assertEquals(3, base.size());
		}
	}

	@DisplayName("Tests embedding URL.")
	@ParameterizedTest
	@MethodSource("services")
	@EnabledIf("hasServices")
	public void test05(Pair<AiEndpoint, String> p) throws Exception {
		try (EmbeddingService es = p.getLeft().getEmbeddingService(p.getRight())) {
			es.embedURL("https://en.wikipedia.org/wiki/Alan_Turing");
		}
	}

	@DisplayName("Getters and setters")
	@ParameterizedTest
	@MethodSource("services")
	@EnabledIf("hasServices")
	public void test06(Pair<AiEndpoint, String> p) throws Exception {
		try (EmbeddingService es = p.getLeft().getEmbeddingService(p.getRight())) {

			String m = es.getModel();
			assertNotNull(m);
			es.setModel("pippo");
			assertEquals("pippo", es.getModel());
			es.setModel(m);

			int t = es.getDefaultChunkTokens();
			assertTrue(t > 0);
			es.setDefaultChunkTokens(150);
			assertEquals(150, es.getDefaultChunkTokens());
			assertThrows(IllegalArgumentException.class, () -> es.setDefaultChunkTokens(0));
		}
	}
}