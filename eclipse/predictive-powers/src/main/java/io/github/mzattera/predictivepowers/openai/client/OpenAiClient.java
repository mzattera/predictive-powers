/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsResponse;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsResponse;
import io.github.mzattera.predictivepowers.openai.client.edits.EditsRequest;
import io.github.mzattera.predictivepowers.openai.client.edits.EditsResponse;
import io.github.mzattera.predictivepowers.openai.client.embeddings.EmbeddingsRequest;
import io.github.mzattera.predictivepowers.openai.client.embeddings.EmbeddingsResponse;
import io.github.mzattera.predictivepowers.openai.client.images.ImagesRequest;
import io.github.mzattera.predictivepowers.openai.client.images.ImagesResponse;
import io.github.mzattera.predictivepowers.openai.client.models.Model;
import io.github.mzattera.predictivepowers.openai.client.models.ModelsResponse;
import io.github.mzattera.predictivepowers.util.ImageUtil;
import io.reactivex.Single;
import lombok.NonNull;
import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
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

	public final static int DEFAULT_READ_TIMEOUT_MIILLIS = 30 * 1000;

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
				.connectionPool(new ConnectionPool(5, timeoutMillis, TimeUnit.MILLISECONDS))
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

	public ModelsResponse listModels() {
		return callApi(api.models());
	}

	public Model retrieveModel(String modelId) {
		return callApi(api.models(modelId));
	}

	public CompletionsResponse createCompletion(CompletionsRequest req) {
		return callApi(api.completions(req));
	}

	public ChatCompletionsResponse createChatCompletion(ChatCompletionsRequest req) {
		return callApi(api.chatCompletions(req));
	}

	public ImagesResponse createImage(ImagesRequest req) {
		return callApi(api.imagesGenerations(req));
	}

	public ImagesResponse createImageEdit(ImagesRequest req, @NonNull BufferedImage image, BufferedImage mask)
			throws IOException {

		MultipartBody.Builder builder = new MultipartBody.Builder().setType(MediaType.get("multipart/form-data"))
				.addFormDataPart("image", "", ImageUtil.toRequestBody("png", image));

		if (req.getPrompt() != null) {
			builder.addFormDataPart("prompt", req.getPrompt());
		} else {
			throw new IllegalArgumentException("Prompt cannot be null");
		}
		if (mask != null) {
			builder.addFormDataPart("mask", "", ImageUtil.toRequestBody("png", mask));
		}
		if (req.getN() != null) {
			builder.addFormDataPart("n", req.getN().toString());
		}
		if (req.getSize() != null) {
			builder.addFormDataPart("size", req.getSize().toString());
		}
		if (req.getResponseFormat() != null) {
			builder.addFormDataPart("response_format", req.getResponseFormat().toString());
		}
		if (req.getUser() != null) {
			builder.addFormDataPart("user", req.getUser());
		}

		return callApi(api.imagesEdits(builder.build()));
	}

	public ImagesResponse createImageVariation(ImagesRequest req, @NonNull BufferedImage image) throws IOException {

		MultipartBody.Builder builder = new MultipartBody.Builder().setType(MediaType.get("multipart/form-data"))
				.addFormDataPart("image", "", ImageUtil.toRequestBody("png", image));

		if (req.getN() != null) {
			builder.addFormDataPart("n", req.getN().toString());
		}
		if (req.getSize() != null) {
			builder.addFormDataPart("size", req.getSize().toString());
		}
		if (req.getResponseFormat() != null) {
			builder.addFormDataPart("response_format", req.getResponseFormat().toString());
		}
		if (req.getUser() != null) {
			builder.addFormDataPart("user", req.getUser());
		}

		return callApi(api.imagesVariations(builder.build()));
	}

	public EditsResponse createEdit(EditsRequest req) {
		return callApi(api.edits(req));
	}

	public EmbeddingsResponse createEmbeddings(EmbeddingsRequest req) {
		return callApi(api.embeddings(req));
	}

	/////////////////////////////////////////////////////////////////////////////////

	private <T> T callApi(Single<T> apiCall) {
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
