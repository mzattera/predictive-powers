/*
 * Copyright 2023-2024 Massimiliano "Maxi" Zattera
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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.mzattera.predictivepowers.SearchEndpoint;
import io.github.mzattera.predictivepowers.TestConfiguration;

public class SearchServiceTest {

	private static List<SearchEndpoint> svcs;

	@BeforeAll
	static void init() {
		svcs = TestConfiguration.getSearchEndpoints();
	}

	@AfterAll
	static void tearDown() {
		TestConfiguration.close(svcs.stream().collect(Collectors.toList()));
	}

	// All services planned to be tested
	static Stream<SearchEndpoint> services() {
		return svcs.stream();
	}

	// Must be static unless using @TestInstance(Lifecycle.PER_CLASS)
	static boolean hasServices() {
		return services().findAny().isPresent();
	}

	@ParameterizedTest
	@DisplayName("Simple search test.")
	@MethodSource("services")
	@EnabledIf("hasServices")
	public void testSearch(SearchEndpoint ep) throws Exception {

		try (SearchService service = ep.getSearchService()) {
			List<Link> results = service.search("Mount Everest", 3);
			for (Link l : results) {
				System.out.println(l.toString());
			}
			assertEquals(3, results.size());
		}
	}
}
