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

import io.github.mzattera.predictivepowers.google.endpoint.GoogleEndpoint;
import io.github.mzattera.predictivepowers.google.service.GoogleSearchService;
import io.github.mzattera.predictivepowers.services.SearchResult;

/**
 * Example of how to perform Google search.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
public class GoogleSearchExample {

	public static void main(String[] args) {

		try (GoogleEndpoint endpoint = new GoogleEndpoint()) {
			GoogleSearchService service = endpoint.getSearchService();
			
			for (SearchResult result: service.search("Massimliano Zattera")) {
				System.out.println(result.getTitle() + ": " + result.getLink());
			}
		}
	}
}
