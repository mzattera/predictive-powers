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
import io.github.mzattera.predictivepowers.huggingface.client.multimodal.TextToImageRequest;
import io.github.mzattera.predictivepowers.huggingface.client.nlp.ConversationalRequest;
import io.github.mzattera.predictivepowers.huggingface.client.nlp.ConversationalResponse;
import io.github.mzattera.predictivepowers.huggingface.client.nlp.QuestionAnsweringRequest;
import io.github.mzattera.predictivepowers.huggingface.client.nlp.QuestionAnsweringResponse;
import io.github.mzattera.predictivepowers.huggingface.client.nlp.TextClassificationRequest;
import io.github.mzattera.predictivepowers.huggingface.client.nlp.TextClassificationResponse;
import io.github.mzattera.predictivepowers.huggingface.client.nlp.TextGenerationRequest;
import io.github.mzattera.predictivepowers.huggingface.client.nlp.TextGenerationResponse;
import io.github.mzattera.util.ImageUtil;
import io.reactivex.Single;
import lombok.NonNull;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * API Client to access Hugging Face Hosted Inference API.
 * 
 * See {@link https://huggingface.co/docs/api-inference/index} for details.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public final class HuggingFaceClient implements ApiClient {

	private final static Logger LOG = LoggerFactory.getLogger(HuggingFaceClient.class);

	/**
	 * Name of the OS environment variable containing the API key.
	 */
	public static final String OS_ENV_VAR_NAME = "HUGGING_FACE_API_KEY";

	private final static String API_BASE_URL = "https://api-inference.huggingface.co/models/";

	public final static int DEFAULT_TIMEOUT_MILLIS = 6 * 60 * 1000; // Sometimes we must wait for the models to load,
																	// which takes time

	public final static int DEFAULT_KEEP_ALIVE_MILLIS = 5 * 60 * 1000;

	public final static int DEFAULT_MAX_IDLE_CONNECTIONS = 5;

	// OpenAI API defined with Retrofit
	private final HuggingFaceApi api;

	private final OkHttpClient client;

	// Maps from-to POJO <-> JSON
	private final static ObjectMapper JSON_MAPPER;
	static {
		JSON_MAPPER = new ObjectMapper();
		JSON_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		JSON_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		JSON_MAPPER.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
	}

	/**
	 * Constructor, using default parameters for OkHttpClient. Hugging Face API key
	 * is read from {@link #OS_ENV_VAR_NAME} system environment variable.
	 */
	public HuggingFaceClient() {
		this(null, DEFAULT_TIMEOUT_MILLIS, DEFAULT_KEEP_ALIVE_MILLIS, DEFAULT_MAX_IDLE_CONNECTIONS);
	}

	/**
	 * Constructor, using default parameters for OkHttpClient.
	 * 
	 * @param apiKey Hugging Face API key. If this is null, it will try to read it
	 *               from {@link #OS_ENV_VAR_NAME} system environment variable.
	 */
	public HuggingFaceClient(String apiKey) {
		this(apiKey, DEFAULT_TIMEOUT_MILLIS, DEFAULT_KEEP_ALIVE_MILLIS, DEFAULT_MAX_IDLE_CONNECTIONS);
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
	public HuggingFaceClient(String apiKey, int readTimeout, int keepAliveDuration, int maxIdleConnections) {
		this(ApiClient.getDefaultHttpClient((apiKey == null) ? getApiKey() : apiKey, readTimeout, keepAliveDuration,
				maxIdleConnections));
	}

	/**
	 * Constructor. This client uses provided OkHttpClient for API calls, to allow
	 * full customization {@link #getDefaultHttpClient(String, int, int, int)}
	 */
	public HuggingFaceClient(OkHttpClient http) {

		client = http;
//		client = http.newBuilder().addInterceptor(new Interceptor() {
//
//			@Override
//			public Response intercept(Chain chain) throws IOException {
//
//				try {
//					System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
//					System.out.println(JSON_MAPPER.writerWithDefaultPrettyPrinter()
//							.writeValueAsString(chain.request().toString()));
//					System.out.println("--------------------------------");
//				} catch (JsonProcessingException e) {
//					e.printStackTrace();
//				}
//
//				Response resp = chain.proceed(chain.request());
//				try {
//					System.out.println(
//							JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(resp.body().string()));
//					System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
//				} catch (JsonProcessingException e) {
//					e.printStackTrace();
//				}
//
//				return null;
//			}
//		}).build();

		Retrofit retrofit = new Retrofit.Builder().baseUrl(API_BASE_URL).client(client)
				.addConverterFactory(JacksonConverterFactory.create(JSON_MAPPER))
				.addCallAdapterFactory(RxJava2CallAdapterFactory.create()).build();

		api = retrofit.create(HuggingFaceApi.class);
	}

	/**
	 * @return The API key from OS environment.
	 */
	private static String getApiKey() {
		String apiKey = System.getenv(OS_ENV_VAR_NAME);
		if (apiKey == null)
			throw new IllegalArgumentException("OpenAI API key is not provided and it cannot be found in "
					+ OS_ENV_VAR_NAME + " system environment variable");
		return apiKey;
	}

	//////// API METHODS MAPPED INTO JAVA CALLS ////////////////////////////////////

	/// NLP //////////////////////////////////////////////////

	public List<List<TextClassificationResponse>> textClassification(@NonNull String model,
			TextClassificationRequest req) {
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

	public BufferedImage textToImage(@NonNull String model, TextToImageRequest req) throws IOException {
		return ImageUtil.fromBytes(callApi(api.textToImage(model, req)).bytes());
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
			client.cache().close();
		} catch (Exception e) {
			LOG.warn("Error while closing client", e);
		}
	}
}
