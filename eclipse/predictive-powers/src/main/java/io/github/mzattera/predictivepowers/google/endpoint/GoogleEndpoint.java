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

package io.github.mzattera.predictivepowers.google.endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mzattera.predictivepowers.SearchEndpoint;
import io.github.mzattera.predictivepowers.google.client.GoogleClient;
import io.github.mzattera.predictivepowers.google.service.GoogleSearchService;
import lombok.Getter;
import lombok.NonNull;

/**
 * This represents a Google endpoint, from which services can be created.
 * 
 * This class is thread-safe.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
public class GoogleEndpoint implements SearchEndpoint {

	// TODO always ensure it returns Google specific services

	private final static Logger LOG = LoggerFactory.getLogger(GoogleEndpoint.class);

	@Getter
	private final GoogleClient client;

	public GoogleEndpoint() {
		this(new GoogleClient());
	}

	public GoogleEndpoint(String engineId, String apiKey) {
		this(new GoogleClient(engineId, apiKey, -1, -1, -1));
	}

	public GoogleEndpoint(@NonNull GoogleClient client) {
		this.client = client;
	}

	@Override
	public GoogleSearchService getSearchService() {
		return new GoogleSearchService(this);
	}

	@Override
	public synchronized void close() {
		try {
			client.close();
		} catch (Exception e) {
			LOG.warn("Error while closing endpoint", e);
		}
	}
}
