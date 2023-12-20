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
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Interface for all clients accessing an existing HTTP API.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public interface ApiClient extends Closeable {

	final static Logger LOG = LoggerFactory.getLogger(ApiClient.class);

	final Random RND = new Random();

	final int BASE_DELAY_MILLIS = 1 * 1000;

	/**
	 * Returns an OkHttpClient to use for API calls. The client can be further
	 * customized and then used to build an API client which, in turn, can be used
	 * to build an Endpoint.
	 * 
	 * This client automatically retries calls if the API is unavailable or rate
	 * limits (e.g. requests per minute) are reached (HTTP errors 429, 500, and 503). see maxRetries parameter if
	 * you want to disable this feature.
	 * 
	 * @param apiKey             API key for underlying API calls. If this is not
	 *                           null an "Authorization" header is added
	 *                           automatically to each call performed by the client.
	 * @param readTimeout        Read timeout for connections. 0 means no timeout.
	 * @param maxRetries         In case we receive an HTTP error signaling
	 *                           temporary server unavailability, the client will
	 *                           retry the call, at maximum this amount of times.
	 *                           Use values <= 0 to disable this feature.
	 * @param keepAliveDuration  Timeout for connections in client pool
	 *                           (milliseconds).
	 * @param maxIdleConnections Maximum number of idle connections to keep in the
	 *                           pool.
	 */
	public static OkHttpClient getDefaultHttpClient(String apiKey, int readTimeout, int maxRetries,
			int keepAliveDuration, int maxIdleConnections) {

		if (readTimeout < 0)
			throw new IllegalArgumentException();
		if (keepAliveDuration < 0)
			throw new IllegalArgumentException();
		if (maxIdleConnections < 0)
			throw new IllegalArgumentException();

		Builder builder = new OkHttpClient.Builder()
				.connectionPool(new ConnectionPool(maxIdleConnections, keepAliveDuration, TimeUnit.MILLISECONDS))
				.readTimeout(readTimeout, TimeUnit.MILLISECONDS);

		if (apiKey != null)
			builder.addInterceptor(new Interceptor() { // Add API key in authorization header
				@Override
				public Response intercept(Chain chain) throws IOException {
					return chain
							.proceed(chain.request().newBuilder().header("Authorization", "Bearer " + apiKey).build());
				}
			});

		builder.addInterceptor(new Interceptor() { // Handles service unavailable HTTP errors
			@Override
			public Response intercept(Chain chain) throws IOException {

				Request request = chain.request();
				Response response = chain.proceed(request);

				int retries = 0;
				int delayMillis = BASE_DELAY_MILLIS;
				while ((retries < maxRetries) && ((response.code() == 429) || (response.code() == 500) || (response.code() == 503))) {

					// Waits and retries in case server is temporarily unavailable

					// Try to get a meaningful error message
					String message = "";
					try {
						message = "\n" + response.body().string();
					} catch (Exception e) {
					}
					response.close();

					// Wait (progressively increasing wait time)
					try {
						// Use value provided by the server, if available.
						delayMillis = Integer.parseInt(response.header("Retry-After")) * 1000;
					} catch (Exception e) {
						// Else use manual backoff, max 1 minute
						delayMillis = Math.min(61_000, (int) (delayMillis * 2.0 * (1.0 + RND.nextDouble())));
					}
					LOG.warn("HTTP " + response.code() + ": Waiting " + delayMillis + "ms: " + request.url() + message);
					try {
						Thread.sleep(delayMillis);
					} catch (InterruptedException e) {
					}

					// Retry
					response = chain.proceed(request);
					++retries;
				}

				// return last response
				return response;
			}
		});

		return builder.build();
	}
}
