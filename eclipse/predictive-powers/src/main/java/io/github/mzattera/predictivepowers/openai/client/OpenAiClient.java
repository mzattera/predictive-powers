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
package io.github.mzattera.predictivepowers.openai.client;

import java.awt.image.BufferedImage;
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
public final class OpenAiClient {

	// TODO Expose features descibed in openai-java
	// TODO chnge to use inside Azure
	
	private final static String API_BASE_URL = "https://api.openai.com/v1/";

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

	public File uploadFile(@NonNull java.io.File jsonl, @NonNull String purpose) throws IOException {
		try (InputStream is = new FileInputStream(jsonl)) {
			return uploadFile(is, jsonl.getName(), purpose);
		}
	}

	public File uploadFile(@NonNull String fileName, @NonNull String purpose) {

		MultipartBody.Builder builder = new MultipartBody.Builder().setType(MediaType.get("multipart/form-data"))
				.addFormDataPart("file", fileName, RequestBody.create(MediaType.parse("text/jsonl"), fileName))
				.addFormDataPart("purpose", purpose);

		return callApi(api.files(builder.build()));
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
				oaie = new OpenAiException(mapper.readValue(e.response().errorBody().string(), OpenAiError.class), e);
			} catch (Exception ex) {
				throw e;
			}
			throw oaie;
		}
	}
}
