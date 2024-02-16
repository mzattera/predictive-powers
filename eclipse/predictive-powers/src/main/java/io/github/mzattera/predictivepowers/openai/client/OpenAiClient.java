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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import io.github.mzattera.predictivepowers.ApiClient;
import io.github.mzattera.predictivepowers.openai.client.assistants.Assistant;
import io.github.mzattera.predictivepowers.openai.client.assistants.AssistantsRequest;
import io.github.mzattera.predictivepowers.openai.client.audio.AudioRequest;
import io.github.mzattera.predictivepowers.openai.client.audio.AudioSpeechRequest;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsResponse;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsResponse;
import io.github.mzattera.predictivepowers.openai.client.embeddings.EmbeddingsRequest;
import io.github.mzattera.predictivepowers.openai.client.embeddings.EmbeddingsResponse;
import io.github.mzattera.predictivepowers.openai.client.files.File;
import io.github.mzattera.predictivepowers.openai.client.finetuning.FineTuningJob;
import io.github.mzattera.predictivepowers.openai.client.finetuning.FineTuningJobEvent;
import io.github.mzattera.predictivepowers.openai.client.finetuning.FineTuningRequest;
import io.github.mzattera.predictivepowers.openai.client.images.Image;
import io.github.mzattera.predictivepowers.openai.client.images.ImagesRequest;
import io.github.mzattera.predictivepowers.openai.client.models.Model;
import io.github.mzattera.predictivepowers.openai.client.moderations.ModerationsRequest;
import io.github.mzattera.predictivepowers.openai.client.moderations.ModerationsResponse;
import io.github.mzattera.predictivepowers.openai.client.threads.Message;
import io.github.mzattera.predictivepowers.openai.client.threads.MessageFile;
import io.github.mzattera.predictivepowers.openai.client.threads.MessagesRequest;
import io.github.mzattera.predictivepowers.openai.client.threads.OpenAiThread;
import io.github.mzattera.predictivepowers.openai.client.threads.Run;
import io.github.mzattera.predictivepowers.openai.client.threads.RunStep;
import io.github.mzattera.predictivepowers.openai.client.threads.RunsRequest;
import io.github.mzattera.predictivepowers.openai.client.threads.ThreadAndRunRequest;
import io.github.mzattera.predictivepowers.openai.client.threads.ThreadsRequest;
import io.github.mzattera.predictivepowers.openai.client.threads.ToolOutputsRequest;
import io.github.mzattera.predictivepowers.services.messages.FilePart;
import io.github.mzattera.util.FileUtil;
import io.github.mzattera.util.ImageUtil;
import io.reactivex.Single;
import lombok.Getter;
import lombok.NonNull;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import retrofit2.HttpException;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * API Client to access OpenAI API.
 * 
 * See {@link https://platform.openai.com/docs/api-reference} for details.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class OpenAiClient implements ApiClient {

	// TODO add client/endpoint for Azure OpenAI Services

	private final static Logger LOG = LoggerFactory.getLogger(OpenAiClient.class);

	/**
	 * Name of the OS environment variable containing the API key.
	 */
	public static final String OS_ENV_VAR_NAME = "OPENAI_API_KEY";

	public final static int DEFAULT_TIMEOUT_MILLIS = 3 * 60 * 1000;
	public final static int DEFAULT_MAX_RETRIES = 10;
	public final static int DEFAULT_KEEP_ALIVE_MILLIS = 5 * 60 * 1000;
	public final static int DEFAULT_MAX_IDLE_CONNECTIONS = 5;

	private final static String API_BASE_URL = "https://api.openai.com/v1/";

	// OpenAI API defined with Retrofit
	private final OpenAiApi api;

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
	 * Constructor, using default parameters for OkHttpClient. OpenAI API key is
	 * read from {@link #OS_ENV_VAR_NAME} system environment variable.
	 */
	public OpenAiClient() {
		this(null, DEFAULT_TIMEOUT_MILLIS, DEFAULT_MAX_RETRIES, DEFAULT_KEEP_ALIVE_MILLIS,
				DEFAULT_MAX_IDLE_CONNECTIONS);
	}

	/**
	 * Constructor, using default parameters for OkHttpClient.
	 * 
	 * @param apiKey OpenAiApi key. If this is null, it will try to read it from
	 *               {@link #OS_ENV_VAR_NAME} system environment variable.
	 */
	public OpenAiClient(String apiKey) {
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
	public OpenAiClient(String apiKey, int readTimeout, int maxRetries, int keepAliveDuration, int maxIdleConnections) {
		this(ApiClient.getDefaultHttpClient((apiKey == null) ? getApiKey() : apiKey, readTimeout, maxRetries,
				keepAliveDuration, maxIdleConnections));
	}

	/**
	 * Constructor. This client uses provided OkHttpClient for API calls, to allow
	 * full customization (see
	 * {@link ApiClient#getDefaultHttpClient(String, int, int, int, int)}).
	 */
	public OpenAiClient(OkHttpClient http) {

//		client = http;

		// Debug code below
		client = http.newBuilder() //

				// Debug code below, outputs the request
				.addInterceptor(new Interceptor() {

					@Override
					public Response intercept(Chain chain) throws IOException {
						Request req = chain.request();

						if (req.body() != null) {
							Buffer buffer = new Buffer();
							req.body().writeTo(buffer);
							String in = buffer.readUtf8();
							String bodyContent = "";
							try {
								// In case body is not JSON
								bodyContent = jsonMapper.writerWithDefaultPrettyPrinter()
										.writeValueAsString(jsonMapper.readTree(in));
							} catch (Exception e) {
								bodyContent = in;
							}
							System.out.println("Request body: " + bodyContent);
						}

						return chain.proceed(req);
					}
				}) //

				// Debug code below, outputs the response
				.addInterceptor(new Interceptor() {

					@Override
					public Response intercept(Chain chain) throws IOException {

						Response response = chain.proceed(chain.request());
						if (response.body() != null) {
							BufferedSource source = response.body().source();
							source.request(Long.MAX_VALUE);

							@SuppressWarnings("deprecation")
							Buffer buffer = source.buffer();

							String in = buffer.clone().readUtf8();
							String bodyContent = "";
							try {
								// In case body is not JSON
								bodyContent = jsonMapper.writerWithDefaultPrettyPrinter()
										.writeValueAsString(jsonMapper.readTree(in));
							} catch (Exception e) {
								bodyContent = in;
							}
							System.out.println("Response body: " + bodyContent);
						}

						return response; // Return the original response unaltered
					}
				}) //

				.build();

		Retrofit retrofit = new Retrofit.Builder().baseUrl(API_BASE_URL).client(client)
				.addConverterFactory(JacksonConverterFactory.create(jsonMapper))
				.addCallAdapterFactory(RxJava2CallAdapterFactory.create()).build();

		api = retrofit.create(OpenAiApi.class);
	}

	/**
	 * @return The API key from OS environment.
	 */
	public static String getApiKey() {
		String apiKey = System.getenv(OS_ENV_VAR_NAME);
		if (apiKey == null)
			throw new IllegalArgumentException("OpenAI API key is not provided and it cannot be found in "
					+ OS_ENV_VAR_NAME + " system environment variable");
		return apiKey;
	}

	//////// API METHODS MAPPED INTO JAVA CALLS ////////////////////////////////////

	public List<Model> listModels() {
		return callApi(api.models()).getData();
	}

	public Model retrieveModel(@NonNull String modelId) {
		return callApi(api.models(modelId));
	}

	public CompletionsResponse createCompletion(@NonNull CompletionsRequest req) {
		return callApi(api.completions(req));
	}

	public ChatCompletionsResponse createChatCompletion(@NonNull ChatCompletionsRequest req) {
		return callApi(api.chatCompletions(req));
	}

	public List<Image> createImage(@NonNull ImagesRequest req) {
		return callApi(api.imagesGenerations(req)).getData();
	}

	public List<Image> createImageEdit(@NonNull BufferedImage image, @NonNull ImagesRequest req, BufferedImage mask)
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

	public List<Image> createImageVariation(@NonNull BufferedImage image, @NonNull ImagesRequest req)
			throws IOException {

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

	public EmbeddingsResponse createEmbeddings(@NonNull EmbeddingsRequest req) {
		return callApi(api.embeddings(req));
	}

	// TODO Add streaming for TTS

	/**
	 * Generates audio from the input text.
	 * 
	 * The output file content is stored in memory, this might cause OutOfMemory
	 * errors if the file is too big.
	 */
	public byte[] createSpeech(@NonNull AudioSpeechRequest req) throws IOException {
		return callApi(api.audioSpeech(req)).bytes();
	}

	/**
	 * Generates audio from the input text and downloads it into a file.
	 * 
	 * @param downloadedFile A File where the contents of generated audio will be
	 *                       stored.
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public void createSpeech(AudioSpeechRequest req, @NonNull java.io.File downloadedFile)
			throws FileNotFoundException, IOException {
		download(callApi(api.audioSpeech(req)), downloadedFile);
	}

	public String createTranscription(@NonNull java.io.File audio, @NonNull AudioRequest req) throws IOException {
		try (InputStream is = new FileInputStream(audio)) {
			return createTranscription(is, FileUtil.getExtension(audio), req);
		}
	}

	public String createTranscription(@NonNull String fileName, @NonNull AudioRequest req) throws IOException {
		try (InputStream is = new FileInputStream(fileName)) {
			return createTranscription(is, FileUtil.getExtension(fileName), req);
		}
	}

	/**
	 * Creates an audio transcription.
	 * 
	 * @param audio  Audio stream.
	 * @param format Format of the audio stream (e.g. wav).
	 * @param req
	 * 
	 * @return A transcription of given stream.
	 */
	public String createTranscription(@NonNull InputStream audio, @NonNull String format, @NonNull AudioRequest req)
			throws IOException {
		return createTranscription(audio.readAllBytes(), format, req);
	}

	/**
	 * Creates an audio transcription.
	 * 
	 * @param audio  Audio stream.
	 * @param format Format of the audio stream (e.g. wav).
	 * @param req
	 * 
	 * @return A transcription of given stream.
	 */
	public String createTranscription(@NonNull byte[] audio, @NonNull String format, @NonNull AudioRequest req) {

		MultipartBody.Builder builder = new MultipartBody.Builder().setType(MediaType.get("multipart/form-data"))
				.addFormDataPart("model", req.getModel())
				.addFormDataPart("file", "file." + format, RequestBody.create(MediaType.parse("audio/*"), audio));

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

	public String createTranslation(@NonNull java.io.File audio, @NonNull AudioRequest req) throws IOException {
		try (InputStream is = new FileInputStream(audio)) {
			return createTranslation(is, FileUtil.getExtension(audio), req);
		}
	}

	public String createTranslation(@NonNull String fileName, @NonNull AudioRequest req) throws IOException {
		try (InputStream is = new FileInputStream(fileName)) {
			return createTranslation(is, FileUtil.getExtension(fileName), req);
		}
	}

	/**
	 * Creates an audio translation.
	 * 
	 * @param audio  Audio stream.
	 * @param format Format of the audio stream (e.g. wav).
	 * @param req
	 * 
	 * @return A translation of given stream.
	 */
	public String createTranslation(@NonNull InputStream audio, @NonNull String format, @NonNull AudioRequest req)
			throws IOException {
		return createTranslation(audio.readAllBytes(), format, req);
	}

	public String createTranslation(@NonNull byte[] audio, @NonNull String format, @NonNull AudioRequest req) {

		MultipartBody.Builder builder = new MultipartBody.Builder().setType(MediaType.get("multipart/form-data"))
				.addFormDataPart("model", req.getModel())
				.addFormDataPart("file", "file." + format, RequestBody.create(MediaType.parse("audio/*"), audio));

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

		return callApi(api.audioTranslations(builder.build())).getText();
	}

	public List<File> listFiles() {
		return callApi(api.files()).getData();
	}

	public File uploadFile(@NonNull String fileName, @NonNull String purpose) throws IOException {
		return uploadFile(new java.io.File(fileName), purpose);
	}

	public File uploadFile(@NonNull java.io.File file, @NonNull String purpose) throws IOException {
		try (InputStream is = new FileInputStream(file)) {
			return uploadFile(is, file.getName(), purpose);
		}
	}

	public File uploadFile(@NonNull FilePart file, @NonNull String purpose)
			throws IOException {

		return uploadFile(file.getInputStream(), file.getName(), purpose);
	}

	public File uploadFile(@NonNull InputStream file, @NonNull String fileName, @NonNull String purpose)
			throws IOException {

		return uploadFile(file.readAllBytes(), fileName, purpose);
	}

	public File uploadFile(@NonNull byte[] file, @NonNull String fileName, @NonNull String purpose) {

		MultipartBody.Builder builder = new MultipartBody.Builder().setType(MediaType.get("multipart/form-data"))
				.addFormDataPart("file", fileName, RequestBody.create(MediaType.parse("text/file"), file))
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
	public byte[] retrieveFileContent(@NonNull String fileId) throws IOException {
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
	public void retrieveFileContent(String fileId, @NonNull java.io.File downloadedFile)
			throws FileNotFoundException, IOException {
		download(callApi(api.filesContent(fileId)), downloadedFile);
	}

	public FineTuningJob createFineTuningJob(@NonNull FineTuningRequest req) {
		return callApi(api.fineTuningJobsCreate(req));
	}

	public DataList<FineTuningJob> listFineTuningJobs() {
		return callApi(api.fineTuningJobs(null, null));
	}

	public DataList<FineTuningJob> listFineTuningJobs(Integer limit, String after) {
		return callApi(api.fineTuningJobs(limit, after));
	}

	public DataList<FineTuningJobEvent> listFineTuningEvents(@NonNull String fineTuningJobId) {
		return callApi(api.fineTuningJobsEvents(fineTuningJobId, null, null));
	}

	public DataList<FineTuningJobEvent> listFineTuningEvents(@NonNull String fineTuningJobId, Integer limit,
			String after) {
		return callApi(api.fineTuningJobsEvents(fineTuningJobId, limit, after));
	}

	public FineTuningJob retrieveFineTuningJob(@NonNull String fineTuningJobId) {
		return callApi(api.fineTuningJobsGet(fineTuningJobId));
	}

	public FineTuningJob cancelFineTuning(@NonNull String fineTuningJobId) {
		return callApi(api.fineTuningJobsCancel(fineTuningJobId));
	}

	public DeleteResponse deleteFineTunedModel(@NonNull String model) {
		return callApi(api.modelsDelete(model));
	}

	public ModerationsResponse createModeration(@NonNull ModerationsRequest req) {
		return callApi(api.moderations(req));
	}

	public Assistant createAssistant(@NonNull AssistantsRequest req) {
		return callApi(api.assistantsCreate(req));
	}

	public File createAssistantFile(@NonNull String assistantId, @NonNull String fileId) {
		Map<String, String> body = new HashMap<>();
		body.put("file_id", fileId);
		return callApi(api.assistantsFiles(assistantId, body));
	}

	public DataList<Assistant> listAssistants() {
		return callApi(api.assistants(null, null, null, null));
	}

	public DataList<Assistant> listAssistants(SortOrder sort, Integer limit, String before, String after) {
		return callApi(api.assistants(limit, sort.toString(), after, before));
	}

	public DataList<File> listAssistantFiles(@NonNull String assistantId) {
		return callApi(api.assistantsFiles(assistantId, null, null, null, null));
	}

	public DataList<File> listAssistantFiles(@NonNull String assistantId, SortOrder sort, Integer limit, String before,
			String after) {
		return callApi(api.assistantsFiles(assistantId, limit, sort.toString(), after, before));
	}

	/**
	 * Retrieves an assistant from OpenAI.
	 * 
	 * Notice that tool parameters for any tool attached to the agent are not
	 * properly de-serialized, so they will always be empty.
	 * 
	 * Unfortunately, there is no easy workaround as it is not easy to de-serialize
	 * a JSON schema.
	 * 
	 * @param assistantId
	 * @return
	 */
	public Assistant retrieveAssistant(@NonNull String assistantId) {
		return callApi(api.assistantsGet(assistantId));
	}

	public File retrieveAssistantFile(@NonNull String assistantId, @NonNull String fileId) {
		return callApi(api.assistantsFilesGet(assistantId, fileId));
	}

	public Assistant modifyAssistant(@NonNull String assistantId, @NonNull AssistantsRequest req) {
		return callApi(api.assistantsModify(assistantId, req));
	}

	public DeleteResponse deleteAssistant(@NonNull String assistantId) {
		return callApi(api.assistantsDelete(assistantId));
	}

	public DeleteResponse deteAssistantFile(@NonNull String assistantId, @NonNull String fileId) {
		return callApi(api.assistantsFilesDelete(assistantId, fileId));
	}

	public OpenAiThread createThread(@NonNull ThreadsRequest req) {
		return callApi(api.threads(req));
	}

	public OpenAiThread retrieveThread(@NonNull String threadId) {
		return callApi(api.threadsGet(threadId));
	}

	public OpenAiThread modifyThread(@NonNull String threadId, @NonNull Metadata metadata) {
		return callApi(api.threadsModify(threadId, metadata));
	}

	public OpenAiThread modifyThread(@NonNull String threadId, @NonNull Map<String, String> metadata) {
		return callApi(api.threadsModify(threadId, new Metadata(metadata)));
	}

	public DeleteResponse deleteThread(@NonNull String threadId) {
		return callApi(api.threadsDelete(threadId));
	}

	public Message createMessage(@NonNull String threadId, @NonNull MessagesRequest req) {
		return callApi(api.threadsMessagesCreate(threadId, req));
	}

	public DataList<Message> listMessages(@NonNull String threadId) {
		return callApi(api.threadsMessages(threadId, null, null, null, null));
	}

	public DataList<Message> listMessages(@NonNull String threadId, SortOrder order, Integer limit, String after,
			String before) {
		return callApi(api.threadsMessages(threadId, limit, order.toString(), after, before));
	}

	public DataList<MessageFile> listMessageFiles(@NonNull String threadId, @NonNull String messageId) {
		return callApi(api.threadsMessagesFiles(threadId, messageId, null, null, null, null));
	}

	public DataList<MessageFile> listMessageFiles(@NonNull String threadId, @NonNull String messageId, SortOrder order,
			Integer limit, String after, String before) {
		return callApi(api.threadsMessagesFiles(threadId, messageId, limit, order.toString(), after, before));
	}

	public Message retrieveMessage(@NonNull String threadId, @NonNull String messageId) {
		return callApi(api.threadsMessagesGet(threadId, messageId));
	}

	public MessageFile retrieveMessageFile(@NonNull String threadId, @NonNull String messageId,
			@NonNull String fileId) {
		return callApi(api.threadsMessagesFiles(threadId, messageId, fileId));
	}

	public Message modifyMessage(@NonNull String threadId, @NonNull String messageId, @NonNull Metadata metadata) {
		return callApi(api.threadsMessagesModify(threadId, messageId, metadata));
	}

	public Run createRun(@NonNull String threadId, @NonNull RunsRequest req) {
		return callApi(api.threadsRunsCreate(threadId, req));
	}

	public Run createThreadAndRun(@NonNull ThreadAndRunRequest req) {
		return callApi(api.threadsRunsCreate(req));
	}

	public DataList<Run> listRuns(@NonNull String threadId, Integer limit, String order, String after, String before) {
		return callApi(api.threadsRuns(threadId, limit, order, after, before));
	}

	public DataList<RunStep> listRunSteps(@NonNull String threadId, @NonNull String runId, Integer limit, String order,
			String after, String before) {
		return callApi(api.threadsRunsSteps(threadId, runId, limit, order, after, before));
	}

	public Run retrieveRun(@NonNull String threadId, @NonNull String runId) {
		return callApi(api.threadsRunsGet(threadId, runId));
	}

	public RunStep retrieveRunStep(@NonNull String threadId, @NonNull String runId, @NonNull String stepId) {
		return callApi(api.threadsRunsStepsGet(threadId, runId, stepId));
	}

	public Run modifyRun(@NonNull String threadId, @NonNull String runId, @NonNull Metadata metadata) {
		return callApi(api.threadsRunsModify(threadId, runId, metadata));
	}

	public Run submitToolOutputsToRun(@NonNull String threadId, @NonNull String runId,
			@NonNull ToolOutputsRequest req) {
		return callApi(api.threadsRunsSubmitToolOutputs(threadId, runId, req));
	}

	public Run cancelRun(@NonNull String threadId, @NonNull String runId) {
		return callApi(api.threadsRunsCancel(threadId, runId));
	}

	/////////////////////////////////////////////////////////////////////////////////

	private <T> T callApi(Single<T> apiCall) {
		try {
			return apiCall.blockingGet();
		} catch (HttpException e) {

			OpenAiException oaie;
			try {
				oaie = new OpenAiException(e);
			} catch (Exception ex) {
				throw e;
			}
			throw oaie;
		}
	}

	/**
	 * Downloads a file, returned by an HTTP response.
	 */
	private void download(ResponseBody body, java.io.File downloadedFile) throws IOException {
		try (InputStream is = body.byteStream(); FileOutputStream os = new FileOutputStream(downloadedFile)) {

			byte[] buffer = new byte[4096];
			int lengthRead;
			while ((lengthRead = is.read(buffer)) > 0) {
				os.write(buffer, 0, lengthRead);
			}
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
