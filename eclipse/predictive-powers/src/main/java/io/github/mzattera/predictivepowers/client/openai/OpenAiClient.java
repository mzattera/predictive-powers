/**
 * 
 */
package io.github.mzattera.predictivepowers.client.openai;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import io.github.mzattera.predictivepowers.client.openai.chatcompletion.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.client.openai.chatcompletion.ChatCompletionsResponse;
import io.github.mzattera.predictivepowers.client.openai.completions.CompletionsRequest;
import io.github.mzattera.predictivepowers.client.openai.completions.CompletionsResponse;
import io.github.mzattera.predictivepowers.client.openai.models.Model;
import io.reactivex.Single;
import lombok.NonNull;
import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import retrofit2.HttpException;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * Clients that calls OpenAI API and return results.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public final class OpenAiClient {

	public final static String API_BASE_URL = "https://api.openai.com/v1/";

	public final static int DEFAULT_READ_TIMEOUT_MIILLIS = 15 * 1000;

	// OpenAI API defined with Retrofit
	private final OpenAiApi api;

	// Maps from-to POJO <-> JSON
	private final static ObjectMapper mapper;
	static {
		mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
	}

	public OpenAiClient() {
		this(System.getenv("OPENAI_API_KEY"), DEFAULT_READ_TIMEOUT_MIILLIS);
	}

	public OpenAiClient(@NonNull String apiKey) {
		this(apiKey, DEFAULT_READ_TIMEOUT_MIILLIS);
	}

	// TODO expose other parameters
	public OpenAiClient(@NonNull String apiKey, int timeoutMillis) {

		OkHttpClient client = new OkHttpClient.Builder()
				.connectionPool(new ConnectionPool(5, DEFAULT_READ_TIMEOUT_MIILLIS, TimeUnit.MILLISECONDS))
				.readTimeout(timeoutMillis, TimeUnit.MILLISECONDS).addInterceptor(new Interceptor() {
					@Override
					public Response intercept(Chain chain) throws IOException {
						return chain.proceed(
								chain.request().newBuilder().header("Authorization", "Bearer " + apiKey).build());
					}
				}).build();

		Retrofit retrofit = new Retrofit.Builder().baseUrl(API_BASE_URL).client(client)
				.addConverterFactory(JacksonConverterFactory.create(mapper))
				.addCallAdapterFactory(RxJava2CallAdapterFactory.create()).build();

		api = retrofit.create(OpenAiApi.class);
	}

	//////// API METHODS MAPPED INTO JAVA CALLS ////////////////////////////////////

	public List<Model> models() {
		return callApi(api.models()).data;
	}

	public Model models(String modelId) {
		return callApi(api.models(modelId));
	}

	public CompletionsResponse createCompletion(CompletionsRequest req) {
		return callApi(api.createCompletion(req));
	}

	public ChatCompletionsResponse createChatCompletion(ChatCompletionsRequest req) {
		return callApi(api.createChatCompletion(req));
	}

	/////////////////////////////////////////////////////////////////////////////////

	private static <T> T callApi(Single<T> apiCall) {
		try {
			return apiCall.blockingGet();
		} catch (HttpException e) {
			OpenAiException oaie;
			try {
				oaie = new OpenAiException(mapper.readValue(e.response().errorBody().string(), OpenAiError.class), e);
			} catch (Exception ex) {
				throw e;
			}
			throw oaie;
		}
	}
}
