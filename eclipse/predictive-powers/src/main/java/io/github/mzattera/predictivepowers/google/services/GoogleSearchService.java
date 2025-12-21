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

package io.github.mzattera.predictivepowers.google.services;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.google.api.services.customsearch.v1.Customsearch;
import com.google.api.services.customsearch.v1.model.Result;

import io.github.mzattera.predictivepowers.EndpointException;
import io.github.mzattera.predictivepowers.services.Link;
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

	private final @NonNull String apiKey;
	private final @NonNull String engineId;

	@Getter
	protected final @NonNull GoogleEndpoint endpoint;

	public GoogleSearchService(GoogleEndpoint endpoint, String apiKey, String engineId) {
		this.endpoint = endpoint;
		this.apiKey = apiKey;
		this.engineId = engineId;
	}

	@Override
	public List<Link> search(@NonNull String query) {
		return search(query, 10);
	}

	@Override
	public List<Link> search(@NonNull String query, int n) {
		try {
			List<Link> links = new ArrayList<>();

			Customsearch.Cse.List listRequest = endpoint.getClient().cse()
					.list();
			listRequest.setKey(apiKey);
			listRequest.setCx(engineId);
			listRequest.setQ(query);
			listRequest.setNum(n);
			java.util.List<Result> results = listRequest.execute().getItems();

			if (results != null) {
				for (Result result : results) {
					links.add(Link.builder().fileFormat(result.getFileFormat()) //
							.mime(result.getMime()) //
							.snippet(result.getSnippet()) //
							.title(result.getTitle()) //
							.url(URI.create(result.getLink()).toURL()) //
							.build());
				}
			}

			return links;
		} catch (Exception e) {
			throw new EndpointException(e);
		}
	}

	@Override
	public void close() {
	}
}
