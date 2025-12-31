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

package io.github.mzattera.predictivepowers.openai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
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

import com.openai.models.embeddings.EmbeddingCreateParams;

import io.github.mzattera.predictivepowers.AiEndpoint;
import io.github.mzattera.predictivepowers.TestConfiguration;
import io.github.mzattera.predictivepowers.services.EmbeddedText;
import io.github.mzattera.predictivepowers.services.ModelService.Tokenizer;

/**
 * Test the OpenAI embedding service.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class OpenAiEmbeddingServiceTest {

	private final static Logger LOG = LoggerFactory.getLogger(OpenAiEmbeddingServiceTest.class);

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
		return svcs.stream().filter(p -> (p.getLeft() instanceof OpenAiEndpoint));
	}

	static boolean hasServices() {
		return services().findAny().isPresent();
	}

	// TODO add tests using embedding windows, check length and size of the returned
	// pieces too

	@DisplayName("OpenAI Custom Tests")
	@ParameterizedTest
	@MethodSource("services")
	@EnabledIf("hasServices")
	public void oaiTest(Pair<AiEndpoint, String> p) throws Exception {

		try (OpenAiEmbeddingService es = (OpenAiEmbeddingService) p.getLeft().getEmbeddingService(p.getRight())) {

			String b = "banana ";
			Tokenizer tokenizer = es.getEndpoint().getModelService().getTokenizer(es.getModel());
			int ctx = es.getEndpoint().getModelService().getContextSize(es.getModel());
			es.setDefaultChunkTokens(ctx);
			List<EmbeddedText> res;
			StringBuilder sb = new StringBuilder();

			// A string longer than 8192 and less than 300_000 should be handled in a single
			// call.
			// We have logs for that.
			LOG.info("Test A.");
			while (tokenizer.count(sb.toString()) < (ctx + 10))
				sb.append(b);
			res = es.embed(sb.toString());
			assertEquals(2, res.size());

			// THIS HAS BEEN TESTED AND COMMENTED OUT BECAUSE IT TAKES WAY TOO MUCH TIME
			// A string longer than 300_000 should be handled in two calls.
			// We have logs for that.
			LOG.info("Test B.");
//			while (tokenizer.count(sb.toString()) < (300_000 + 10))
//				sb.append(sb.toString());
//			res = oaies.embed(sb.toString());
////			assertEquals(2, res.size()); // There are more than 2

			// Forces breaking down each word
			es.setDefaultChunkTokens(1);

			// Fewer than 2048+1 strings longer than 8192 and less than 300_000 should be
			// handled in a single call.
			// We have logs for that.
			LOG.info("Test C.");
			sb.setLength(0);
			for (int i = 0; i < 2048; ++i)
				sb.append(b);
			res = es.embed(sb.toString());
			assertEquals(2048, res.size());

			// More than 2048 strings longer than 8192 and less than 300_000 should be
			// handled in a single call.
			// We have logs for that.
			LOG.info("Test D.");
			sb.append(b);
			res = es.embed(sb.toString());
			assertEquals(2049, res.size());

			// Default Request getters and setters
			EmbeddingCreateParams req = es.getDefaultRequest().toBuilder().model("banana").build();
			es.setDefaultRequest(req);
			assertEquals(es.getModel(), "banana");
		}
	}
}