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

package io.github.mzattera.predictivepowers.google;

import java.io.IOException;
import java.security.GeneralSecurityException;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.customsearch.v1.Customsearch;

import io.github.mzattera.predictivepowers.EndpointException;
import io.github.mzattera.predictivepowers.SearchEndpoint;
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

	public static final String OS_ENV_VAR_NAME = "GOOGLE_API_KEY";

	/**
	 * Name of the OS environment variable containing the Engine ID.
	 */
	public static final String OS_ENV_ENGINE_VAR_NAME = "GOOGLE_ENGINE_ID";

	private final @NonNull String apiKey;
	private final @NonNull String engineId;

	@Getter
	private final @NonNull Customsearch client;

	public GoogleEndpoint() {
		this(System.getenv(OS_ENV_ENGINE_VAR_NAME), System.getenv(OS_ENV_VAR_NAME));
	}

	public GoogleEndpoint(@NonNull String engineId, @NonNull String apiKey) throws EndpointException {
		this.apiKey = apiKey;
		this.engineId = engineId;
		try {
			this.client = new Customsearch.Builder(GoogleNetHttpTransport.newTrustedTransport(),
					GsonFactory.getDefaultInstance(), null).setApplicationName("predictive-powers").build();
		} catch (GeneralSecurityException | IOException e) {
			throw EndpointException.fromException(e, "Error instancieting Google Search endpoint");
		}
	}

	public GoogleEndpoint(@NonNull String engineId, @NonNull String apiKey, @NonNull Customsearch client) {
		this.apiKey = apiKey;
		this.engineId = engineId;
		this.client = client;
	}

	@Override
	public GoogleSearchService getSearchService() {
		return new GoogleSearchService(this, apiKey, engineId);
	}

	@Override
	public void close() {
		// Nothing to do here.
	}
}
