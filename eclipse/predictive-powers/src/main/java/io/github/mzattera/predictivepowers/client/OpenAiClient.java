/**
 * 
 */
package io.github.mzattera.predictivepowers.client;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import io.reactivex.Single;
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
public class OpenAiClient {

	public final static String API_BASE_URL = "https://api.openai.com/v1/";

	// Maps from-to POJO <-> JSON
	private final static ObjectMapper mapper;
	static {
		mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
	}

	// OpenAI API defined with Retrofit
	private final OpenAiApi api;

	public OpenAiClient(String apiKey) {
		this(apiKey, 5000);
	}

	public OpenAiClient(String apiKey, int timeoutMillis) {

		OkHttpClient client = new OkHttpClient.Builder().connectionPool(new ConnectionPool(5, 3, TimeUnit.SECONDS))
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

	private static <T> T callApi(Single<T> apiCall) {
		try {
			return apiCall.blockingGet();
		} catch (HttpException e) {
			try {
				throw mapper.readValue(e.response().errorBody().string(), OpenAiException.class);
			} catch (Exception ex) {
				// fallback
				throw e;
			}
		}
	}
}
