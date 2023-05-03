/**
 * 
 */
package io.github.mzattera.predictivepowers.applications;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.knowledge.KnowledgeBase;
import io.github.mzattera.predictivepowers.services.EmbeddedText;
import io.github.mzattera.predictivepowers.services.EmbeddingService;
import io.github.mzattera.predictivepowers.services.QnAPair;
import io.github.mzattera.predictivepowers.services.QuestionAnsweringService;
import io.github.mzattera.util.ResourceUtil;

public class OracleTest {

	/**
	 * Makes some tests by reading a knowledge base and asking a relevant question
	 * using the question answering service.
	 * 
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	@Test
	public void test01() throws ClassNotFoundException, IOException {
		OpenAiEndpoint ep = OpenAiEndpoint.getInstance();
		EmbeddingService es = ep.getEmbeddingService();
		QuestionAnsweringService qas = ep.getQuestionAnsweringService();
		KnowledgeBase kb = KnowledgeBase.load(ResourceUtil.getResourceFile("kb_banana.object"));

		String question = "What is that you like?";
		List<Pair<EmbeddedText, Double>> context = kb.search(es.embed(question).get(0), 50, 0);
		QnAPair answer = qas.answerWithEmbeddings(question, context);

		assertTrue(answer.getAnswer().toLowerCase().contains("banana"));
	}
}
