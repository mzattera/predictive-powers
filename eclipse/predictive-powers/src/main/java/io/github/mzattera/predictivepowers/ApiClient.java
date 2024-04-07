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
public interface ApiClient extends AutoCloseable {

	final Logger LOG = LoggerFactory.getLogger(ApiClient.class);

	final Random RND = new Random();

	final int BASE_DELAY_MILLIS = 1 * 1000;

	/**
	 * Returns an OkHttpClient to use for API calls. The client can be further
	 * customized and then used to build an API client which, in turn, can be used
	 * to build an Endpoint.
	 * 
	 * This client automatically retries calls if the API is unavailable or rate
	 * limits (e.g. requests per minute) are reached (HTTP errors 429, 500, 503, and
	 * 504). see maxRetries parameter if you want to disable this feature.
	 * 
	 * @param readTimeoutMillis        Read timeout for connections (milliseconds).
	 *                                 0 means no timeout.
	 * @param maxRetries               In case we receive an HTTP error signaling
	 *                                 temporary server unavailability, the client
	 *                                 will retry the call, at maximum this amount
	 *                                 of times. Use values <= 0 to disable this
	 *                                 feature.
	 * @param keepAliveDurationMillis  Timeout for connections in client pool
	 *                                 (milliseconds).
	 * @param maxIdleConnectionsMillis Maximum number of idle connections to keep in
	 *                                 the pool.
	 */
	public static OkHttpClient getDefaultHttpClient(int readTimeoutMillis, int maxRetries, int keepAliveDurationMillis,
			int maxIdleConnectionsMillis) {

		if (readTimeoutMillis < 0)
			throw new IllegalArgumentException("Read time out must be >= 0");
		if (keepAliveDurationMillis < 0)
			throw new IllegalArgumentException("Keep alive druaation out be >= 0");
		if (maxIdleConnectionsMillis < 0)
			throw new IllegalArgumentException("Max idel connections must must be >= 0");

		Builder builder = new OkHttpClient.Builder()
				.connectionPool(
						new ConnectionPool(maxIdleConnectionsMillis, keepAliveDurationMillis, TimeUnit.MILLISECONDS))
				.readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS);

		builder.addInterceptor(new Interceptor() { // Handles service unavailable HTTP errors
			@Override
			public Response intercept(Chain chain) throws IOException {

				Request request = chain.request();

				long start = System.currentTimeMillis();
				Response response = chain.proceed(request);

				int retries = 0;
				int delayMillis = BASE_DELAY_MILLIS;
				long elapsed = System.currentTimeMillis() - start;
//				if (response.code() != 200)
//					System.out.println("SERVER BUSY check: " + (retries < maxRetries) + " timeOut:" + readTimeoutMillis
//							+ " elapsed: " + elapsed + " " + (readTimeoutMillis <= elapsed));

				while ((retries < maxRetries) && ((readTimeoutMillis == 0) || (elapsed < readTimeoutMillis))
						&& ((response.code() == 429) || (response.code() == 500) || (response.code() == 503)
								|| (response.code() == 504))) {

					// Waits and retries as server is temporarily unavailable

					// System.out.println("SERVER BUSY: retrues: " + retries + " timeOut: " +
					// readTimeoutMillis
//							+ " elapsed: " + elapsed);

					try {
						// Use wait value provided by the server, if available.
						delayMillis = Integer.parseInt(response.header("Retry-After")) * 1000;
						if ((readTimeoutMillis > 0) && (readTimeoutMillis < (delayMillis + elapsed))) {
//							System.out.println("SERVER BUSY: wait too long delay: " + delayMillis + " delay+elapsed: "
//									+ (delayMillis + elapsed));
							// the wait is too long
							return response;
						}
					} catch (Exception e) {
						// Wait (progressively increasing wait time)
						delayMillis = Math.min( //
								(readTimeoutMillis == 0) ? Integer.MAX_VALUE : (int) (readTimeoutMillis - elapsed), //
								(int) (delayMillis * 2.0 * (1.0 + RND.nextDouble())));
					}

					String message = "";
					try {
						message = "\n" + response.body().string();
						response.close();
					} catch (Exception e) {
					}
					LOG.warn("HTTP " + response.code() + ": Waiting " + delayMillis + "ms: " + request.url() + message);

					try { // Wait
//						System.out.println("SERVER BUSY: waiting: " + delayMillis);
						Thread.sleep(delayMillis);
					} catch (InterruptedException e) {
					}

					// Retry
					response = chain.proceed(request);
					elapsed = System.currentTimeMillis() - start;
					++retries;
				} // While retrying

				// return last response
				return response;
			}
		});

		return builder.build();
	}
}
