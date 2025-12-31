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

package io.github.mzattera.predictivepowers.deepseek;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.mzattera.predictivepowers.TestConfiguration;
import io.github.mzattera.predictivepowers.services.messages.ChatCompletion;
import io.github.mzattera.predictivepowers.services.messages.FinishReason;

public class DeepSeekAgentTest {

	private static List<Pair<DeepSeekEndpoint, String>> svcs = new ArrayList<>();

	@BeforeAll
	static void init() {
		if (TestConfiguration.TEST_DEEPSEEK_SERVICES)
			svcs.add(new ImmutablePair<>(new DeepSeekEndpoint(), "deepseek-reasoner"));
	}

	@AfterAll
	static void tearDown() {
		TestConfiguration.close(svcs.stream().map(p -> p.getLeft()).collect(Collectors.toList()));
	}

	// Reasoning services to be tested (for refusal)
	static Stream<Pair<DeepSeekEndpoint, String>> reasoning() {
		return svcs.stream();
	}

	static boolean hasReasoning() {
		return reasoning().findAny().isPresent();
	}

	@ParameterizedTest
	@MethodSource("reasoning")
	@EnabledIf("hasReasoning")
	@DisplayName("Checks reasoning COT is added to response")
	public void testHistoryComplete(Pair<DeepSeekEndpoint, String> p) throws Exception {
		try (DeepSeekChatService svc = p.getLeft().getChatService(p.getRight())) {
			ChatCompletion response = svc.chat("If a>b and b>c then...");
			assertEquals(FinishReason.COMPLETED, response.getFinishReason());
			assertNotNull(response.getMessage().getReasoning());
		}
	}
}
