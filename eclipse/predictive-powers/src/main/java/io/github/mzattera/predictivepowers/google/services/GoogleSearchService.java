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

package io.github.mzattera.predictivepowers.google.services;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mzattera.predictivepowers.google.client.GoogleClient;
import io.github.mzattera.predictivepowers.google.client.Result;
import io.github.mzattera.predictivepowers.google.client.Search;
import io.github.mzattera.predictivepowers.google.endpoint.GoogleEndpoint;
import io.github.mzattera.predictivepowers.services.SearchResult;
import io.github.mzattera.predictivepowers.services.SearchService;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Search service over Google search.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@RequiredArgsConstructor
public class GoogleSearchService implements SearchService {
	
	// TODO Add more methods
	
	public GoogleSearchService(GoogleEndpoint endpoint) {
		this(endpoint, endpoint.getClient());
	}

	private final static Logger LOG = LoggerFactory.getLogger(GoogleSearchService.class);

	@NonNull
	@Getter
	protected final GoogleEndpoint endpoint;

	@Getter
	private final GoogleClient client;

	@Override
	public List<SearchResult> search(@NonNull String query) {
		return search(query, 10);
	}

	@Override
	public List<SearchResult> search(@NonNull String query, int n) {
		Search search = endpoint.getClient().list(query, n);
		List<SearchResult> result = new ArrayList<>(search.getItems().size());
		for (Result i : search.getItems()) {
			try {
				// TODO urgent add all fields to SearchResult
				result.add(SearchResult.builder().title(i.getTitle()).url(new URL(i.getLink())).build());
			} catch (MalformedURLException e) {
				LOG.error("Malformed URL in search result: " + i.getLink(), e);
			}
		}

		return result;
	}
}
