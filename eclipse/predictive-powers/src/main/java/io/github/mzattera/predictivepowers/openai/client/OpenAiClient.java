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

package io.github.mzattera.predictivepowers.openai.client;

import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import io.github.mzattera.predictivepowers.openai.client.audio.AudioRequest;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsResponse;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsResponse;
import io.github.mzattera.predictivepowers.openai.client.edits.EditsRequest;
import io.github.mzattera.predictivepowers.openai.client.edits.EditsResponse;
import io.github.mzattera.predictivepowers.openai.client.embeddings.EmbeddingsRequest;
import io.github.mzattera.predictivepowers.openai.client.embeddings.EmbeddingsResponse;
import io.github.mzattera.predictivepowers.openai.client.files.File;
import io.github.mzattera.predictivepowers.openai.client.finetunes.FineTune;
import io.github.mzattera.predictivepowers.openai.client.finetunes.FineTuneEvent;
import io.github.mzattera.predictivepowers.openai.client.finetunes.FineTunesRequest;
import io.github.mzattera.predictivepowers.openai.client.images.Image;
import io.github.mzattera.predictivepowers.openai.client.images.ImagesRequest;
import io.github.mzattera.predictivepowers.openai.client.models.Model;
import io.github.mzattera.predictivepowers.openai.client.moderations.ModerationsRequest;
import io.github.mzattera.predictivepowers.openai.client.moderations.ModerationsResponse;
import io.github.mzattera.util.ImageUtil;
import io.reactivex.Single;
import lombok.NonNull;
import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
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
public final class OpenAiClient implements Closeable {

	// TODO Expose features described in openai-java (e.g. Proxy)
	// TODO add client/endpoint for Azure OpenAi Services

	private final static String API_BASE_URL = "https://api.openai.com/v1/";

	public final static int DEFAULT_TIMEOUT_MILLIS = 60 * 1000;

	public final static int DEFAULT_KEEP_ALIVE_MILLIS = 5 * 60 * 1000;

	public final static int DEFAULT_MAX_IDLE_CONNECTIONS = 5;

	// OpenAI API defined with Retrofit
	private final OpenAiApi api;

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
	 * Constructor, using default parameters for OkHttpClient. OpenAiApi key is read
	 * from OPENAI_API_KEY system environment variable.
	 */
	public OpenAiClient() {
		this(OpenAiClient.getDefaultHttpClient(null, -1, -1, -1));
	}

	/**
	 * Constructor, using default parameters for OkHttpClient.
	 * 
	 * @param apiKey OpenAiApi key. If this is null, it will try to read it from
	 *               OPENAI_API_KEY system environment variable.
	 */
	public OpenAiClient(String apiKey) {
		this(OpenAiClient.getDefaultHttpClient(apiKey, -1, -1, -1));
	}

	/**
	 * Constructor. This client uses an underlying OkHttpClient for API calls, which
	 * parameters can be specified.
	 * 
	 * @param apiKey             OpenAiApi key. If this is null, it will try to read
	 *                           it from OPENAI_API_KEY system environment variable.
	 * @param readTimeout        Read timeout for connections. 0 means no timeout,
	 *                           if a value < 0 is provided, then
	 *                           {@link DEFAULT_TIMEOUT_MILLIS} is used.
	 * @param keepAliveDuration  Timeout for connections in client pool
	 *                           (milliseconds). If this is < 0,
	 *                           {@link #DEFAULT_KEEP_ALIVE_MILLIS} is used instead.
	 * @param maxIdleConnections Maximum number of idle connections to keep in the
	 *                           pool. If this is < 0,
	 *                           {@link #DEFAULT_MAX_IDLE_CONNECTIONS} is used
	 *                           instead.
	 */
	public OpenAiClient(String apiKey, int readTimeout, int keepAliveDuration, int maxIdleConnections) {
		this(OpenAiClient.getDefaultHttpClient(apiKey, readTimeout, keepAliveDuration, maxIdleConnections));
	}

