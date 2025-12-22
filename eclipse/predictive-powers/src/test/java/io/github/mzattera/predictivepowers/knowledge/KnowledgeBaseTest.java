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

package io.github.mzattera.predictivepowers.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import io.github.mzattera.predictivepowers.TestConfiguration;
import io.github.mzattera.predictivepowers.openai.services.OpenAiEmbeddingService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.EmbeddedText;
import io.github.mzattera.predictivepowers.util.ResourceUtil;

/**
 * Test the OpenAI embedding service.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class KnowledgeBaseTest {

	// Must be static unless using @TestInstance(Lifecycle.PER_CLASS)
	static boolean hasServices() {
		return TestConfiguration.TEST_KNOWLEDGE_BASE;
	}

	@Test
	@DisplayName("Insert some strings in the KB and checks search finds them.")
	@EnabledIf("hasServices")
	public void testSearch() {

		try (OpenAiEndpoint ep = new OpenAiEndpoint();
				OpenAiEmbeddingService es = ep.getEmbeddingService();
				KnowledgeBase kb = new KnowledgeBase();) {

			// Do not use saved version here, as we still want to test embedding size.
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

	private static class IndexMatcher implements EmbeddedTextMatcher {

		private final int idx;

		IndexMatcher(int idx) {
			this.idx = idx;
		}

		@Override
		public boolean match(EmbeddedText e) {
			return e.get("index").equals(idx);
		}
	}

	@Test
	@DisplayName("Matcher tests.")
	@EnabledIf("hasServices")
	public void testMatcher() throws ClassNotFoundException, IOException {

		try (KnowledgeBase kb = KnowledgeBase.load(ResourceUtil.getResourceFile("kb.object"))) {

			for (int i = 0; i < savedText.size(); ++i) {
				EmbeddedTextMatcher m = new IndexMatcher(i);
				List<EmbeddedText> rs = kb.query(m);
				assertEquals(1, rs.size());
				assertEquals(savedText.get(i), rs.get(0).getText());
			}

			for (int i = 0; i < savedText.size(); ++i) {
				EmbeddedTextMatcher m = new IndexMatcher(i);
				List<EmbeddedText> rs = kb.query("test", m);
				assertEquals(1, rs.size());
				assertEquals(savedText.get(i), rs.get(0).getText());
			}

			for (int i = 0; i < savedText.size(); ++i) {
				EmbeddedTextMatcher m = new IndexMatcher(i);
				List<EmbeddedText> rs = kb.query(KnowledgeBase.DEFAULT_DOMAIN, m);
				assertEquals(0, rs.size());
			}
		}
	}

	@Test
	@DisplayName("Test using text instead of embeddings.")
	@EnabledIf("hasServices")
	public void test03() {

		try (OpenAiEndpoint ep = new OpenAiEndpoint();
				OpenAiEmbeddingService es = ep.getEmbeddingService();
				KnowledgeBase kb = new KnowledgeBase();) {

			kb.createDomain("test");
			kb.createDomain("other");

			// Insert using embeddings
			for (int i = 0; i < savedText.size(); ++i) {
				List<EmbeddedText> resp = es.embed(savedText.get(i));
				assertEquals(1, resp.size());
				resp.get(0).set("index", i);
				kb.insert("test", resp.get(0));
			}

			// Delete using text
			EmbeddedTextMatcher m = new IndexMatcher(0);
			assertEquals(1, kb.query(m).size());
			kb.delete(savedText.get(0));
			assertEquals(0, kb.query(m).size());

			// Delete from specific group using text
			m = new IndexMatcher(1);
			assertEquals(1, kb.query(m).size());
			kb.delete(KnowledgeBase.DEFAULT_DOMAIN, savedText.get(1));
			assertEquals(1, kb.query(m).size());

			// Delete from specific group using text
			kb.delete("test", savedText.get(1));
			assertEquals(0, kb.query(m).size());

			// Delete using matcher
			m = new IndexMatcher(2);
			assertEquals(1, kb.query(m).size());
			kb.delete(m);
			assertEquals(0, kb.query(m).size());

			// Delete from specific group using text
			m = new IndexMatcher(3);
			assertEquals(1, kb.query(m).size());
			kb.delete(KnowledgeBase.DEFAULT_DOMAIN, m);
			assertEquals(1, kb.query(m).size());

			// Delete from specific group using text
			kb.delete("test", m);
			assertEquals(0, kb.query(m).size());
		}
	}

	@Test
	@DisplayName("Test null parameters.")
	@EnabledIf("hasServices")
	public void testNullParameters() {

		try (OpenAiEndpoint ep = new OpenAiEndpoint();
				OpenAiEmbeddingService es = ep.getEmbeddingService();
				KnowledgeBase kb = new KnowledgeBase();) {

			List<EmbeddedText> resp = es.embed(savedText.get(0));

			assertThrows(java.lang.NullPointerException.class, () -> kb.createDomain((String) null));

			assertThrows(java.lang.NullPointerException.class, () -> kb.insert((EmbeddedText) null));
			assertThrows(java.lang.NullPointerException.class, () -> kb.insert(null, resp.get(0)));
			assertThrows(java.lang.NullPointerException.class, () -> kb.insert((Collection<EmbeddedText>) null));
			assertThrows(java.lang.NullPointerException.class, () -> kb.insert(null, resp));
			assertThrows(java.lang.NullPointerException.class, () -> kb.insert(null, (EmbeddedText) null));
			assertThrows(java.lang.NullPointerException.class, () -> kb.insert(null, (Collection<EmbeddedText>) null));

			assertThrows(java.lang.NullPointerException.class, () -> kb.delete((EmbeddedText) null));
			assertThrows(java.lang.NullPointerException.class, () -> kb.delete(null, resp.get(0)));
			assertThrows(java.lang.NullPointerException.class, () -> kb.delete(null, (EmbeddedText) null));

			kb.createDomain("test");
			kb.createDomain("test");
		}
	}

	private final static List<String> savedText = new ArrayList<>();
	static {
		savedText.add("La somma delle parti e' maggiore del tutto");
		savedText.add("Una tigre corre nella foresta.");
		savedText.add("Non esistono numeri primi minori di 1");
		savedText.add("Giove e' il quinto pianeta del sistema solare");
	}

	/**
	 * Creates and save the KB used in tests.
	 * 
	 * @param args
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void main(String args[]) throws FileNotFoundException, IOException {

		try (OpenAiEndpoint ep = new OpenAiEndpoint();
				OpenAiEmbeddingService es = ep.getEmbeddingService();
				KnowledgeBase kb = new KnowledgeBase();) {

			kb.createDomain("test");

			for (int i = 0; i < savedText.size(); ++i) {
				List<EmbeddedText> resp = es.embed(savedText.get(i));
				assertEquals(1, resp.size());
				resp.get(0).set("index", i);
				kb.insert("test", resp.get(0));
			}

			kb.save("D:\\kb.object");
		}
	}
}