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

	/**
	 * Returns an OkHttpClient to use for API calls, using default parameters. This
	 * can be further customized and then used in constructor to build an client
	 * which, in turn, can be used to build an Endpoint.
	 * 
	 * @param apiKey             API key for underlying API calls. If this is not
	 *                           null an "Authorization" header is added
	 *                           automatically to each call performed by the client.
	 * @param readTimeout        Read timeout for connections. 0 means no timeout.
	 * @param keepAliveDuration  Timeout for connections in client pool
	 *                           (milliseconds).
	 * @param maxIdleConnections Maximum number of idle connections to keep in the
	 *                           pool.
	 */
	public static OkHttpClient getDefaultHttpClient(String apiKey, int readTimeout, int keepAliveDuration,
			int maxIdleConnections) {

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

		builder.addInterceptor(new Interceptor() { // Handles error 429 (request rate limit reached)
			@Override
			public Response intercept(Chain chain) throws IOException {
				Request request = chain.request();
				Response response = chain.proceed(request);

				while ((response.code() == 429)||(response.code() == 503)) { // Waits and retries in case we reached rate limit or server is unavailable
					
					// TODO URGENT add max retries
					
					response.close();

					// TODO URGENT Make configurable
					int waitTime = 3; 
					try {
						waitTime = Integer.parseInt(response.header("Retry-After"));
					} catch (Exception e) {
					}
					LOG.warn("HTTP " + response.code() + ": Waiting " + waitTime + " seconds: " + request.url());
					try {
						Thread.sleep(waitTime * 1000);
					} catch (InterruptedException e) {
					}
					
					response = chain.proceed(request);
				}

				// otherwise just pass the original response on
				return response;
			}
		});

		return builder.build();
	}
}