	/**
	 * Constructor. This client uses provided OkHttpClient for API calls, to allow
	 * full customization {@link #getDefaultHttpClient(String, int, int, int)}
	 */
	public OpenAiClient(OkHttpClient http) {

		client = http;

		Retrofit retrofit = new Retrofit.Builder().baseUrl(API_BASE_URL).client(client)
				.addConverterFactory(JacksonConverterFactory.create(JSON_MAPPER))
				.addCallAdapterFactory(RxJava2CallAdapterFactory.create()).build();

		api = retrofit.create(OpenAiApi.class);
	}

	/**
	 * Returns an OkHttpClient to use for API calls, using default parameters. This
	 * can be further customized and then used in constructor to build an
	 * OpenAiClient.
	 * 
	 * @param apiKey OpenAiApi key. If this is null, it will try to read it from
	 *               OPENAI_API_KEY system environment variable.
	 */
	public static OkHttpClient getDefaultHttpClient(String apiKey) {
		return getDefaultHttpClient(apiKey, -1, -1, -1);
	}

	/**
	 * Returns an OkHttpClient to use for API calls. This can be further customized
	 * and then used in constructor to build an OpenAiClient.
	 * 
	 * @param apiKey             OpenAiApi key. If this is null, it will try to read
	 *                           it from OPENAI_API_KEY system environment variable.
	 * @param readTimeout        Read timeout for connections. 0 means no timeout,
	 *                           if a value < 0 is provided, then
	 *                           {@link DEFAULT_TIMEOUT_MILLIS} is used.
	 * @param keepAliveDuration  Timeout for connections in client pool
	 *                           (milliseconds). If this is < 0,
	 *                           {@link #DEFAULT_KEEP_ALIVE_MILLIS} is used instead.
	 * @param maxIdleConnections Maximum number of idle connections to keep in the
	 *                           pool. If this is < 0,
	 *                           {@link #DEFAULT_MAX_IDLE_CONNECTIONS} is used
	 *                           instead.
	 */
	public static OkHttpClient getDefaultHttpClient(String apiKey, int readTimeout, int keepAliveDuration,
			int maxIdleConnections) {

		if (apiKey == null)
			apiKey = System.getenv("OPENAI_API_KEY");
		if (apiKey == null)
			throw new IllegalArgumentException(
					"OpenAi API key is not provided and it cannot be found in OPENAI_API_KEY system environment variable");
		final String bearer = "Bearer " + apiKey;

		if (readTimeout < 0)
			readTimeout = DEFAULT_TIMEOUT_MILLIS;
		if (keepAliveDuration < 0)
			keepAliveDuration = DEFAULT_KEEP_ALIVE_MILLIS;
		if (maxIdleConnections < 0)
			maxIdleConnections = DEFAULT_MAX_IDLE_CONNECTIONS;

		return new OkHttpClient.Builder()
				.connectionPool(new ConnectionPool(maxIdleConnections, keepAliveDuration, TimeUnit.MILLISECONDS))
				.readTimeout(readTimeout, TimeUnit.MILLISECONDS).addInterceptor(new Interceptor() {
					@Override
					public Response intercept(Chain chain) throws IOException {
						return chain.proceed(chain.request().newBuilder().header("Authorization", bearer).build());
					}
				}).build();
	}

	//////// API METHODS MAPPED INTO JAVA CALLS ////////////////////////////////////

