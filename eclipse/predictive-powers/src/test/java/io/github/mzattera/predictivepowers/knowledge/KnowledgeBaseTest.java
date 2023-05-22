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

/**
 * 
 */
package io.github.mzattera.predictivepowers.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.EmbeddedText;
import io.github.mzattera.predictivepowers.services.EmbeddingService;

/**
 * Test the OpenAi embedding service.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class KnowledgeBaseTest {

	/**
	 * Insert some strings in the KB and checks search finds them.
	 */
	@Test
	public void test01() {
		Random rnd = new Random();
		OpenAiEndpoint ep = OpenAiEndpoint.getInstance();
		EmbeddingService es = ep.getEmbeddingService();
		KnowledgeBase kb = new KnowledgeBase();

		List<String> test = new ArrayList<>();
		test.add("La somma delle parti e' maggiore del tutto");
		test.add("Una tigre corre nella foresta.");
		test.add("Non esistono numeri primi minori di 1");
		test.add("Giove e' il quinto pianeta del sistema solare");

		for (String s : test) {
			List<EmbeddedText> resp = es.embed(s);
			assertEquals(1, resp.size());
			kb.insert(resp);
		}

		// Used default domain
		for (int i = 0; i < 10; i++)
			kb.insert(es.embed("Frase numero " + i));

		for (int i = 0; i < test.size(); ++i) {
			String target = test.get(i);
			List<Pair<EmbeddedText, Double>> result = kb.search(es.embed(target).get(0), 1, 0);
			assertEquals(target, result.get(0).getLeft().getText());
		}

		// delete all the strings and make sure we do not find any
		for (String s : test) {
			List<EmbeddedText> resp = es.embed(s);
			kb.delete(resp.get(0));
		}
		for (int i = 0; i < test.size(); ++i) {
			String target = test.get(i);
			List<Pair<EmbeddedText, Double>> result = kb.search(es.embed(target).get(0), 1, 0);
			assertFalse(target.equals(result.get(0).getLeft().getText()));
		}

		// Do the same for a new domain
		kb.createDomain("other");
		kb.createDomain("domain");
		for (String s : test) {
			List<EmbeddedText> resp = es.embed(s);
			kb.insert("domain", resp);
		}

		for (int i = 0; i < test.size(); ++i) {
			String target = test.get(i);
			List<Pair<EmbeddedText, Double>> result = kb.search("domain", es.embed(target).get(0), 1, 0);
			assertEquals(target, result.get(0).getLeft().getText());
			result = kb.search(es.embed(target).get(0), 1, 0);
			assertEquals(target, result.get(0).getLeft().getText());
			result = kb.search("other", es.embed(target).get(0), 50, 0);
			assertEquals(0, result.size());
		}

		// Delete domains and make sure they are
		kb.dropDomain("domain");
		kb.dropDomain("other");
		List<String> domains = kb.listDomains();
		assertEquals(1, domains.size());
		assertEquals(KnowledgeBase.DEFAULT_DOMAIN, domains.get(0));
	}
}