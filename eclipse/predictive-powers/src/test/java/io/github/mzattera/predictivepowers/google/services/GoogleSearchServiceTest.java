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

package io.github.mzattera.predictivepowers.google.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.TestConfiguration;
import io.github.mzattera.predictivepowers.google.client.GoogleEndpoint;
import io.github.mzattera.predictivepowers.services.Link;

public class GoogleSearchServiceTest {

	@Test
	public void testSearch() throws URISyntaxException, MalformedURLException {

		if (!TestConfiguration.TEST_GOOGLE_SERVICES)
			return;

		try (GoogleEndpoint endpoint = new GoogleEndpoint();
				GoogleSearchService service = endpoint.getSearchService();) {

			List<Link> results = service.search("Massimiliano Zattera predictive-powers github");
			assertTrue(results.size() > 0);
			assertEquals((new URI("https://github.com/mzattera")).toURL(), results.get(0).getUrl());
		}
	}
}
