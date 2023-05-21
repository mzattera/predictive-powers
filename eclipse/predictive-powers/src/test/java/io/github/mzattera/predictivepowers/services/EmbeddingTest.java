/**
 * 
 */
package io.github.mzattera.predictivepowers.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.tika.exception.TikaException;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.util.TokenUtil;
import io.github.mzattera.util.ExtractionUtil;
import io.github.mzattera.util.ResourceUtil;

/**
 * Test the OpenAi embedding service.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class EmbeddingTest {

	@Test
	public void test01() {
		Random rnd = new Random();
		OpenAiEndpoint ep = OpenAiEndpoint.getInstance();
		EmbeddingService es = ep.getEmbeddingService();

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
		for (int i = 0; i < 1024 * 8 * 2 / 5; i++)
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

	@Test
	public void test02() {

		OpenAiEndpoint ep = OpenAiEndpoint.getInstance();
		EmbeddingService es = ep.getEmbeddingService();

		StringBuilder txt = new StringBuilder();
		while (TokenUtil.count(txt.toString()) < 10_000)
			txt.append("Banana! ");

		List<EmbeddedText> resp = es.embed(txt.toString());
		assertTrue(resp.size() > 1);
	}

	@Test
	public void test03() throws IOException, SAXException, TikaException {

		OpenAiEndpoint ep = OpenAiEndpoint.getInstance();
		EmbeddingService es = ep.getEmbeddingService();

		List<EmbeddedText> base = es.embed("banana");
		assertEquals(1, base.size());

		File f = ResourceUtil.getResourceFile("banana.txt");
		assertEquals("banana", ExtractionUtil.fromFile(f));
		List<EmbeddedText> test = es.embedFile(f);
		assertEquals(1, test.size());

		assertEquals(base.get(0).getEmbedding().size(), test.get(0).getEmbedding().size());
		for (int i = 0; i < base.get(0).getEmbedding().size(); ++i)
			assertEquals(base.get(0).getEmbedding().get(i), test.get(0).getEmbedding().get(i));
	}
}