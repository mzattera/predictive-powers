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

package io.github.mzattera.predictivepowers.anthropic.client;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import io.github.mzattera.predictivepowers.ApiClient;
import io.github.mzattera.predictivepowers.anthropic.client.messages.MessagesRequest;
import io.github.mzattera.predictivepowers.anthropic.client.messages.MessagesResponse;
import io.reactivex.Single;
import lombok.Getter;
import lombok.NonNull;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Response;
import retrofit2.HttpException;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * API Client to access Anthrop\c API.
 * 
 * See {@link https://docs.anthropic.com/claude/reference/messages_post} for
 * details.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class AnthropicClient implements ApiClient {

	public static final String API_VERSION = "2023-06-01";

	private final static Logger LOG = LoggerFactory.getLogger(AnthropicClient.class);

	/** Used for JSON (de)serialization in API calls */
	@Getter
	protected final static ObjectMapper jsonMapper;

	static {
		jsonMapper = new ObjectMapper();
		jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		jsonMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		jsonMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
	}

	/**
	 * Name of the OS environment variable containing the API key.
	 */
	public static final String OS_ENV_VAR_NAME = "ANTHROPIC_API_KEY";

	public final static int DEFAULT_TIMEOUT_MILLIS = 3 * 60 * 1000;
	public final static int DEFAULT_MAX_RETRIES = 10;
	public final static int DEFAULT_KEEP_ALIVE_MILLIS = 5 * 60 * 1000;
	public final static int DEFAULT_MAX_IDLE_CONNECTIONS = 5;

	private final static String API_BASE_URL = "https://api.anthropic.com/v1/";

	// OpenAI API defined with Retrofit
	private final AnthropicApi api;

	private final OkHttpClient client;

	/**
	 * Constructor, using default parameters for OkHttpClient. OpenAI API key is
	 * read from {@link #OS_ENV_VAR_NAME} system environment variable.
	 */
	public AnthropicClient() {
		this(null, DEFAULT_TIMEOUT_MILLIS, DEFAULT_MAX_RETRIES, DEFAULT_KEEP_ALIVE_MILLIS,
				DEFAULT_MAX_IDLE_CONNECTIONS);
	}

	/**
	 * Constructor, using default parameters for OkHttpClient.
	 * 
	 * @param apiKey OpenAiApi key. If this is null, it will try to read it from
	 *               {@link #OS_ENV_VAR_NAME} system environment variable.
	 */
	public AnthropicClient(String apiKey) {
		this(apiKey, DEFAULT_TIMEOUT_MILLIS, DEFAULT_MAX_RETRIES, DEFAULT_KEEP_ALIVE_MILLIS,
				DEFAULT_MAX_IDLE_CONNECTIONS);
	}

	/**
	 * Constructor. This client uses an underlying OkHttpClient for API calls, which
	 * parameters can be specified.
	 * 
	 * @param apiKey             OpenAiApi key. If this is null, it will try to read
	 *                           it from {@link #OS_ENV_VAR_NAME} system environment
	 *                           variable.
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
	public AnthropicClient(String apiKey, int readTimeout, int maxRetries, int keepAliveDuration,
			int maxIdleConnections) {
		this(apiKey, ApiClient.getDefaultHttpClient(readTimeout, maxRetries, keepAliveDuration, maxIdleConnections));
	}

	/**
	 * Constructor. This client uses provided OkHttpClient for API calls, to allow
	 * full customization (see
	 * {@link ApiClient#getDefaultHttpClient(int, int, int, int)}).
	 * 
	 * Notice API key header is set in this call, by reading it from OS environment.
	 */
	public AnthropicClient(OkHttpClient http) {
		this(null, http);
	}

	/**
	 * Constructor. This client uses provided OkHttpClient for API calls, to allow
	 * full customization (see
	 * {@link ApiClient#getDefaultHttpClient(int, int, int, int)}).
	 * 
	 * @param apiKey OpenAI API key to use (will be set in the header).
	 */
	public AnthropicClient(String apiKey, OkHttpClient http) {

		Builder builder = http.newBuilder();

		// Debug code below, outputs the request
//		builder.addInterceptor(new Interceptor() {
//
//			@Override
//			public Response intercept(Chain chain) throws IOException {
//				Request req = chain.request();
//
//				if (req.body() != null) {
//					Buffer buffer = new Buffer();
//					req.body().writeTo(buffer);
//					String in = buffer.readUtf8();
//					String bodyContent = "";
//					try {
//						// In case body is not JSON
//						bodyContent = jsonMapper.writerWithDefaultPrettyPrinter()
//								.writeValueAsString(jsonMapper.readTree(in));
//					} catch (Exception e) {
//						bodyContent = in;
//					}
//					System.out.println("Request body: " + bodyContent);
//				}
//
//				return chain.proceed(req);
//			}
//		}); //

		// Debug code below, outputs the response
//		builder.addInterceptor(new Interceptor() {
//
//			@Override
//			public Response intercept(Chain chain) throws IOException {
//
//				Response response = chain.proceed(chain.request());
//				if (response.body() != null) {
//					BufferedSource source = response.body().source();
//					source.request(Long.MAX_VALUE);
//
//					@SuppressWarnings("deprecation")
//					Buffer buffer = source.buffer();
//
//					String in = buffer.clone().readUtf8();
//					String bodyContent = "";
//					try {
//						// In case body is not JSON
//						bodyContent = jsonMapper.writerWithDefaultPrettyPrinter()
//								.writeValueAsString(jsonMapper.readTree(in));
//					} catch (Exception e) {
//						bodyContent = in;
//					}
//					System.out.println("Response body: " + bodyContent);
//				}
//
//				return response; // Return the original response unaltered
//			}
//		}); //

		builder.addInterceptor(new Interceptor() { // Add API key in authorization header
			@Override
			public Response intercept(Chain chain) throws IOException {
				return chain.proceed(chain.request().newBuilder() //
						.header("x-api-key", (apiKey == null) ? getApiKey() : apiKey) //
						.header("content-type", "application/json") // 
						.header("anthropic-version", API_VERSION) //
						.header("anthropic-beta", "tools-2024-04-04") //
						.build());
			}
		}).build();

		client = builder.build();

		Retrofit retrofit = new Retrofit.Builder().baseUrl(API_BASE_URL).client(client)
				.addConverterFactory(JacksonConverterFactory.create(jsonMapper))
				.addCallAdapterFactory(RxJava2CallAdapterFactory.create()).build();

		api = retrofit.create(AnthropicApi.class);
	}

	/**
	 * @return The API key from OS environment.
	 */
	public static String getApiKey() {
		String apiKey = System.getenv(OS_ENV_VAR_NAME);
		if (apiKey == null)
			throw new IllegalArgumentException("ANTHROP\\C API key is not provided and it cannot be found in "
					+ OS_ENV_VAR_NAME + " system environment variable");
		return apiKey;
	}

	//////// API METHODS MAPPED INTO JAVA CALLS ////////////////////////////////////

	public MessagesResponse createMessage(@NonNull MessagesRequest req) {
		return callApi(api.messages(req));
	}

	/////////////////////////////////////////////////////////////////////////////////

	private <T> T callApi(Single<T> apiCall) {
		try {
			return apiCall.blockingGet();
		} catch (HttpException e) {

			AnthropicException ae;
			try {
				ae = new AnthropicException(e);
			} catch (Exception ex) {
				throw e;
			}
			throw ae;
		}
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
