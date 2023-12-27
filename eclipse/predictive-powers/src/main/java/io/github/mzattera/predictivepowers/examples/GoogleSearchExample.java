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

package io.github.mzattera.predictivepowers.examples;

import java.io.IOException;

import io.github.mzattera.predictivepowers.SearchEndpoint;
import io.github.mzattera.predictivepowers.google.endpoint.GoogleEndpoint;
import io.github.mzattera.predictivepowers.services.Link;
import io.github.mzattera.predictivepowers.services.SearchService;

public class GoogleSearchExample {

	public static void main(String[] args) throws IOException {

		// Creates a search endpoint by reading engine ID and
		// API key from system environment variables
		try (SearchEndpoint endpoint = new GoogleEndpoint()) {
			
			// Creates search service
			SearchService service = endpoint.getSearchService();
			
			// Performs search and shows results.
			for (Link result: service.search("Massimliano Zattera")) {
				System.out.println(result.getTitle() + ": " + result.getUrl());
			}
		}
	}
}
