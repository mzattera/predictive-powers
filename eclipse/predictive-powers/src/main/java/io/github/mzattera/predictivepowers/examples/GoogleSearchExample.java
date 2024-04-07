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

import io.github.mzattera.predictivepowers.google.client.GoogleEndpoint;
import io.github.mzattera.predictivepowers.google.services.GoogleSearchService;
import io.github.mzattera.predictivepowers.services.Link;

public class GoogleSearchExample {

	public static void main(String[] args) {

		// Creates a search endpoint and service by reading
		// engine ID and API key from system environment variables
		try (GoogleEndpoint endpoint = new GoogleEndpoint();
				GoogleSearchService service = endpoint.getSearchService();) {
			
			// Performs search and shows results.
			for (Link result : service.search("Massimliano Zattera")) {
				System.out.println(result.getTitle() + ": " + result.getUrl());
			}
			
		} // Closes resources
	}
}
