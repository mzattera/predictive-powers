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

/**
 * 
 */
package io.github.mzattera.predictivepowers;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import lombok.NonNull;
import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;

/**
 * This represent a client to access an existing HTTP API.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public interface ApiClient extends Closeable {

	public final static int DEFAULT_TIMEOUT_MILLIS = 60 * 1000;

	public final static int DEFAULT_KEEP_ALIVE_MILLIS = 5 * 60 * 1000;

	public final static int DEFAULT_MAX_IDLE_CONNECTIONS = 5;

	/**
	 * Returns an OkHttpClient to use for API calls, using default parameters. This
	 * can be further customized and then used in constructor to build an client
	 * which, in turn, can be used to build an Endpoint.
	 * 
	 * @param apiKey API key for underlying API calls.
	 */
	public static OkHttpClient getDefaultHttpClient(@NonNull final String apiKey) {
		return getDefaultHttpClient(apiKey, DEFAULT_TIMEOUT_MILLIS, DEFAULT_KEEP_ALIVE_MILLIS, DEFAULT_MAX_IDLE_CONNECTIONS);
	}

	/**
	 * Returns an OkHttpClient to use for API calls, using default parameters. This
	 * can be further customized and then used in constructor to build an client
	 * which, in turn, can be used to build an Endpoint.
	 * 
	 * @param apiKey             API key for underlying API calls.
	 * @param readTimeout        Read timeout for connections. 0 means no timeout.
	 * @param keepAliveDuration  Timeout for connections in client pool
	 *                           (milliseconds).
	 * @param maxIdleConnections Maximum number of idle connections to keep in the
	 *                           pool. .
	 */
	public static OkHttpClient getDefaultHttpClient(@NonNull final String apiKey, int readTimeout,
			int keepAliveDuration, int maxIdleConnections) {

		if (readTimeout < 0)
			throw new IllegalArgumentException();
		if (keepAliveDuration < 0)
			throw new IllegalArgumentException();
		if (maxIdleConnections < 0)
			throw new IllegalArgumentException();

		return new OkHttpClient.Builder()
				.connectionPool(new ConnectionPool(maxIdleConnections, keepAliveDuration, TimeUnit.MILLISECONDS))
				.readTimeout(readTimeout, TimeUnit.MILLISECONDS).addInterceptor(new Interceptor() {
					@Override
					public Response intercept(Chain chain) throws IOException {
						return chain.proceed(
								chain.request().newBuilder().header("Authorization", "Bearer " + apiKey).build());
					}
				}).build();
	}
}
