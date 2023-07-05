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

package io.github.mzattera.predictivepowers.google.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import io.github.mzattera.predictivepowers.ApiClient;
import io.reactivex.Single;
import lombok.Getter;
import lombok.NonNull;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * API Client to access Google API.
 * 
 * See
 * {@link https://developers.google.com/custom-search/v1/reference/rest/v1/cse}
 * for details.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class GoogleClient implements ApiClient {

	private final static Logger LOG = LoggerFactory.getLogger(GoogleClient.class);

	/**
	 * Name of the OS environment variable containing the API key.
	 */
	public static final String OS_ENV_VAR_NAME = "GOOGLE_API_KEY";

	/**
	 * Name of the OS environment variable containing the Engine ID.
	 */
	public static final String OS_ENV_ENGINE_VAR_NAME = "GOOGLE_ENGINE_ID";

	public final static int DEFAULT_TIMEOUT_MILLIS = 60 * 1000;
	public final static int DEFAULT_MAX_RETRIES = 10;
	public final static int DEFAULT_KEEP_ALIVE_MILLIS = 5 * 60 * 1000;
	public final static int DEFAULT_MAX_IDLE_CONNECTIONS = 5;

	private final static String API_BASE_URL = "https://customsearch.googleapis.com/customsearch/";

	// OpenAI API defined with Retrofit
	private final GoogleApi api;

	// Google Programmable Search Engine ID
	private final String engineId;

	// Google API Key
	private final String apiKey;

	private final OkHttpClient client;

	/** Used for JSON (de)serialization in API calls */
	@Getter
	private final static ObjectMapper jsonMapper;
	static {
		jsonMapper = new ObjectMapper();
		jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		jsonMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		jsonMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
	}

	/**
	 * Constructor, using default parameters for OkHttpClient. Google engine ID and
	 * API keys are read system environment.
	 */
	public GoogleClient() {
		this(null, null, DEFAULT_TIMEOUT_MILLIS, DEFAULT_MAX_RETRIES, DEFAULT_KEEP_ALIVE_MILLIS,
				DEFAULT_MAX_IDLE_CONNECTIONS);
	}

	/**
	 * Constructor, using default parameters for OkHttpClient.
	 * 
	 * @param engineId Google engine ID. If this is null, it will try to read it
	 *                 from {@link #OS_ENV_ENGINE_VAR_NAME} system environment
	 *                 variable.
	 * @param apiKey   Google API key. If this is null, it will try to read it from
	 *                 {@link #OS_ENV_VAR_NAME} system environment variable.
	 */
	public GoogleClient(String engineId, String apiKey) {
		this(engineId, apiKey, DEFAULT_TIMEOUT_MILLIS, DEFAULT_KEEP_ALIVE_MILLIS, DEFAULT_MAX_RETRIES,
				DEFAULT_MAX_IDLE_CONNECTIONS);
	}

	/**
	 * Constructor. This client uses an underlying OkHttpClient for API calls, which
	 * parameters can be specified.
	 * 
	 * @param engineId           Google engine ID. If this is null, it will try to
	 *                           read it from {@link #OS_ENV_ENGINE_VAR_NAME} system
	 *                           environment variable.
	 * @param apiKey             Google API key. If this is null, it will try to
	 *                           read it from {@link #OS_ENV_VAR_NAME} system
	 *                           environment variable.
	 * @param readTimeout        Read timeout for connections. 0 means no timeout.
	 *                           * @param maxRetries In case we receive an HTTP
	 *                           error signaling temporary server unavailability,
	 *                           the client will retry the call, at maximum this
	 *                           amount of times. Use values <= 0 to disable this
	 *                           feature.
	 * @param keepAliveDuration  Timeout for connections in client pool
	 *                           (milliseconds).
	 * @param maxIdleConnections Maximum number of idle connections to keep in the
	 *                           pool.
	 */
	public GoogleClient(String engineId, String apiKey, int readTimeout, int maxRetries, int keepAliveDuration,
			int maxIdleConnections) {
		this((engineId == null) ? getEngineId() : engineId, (apiKey == null) ? getApiKey() : apiKey,
				ApiClient.getDefaultHttpClient(null, readTimeout, maxRetries, keepAliveDuration, maxIdleConnections));
	}

	/**
	 * Constructor. This client uses provided OkHttpClient for API calls, to allow
	 * full customization {@link #getDefaultHttpClient(String, int, int, int)}
	 * 
	 * @param engineId Google engine ID.
	 * @param apiKey   Google API key.
	 */
	public GoogleClient(@NonNull String engineId, @NonNull String apiKey, OkHttpClient http) {
		this.engineId = engineId;
		this.apiKey = apiKey;

		client = http;

		Retrofit retrofit = new Retrofit.Builder().baseUrl(API_BASE_URL).client(client)
				.addConverterFactory(JacksonConverterFactory.create(jsonMapper))
				.addCallAdapterFactory(RxJava2CallAdapterFactory.create()).build();

		api = retrofit.create(GoogleApi.class);
	}

	/**
	 * @return The API key from OS environment.
	 */
	public static String getApiKey() {
		String apiKey = System.getenv(OS_ENV_VAR_NAME);
		if (apiKey == null)
			throw new IllegalArgumentException("Google API key is not provided and it cannot be found in "
					+ OS_ENV_VAR_NAME + " system environment variable");
		return apiKey;
	}

	/**
	 * @return Google engine ID from OS environment.
	 */
	public static String getEngineId() {
		String engineId = System.getenv(OS_ENV_ENGINE_VAR_NAME);
		if (engineId == null)
			throw new IllegalArgumentException(
					"Google cusotm search engine ID is not provided and it cannot be found in " + OS_ENV_ENGINE_VAR_NAME
							+ " system environment variable");
		return engineId;
	}

	//////// API METHODS MAPPED INTO JAVA CALLS ////////////////////////////////////

	// TODO urgent add a method that uses Query or all parameters

	public Search list(String query, int n) {
		return callApi(api.list(engineId, apiKey, query, n));
	}

	/////////////////////////////////////////////////////////////////////////////////

	private <T> T callApi(Single<T> apiCall) {
		return apiCall.blockingGet();
	}

	@Override
	public void close() {
		try {
			client.dispatcher().executorService().shutdown();
			client.connectionPool().evictAll();
			if (client.cache() != null)
				client.cache().close();
		} catch (Exception e) {
			LOG.warn("Error while closing client", e);
		}
	}
}