	public List<Model> listModels() {
		return callApi(api.models()).getData();
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

	public EditsResponse createEdit(EditsRequest req) {
		return callApi(api.edits(req));
	}

	public List<Image> createImage(ImagesRequest req) {
		return callApi(api.imagesGenerations(req)).getData();
	}

	public List<Image> createImageEdit(@NonNull BufferedImage image, ImagesRequest req, BufferedImage mask)
			throws IOException {

		MultipartBody.Builder builder = new MultipartBody.Builder().setType(MediaType.get("multipart/form-data"))
				.addFormDataPart("image", "image", ImageUtil.toRequestBody("png", image));

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

		return callApi(api.imagesEdits(builder.build())).getData();
	}

	public List<Image> createImageVariation(@NonNull BufferedImage image, ImagesRequest req) throws IOException {

		MultipartBody.Builder builder = new MultipartBody.Builder().setType(MediaType.get("multipart/form-data"))
				.addFormDataPart("image", "image", ImageUtil.toRequestBody("png", image));

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

		return callApi(api.imagesVariations(builder.build())).getData();
	}

	public EmbeddingsResponse createEmbeddings(EmbeddingsRequest req) {
		return callApi(api.embeddings(req));
	}

	public String createTranscription(@NonNull java.io.File audio, AudioRequest req) throws IOException {
		try (InputStream is = new FileInputStream(audio)) {
			return createTranscription(is, req);
		}
	}

	public String createTranscription(@NonNull String fileName, AudioRequest req) throws IOException {
		try (InputStream is = new FileInputStream(fileName)) {
			return createTranscription(is, req);
		}
	}

	public String createTranscription(@NonNull InputStream audio, AudioRequest req) throws IOException {
		return createTranscription(audio.readAllBytes(), req);
	}

	public String createTranscription(@NonNull byte[] audio, AudioRequest req) {

		MultipartBody.Builder builder = new MultipartBody.Builder().setType(MediaType.get("multipart/form-data"))
				.addFormDataPart("file", "file", RequestBody.create(MediaType.parse("audio/*"), audio));

		if (req.getModel() != null) {
			builder.addFormDataPart("model", req.getModel());
		} else {
			throw new IllegalArgumentException("Model cannot be null");
		}
		if (req.getPrompt() != null) {
			builder.addFormDataPart("prompt", req.getPrompt());
		}
		if (req.getResponseFormat() != null) {
			builder.addFormDataPart("response_format", req.getResponseFormat().toString());
		}
		if (req.getTemperature() != null) {
			builder.addFormDataPart("temperature", req.getTemperature().toString());
		}
		if (req.getLanguage() != null) {
			builder.addFormDataPart("language", req.getLanguage());
		}

		return callApi(api.audioTranscriptions(builder.build())).getText();
	}

	public String createTranslation(@NonNull java.io.File audio, AudioRequest req) throws IOException {
		try (InputStream is = new FileInputStream(audio)) {
			return createTranslation(is, req);
		}
	}

	public String createTranslation(@NonNull String fileName, AudioRequest req) throws IOException {
		try (InputStream is = new FileInputStream(fileName)) {
			return createTranslation(is, req);
		}
	}

	public String createTranslation(@NonNull InputStream audio, AudioRequest req) throws IOException {
		return createTranslation(audio.readAllBytes(), req);
	}

	public String createTranslation(@NonNull byte[] audio, AudioRequest req) {

		MultipartBody.Builder builder = new MultipartBody.Builder().setType(MediaType.get("multipart/form-data"))
				.addFormDataPart("file", "file", RequestBody.create(MediaType.parse("audio/*"), audio));

		if (req.getModel() != null) {
			builder.addFormDataPart("model", req.getModel());
		} else {
			throw new IllegalArgumentException("Model cannot be null");
		}
		if (req.getPrompt() != null) {
			builder.addFormDataPart("prompt", req.getPrompt());
		}
		if (req.getResponseFormat() != null) {
			builder.addFormDataPart("response_format", req.getResponseFormat().toString());
		}
		if (req.getTemperature() != null) {
			builder.addFormDataPart("temperature", req.getTemperature().toString());
		}

		return callApi(api.audioTranscriptions(builder.build())).getText();
	}

	public List<File> listFiles() {
		return callApi(api.files()).getData();
	}

	public File uploadFile(@NonNull String fileName, @NonNull String purpose) throws IOException {
		return uploadFile(new java.io.File(fileName), purpose);
	}

	public File uploadFile(@NonNull java.io.File jsonl, @NonNull String purpose) throws IOException {
		try (InputStream is = new FileInputStream(jsonl)) {
			return uploadFile(is, jsonl.getName(), purpose);
		}
	}

	public File uploadFile(@NonNull InputStream jsonl, @NonNull String fileName, @NonNull String purpose)
			throws IOException {

		return uploadFile(jsonl.readAllBytes(), fileName, purpose);
	}

	public File uploadFile(@NonNull byte[] jsonl, @NonNull String fileName, @NonNull String purpose) {

		MultipartBody.Builder builder = new MultipartBody.Builder().setType(MediaType.get("multipart/form-data"))
				.addFormDataPart("file", fileName, RequestBody.create(MediaType.parse("text/jsonl"), jsonl))
				.addFormDataPart("purpose", purpose);

		return callApi(api.files(builder.build()));
	}

	public DeleteResponse deleteFile(@NonNull String fileId) {
		return callApi(api.filesDelete(fileId));
	}

	public File retrieveFile(@NonNull String fileId) {
		return callApi(api.files(fileId));
	}

	/**
	 * Retrieves a file.
	 * 
	 * The file content is stored in memory, this might cause OutOfMemory errors if
	 * the file is too big.
	 * 
	 * @param fileId Id of file to retrieve.
	 * @throws IOException
	 */
	public byte[] retrieveFileContent(String fileId) throws IOException {
		return callApi(api.filesContent(fileId)).bytes();
	}

	/**
	 * Downloads a file.
	 * 
	 * @param fileId         Id of file to retrieve.
	 * @param downloadedFile A File where the contents of retrieved file will be
	 *                       stored.
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public void retrieveFileContent(String fileId, java.io.File downloadedFile)
			throws FileNotFoundException, IOException {

		try (InputStream is = callApi(api.filesContent(fileId)).byteStream();
				FileOutputStream os = new FileOutputStream(downloadedFile)) {

			byte[] buffer = new byte[4096];
			int lengthRead;
			while ((lengthRead = is.read(buffer)) > 0) {
				os.write(buffer, 0, lengthRead);
			}
		}
	}

	public FineTune createFineTune(FineTunesRequest req) {
		return callApi(api.fineTunesCreate(req));
	}

	public List<FineTune> listFineTunes() {
		return callApi(api.fineTunes()).getData();
	}

	public FineTune retrieveFineTune(String fineTuneId) {
		return callApi(api.fineTunes(fineTuneId));
	}

	public FineTune cancelFineTune(String fineTuneId) {
		return callApi(api.fineTunesCancel(fineTuneId));
	}

	public List<FineTuneEvent> listFineTuneEvents(String fineTuneId) {
		return callApi(api.fineTunesEvents(fineTuneId)).getData();
	}

	public DeleteResponse deleteFineTuneModel(String model) {
		return callApi(api.modelsDelete(model));
	}

	public ModerationsResponse createModeration(ModerationsRequest req) {
		return callApi(api.moderations(req));
	}

	/////////////////////////////////////////////////////////////////////////////////

	private <T> T callApi(Single<T> apiCall) {
		try {
			return apiCall.blockingGet();
		} catch (HttpException e) {
			OpenAiException oaie;
			try {
				oaie = new OpenAiException(JSON_MAPPER.readValue(e.response().errorBody().string(), OpenAiError.class),
						e);
			} catch (Exception ex) {
				throw e;
			}
			throw oaie;
		}
	}

	@Override
	public void close() {
		try {
			client.dispatcher().executorService().shutdown();
			client.connectionPool().evictAll();
			client.cache().close();
		} catch (Exception e) {
			// TODO log here
		}
	}
}
