/*
 * Copyright 2023-2024 Massimiliano "Maxi" Zattera
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

package io.github.mzattera.predictivepowers.huggingface.client;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import io.github.mzattera.predictivepowers.ApiClient;
import io.github.mzattera.predictivepowers.huggingface.client.nlp.ConversationalRequest;
import io.github.mzattera.predictivepowers.huggingface.client.nlp.ConversationalResponse;
import io.github.mzattera.predictivepowers.huggingface.client.nlp.QuestionAnsweringRequest;
import io.github.mzattera.predictivepowers.huggingface.client.nlp.QuestionAnsweringResponse;
import io.github.mzattera.predictivepowers.huggingface.client.nlp.TextClassificationResponse;
import io.github.mzattera.predictivepowers.huggingface.client.nlp.TextGenerationRequest;
import io.github.mzattera.predictivepowers.huggingface.client.nlp.TextGenerationResponse;
import io.github.mzattera.predictivepowers.util.ImageUtil;
import io.reactivex.rxjava3.core.Single;
import lombok.Getter;
import lombok.NonNull;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Response;
import retrofit2.HttpException;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * API Client to access Hugging Face Hosted Inference API.
 * 
 * See {@link https://huggingface.co/docs/api-inference/index} for details.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class HuggingFaceClient implements ApiClient {

	private final static Logger LOG = LoggerFactory.getLogger(HuggingFaceClient.class);

	/**
	 * Name of the OS environment variable containing the API key.
	 */
	public static final String OS_ENV_VAR_NAME = "HUGGING_FACE_API_KEY";

	private final static String API_BASE_URL = "https://api-inference.huggingface.co/models/";

	public final static int DEFAULT_TIMEOUT_MILLIS = 6 * 60 * 1000; // Sometimes we must wait for the models to load,
																	// which takes time
	public final static int DEFAULT_MAX_RETRIES = 10;
	public final static int DEFAULT_KEEP_ALIVE_MILLIS = 5 * 60 * 1000;
	public final static int DEFAULT_MAX_IDLE_CONNECTIONS = 5;

	// OpenAI API defined with Retrofit
	private final HuggingFaceApi api;

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
	 * Constructor, using default parameters for OkHttpClient. Hugging Face API key
	 * is read from {@link #OS_ENV_VAR_NAME} system environment variable.
	 */
	public HuggingFaceClient() {
		this(null, DEFAULT_TIMEOUT_MILLIS, DEFAULT_MAX_RETRIES, DEFAULT_KEEP_ALIVE_MILLIS,
				DEFAULT_MAX_IDLE_CONNECTIONS);
	}

	/**
	 * Constructor, using default parameters for OkHttpClient.
	 * 
	 * @param apiKey Hugging Face API key. If this is null, it will try to read it
	 *               from {@link #OS_ENV_VAR_NAME} system environment variable.
	 */
	public HuggingFaceClient(String apiKey) {
		this(apiKey, DEFAULT_TIMEOUT_MILLIS, DEFAULT_MAX_RETRIES, DEFAULT_KEEP_ALIVE_MILLIS,
				DEFAULT_MAX_IDLE_CONNECTIONS);
	}

	/**
	 * Constructor. This client uses an underlying OkHttpClient for API calls, which
	 * parameters can be specified.
	 * 
	 * @param apiKey             Hugging Face API key. If this is null, it will try
	 *                           to read it from {@link #OS_ENV_VAR_NAME} system
	 *                           environment variable.
	 * @param readTimeout        Read timeout for connections. 0 means no timeout.
	 * @param keepAliveDuration  Timeout for connections in client pool
	 *                           (milliseconds).
	 * @param maxIdleConnections Maximum number of idle connections to keep in the
	 *                           pool.
	 */
	public HuggingFaceClient(String apiKey, int readTimeout, int maxRetries, int keepAliveDuration,
			int maxIdleConnections) {
		this(ApiClient.getDefaultHttpClient(readTimeout, maxRetries, keepAliveDuration, maxIdleConnections));
	}

	/**
	 * Constructor. This client uses provided OkHttpClient for API calls, to allow
	 * full customization {@link #getDefaultHttpClient(String, int, int, int)}
	 */
	public HuggingFaceClient(OkHttpClient http) {
		this(null, http);
	}

	/**
	 * Constructor. This client uses provided OkHttpClient for API calls, to allow
	 * full customization {@link #getDefaultHttpClient(String, int, int, int)}
	 */
	public HuggingFaceClient(String apiKey, OkHttpClient http) {

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
						.header("Authorization", "Bearer " + ((apiKey == null) ? getApiKey() : apiKey)) //
						.build());
			}
		}).build();

		client = builder.build();

		Retrofit retrofit = new Retrofit.Builder().baseUrl(API_BASE_URL).client(client)
				.addConverterFactory(JacksonConverterFactory.create(jsonMapper))
				.addCallAdapterFactory(RxJava3CallAdapterFactory.create()).build();

		api = retrofit.create(HuggingFaceApi.class);
	}

	/**
	 * @return The API key from OS environment.
	 */
	public static String getApiKey() {
		String apiKey = System.getenv(OS_ENV_VAR_NAME);
		if (apiKey == null)
			throw new IllegalArgumentException("Hugging Face API key is not provided and it cannot be found in "
					+ OS_ENV_VAR_NAME + " system environment variable");
		return apiKey;
	}

	//////// API METHODS MAPPED INTO JAVA CALLS ////////////////////////////////////

	/// NLP //////////////////////////////////////////////////

	public List<List<TextClassificationResponse>> textClassification(@NonNull String model, HuggingFaceRequest req) {
		return callApi(api.textClassification(model, req));
	}

	public List<List<TextGenerationResponse>> textGeneration(@NonNull String model, TextGenerationRequest req) {
		return callApi(api.textGeneration(model, req));
	}

	public List<List<Double>> featureExtraction(@NonNull String model, HuggingFaceRequest req) {
		return callApi(api.featureExtraction(model, req));
	}

	public QuestionAnsweringResponse questionAnswering(@NonNull String model, QuestionAnsweringRequest req) {
		return callApi(api.questionAnswering(model, req));
	}

	public ConversationalResponse conversational(@NonNull String model, ConversationalRequest req) {
		return callApi(api.conversational(model, req));
	}

	/// MULTIMODAL //////////////////////////////////////////

	public BufferedImage textToImage(@NonNull String model, SingleHuggingFaceRequest req) throws IOException {
		return ImageUtil.fromBytes(callApi(api.textToImage(model, req)).bytes());
	}

	/////////////////////////////////////////////////////////////////////////////////

	private <T> T callApi(Single<T> apiCall) {
		try {
			return apiCall.blockingGet();
		} catch (HttpException e) {

			HuggingFaceException hfe;
			try {
				hfe = new HuggingFaceException(e);
			} catch (Exception ex) {
				throw e;
			}
			throw hfe;
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
