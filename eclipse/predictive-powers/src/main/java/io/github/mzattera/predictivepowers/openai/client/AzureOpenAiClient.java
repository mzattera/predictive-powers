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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mzattera.predictivepowers.ApiClient;
import io.github.mzattera.predictivepowers.openai.client.assistants.Assistant;
import io.github.mzattera.predictivepowers.openai.client.assistants.AssistantsRequest;
import io.github.mzattera.predictivepowers.openai.client.audio.AudioResponse;
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
import io.reactivex.Single;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import okhttp3.Interceptor;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.HttpException;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * API Client to accessAzure OpenAI Cognitive Service.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class AzureOpenAiClient extends OpenAiClient {

	private final static Logger LOG = LoggerFactory.getLogger(AzureOpenAiClient.class);

	/**
	 * Name of the OS environment variable containing the API key.
	 */
	public static final String OS_ENV_VAR_NAME_API_KEY = "AZURE_OPENAI_API_KEY";

	/**
	 * Name of the OS environment variable containing Azure OpenAI Service resource
	 * name.
	 */
	public static final String OS_ENV_VAR_NAME_RESOURCE = "AZURE_OPENAI_RESOURCE_NAME";

	public final static int DEFAULT_TIMEOUT_MILLIS = 3 * 60 * 1000;
	public final static int DEFAULT_MAX_RETRIES = 10;
	public final static int DEFAULT_KEEP_ALIVE_MILLIS = 5 * 60 * 1000;
	public final static int DEFAULT_MAX_IDLE_CONNECTIONS = 5;

	@Getter
	private final String azureResourceName;

	/**
	 * This is stable API version that all calls through this client will use when
	 * accessing stable APIs.
	 */
	@Getter
	@Setter
	private String stableApiVersion = "2023-05-15";

	/**
	 * This is preview API version that all calls through this client will use when
	 * accessing APIs that are available only as preview.
	 */
	@Getter
	@Setter
	private String previewApiVersion = "2024-02-15-preview";

	// OpenAI API defined with Retrofit
	private final AzureOpenAiApi api;

	private final OkHttpClient client;

	/**
	 * Constructor, using default parameters for OkHttpClient.
	 * 
	 * OpenAI API key is read from {@link #OS_ENV_VAR_NAME_API_KEY} system
	 * environment variable.
	 * 
	 * Azure OpenAI Service resource name is read from
	 * {@link #OS_ENV_VAR_NAME_RESOURCE} system environment variable.
	 */
	public AzureOpenAiClient() {
		this(null, null, DEFAULT_TIMEOUT_MILLIS, DEFAULT_MAX_RETRIES, DEFAULT_KEEP_ALIVE_MILLIS,
				DEFAULT_MAX_IDLE_CONNECTIONS);
	}

	/**
	 * Constructor, using default parameters for OkHttpClient.
	 * 
	 * OpenAI API key is read from {@link #OS_ENV_VAR_NAME_API_KEY} system
	 * environment variable.
	 * 
	 * @param resourceName Resource name for the Azure OpenAI Service to connect to.
	 *                     If null, it will be read from
	 *                     {@link #OS_ENV_VAR_NAME_RESOURCE} system environment
	 *                     variable.
	 */
	public AzureOpenAiClient(String resourceName) {
		this(resourceName, null, DEFAULT_TIMEOUT_MILLIS, DEFAULT_MAX_RETRIES, DEFAULT_KEEP_ALIVE_MILLIS,
				DEFAULT_MAX_IDLE_CONNECTIONS);
	}

	/**
	 * Constructor, using default parameters for OkHttpClient.
	 * 
	 * @param apiKey       OpenAiApi key. If this is null, it will try to read it
	 *                     from {@link #OS_ENV_VAR_NAME_API_KEY} system environment
	 *                     variable.
	 * 
	 * 
	 * @param resourceName Resource name for the Azure OpenAI Service to connect to.
	 *                     If null, it will be read from
	 *                     {@link #OS_ENV_VAR_NAME_RESOURCE} system environment
	 *                     variable.
	 */
	public AzureOpenAiClient(String resourceName, String apiKey) {
		this(resourceName, apiKey, DEFAULT_TIMEOUT_MILLIS, DEFAULT_MAX_RETRIES, DEFAULT_KEEP_ALIVE_MILLIS,
				DEFAULT_MAX_IDLE_CONNECTIONS);
	}

	/**
	 * Constructor. This client uses an underlying OkHttpClient for API calls, which
	 * parameters can be specified.
	 * 
	 * 
	 * @param resourceName       Resource name for the Azure OpenAI Service to
	 *                           connect to. If null, it will be read from
	 *                           {@link #OS_ENV_VAR_NAME_RESOURCE} system
	 *                           environment variable.
	 * @param apiKey             OpenAiApi key. If this is null, it will try to read
	 *                           it from {@link #OS_ENV_VAR_NAME_API_KEY} system
	 *                           environment variable.
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
	public AzureOpenAiClient(String resourceName, String apiKey, int readTimeout, int maxRetries, int keepAliveDuration,
			int maxIdleConnections) {
		this(resourceName, apiKey,
				ApiClient.getDefaultHttpClient(readTimeout, maxRetries, keepAliveDuration, maxIdleConnections));
	}

	/**
	 * Constructor. This client uses provided OkHttpClient for API calls, to allow
	 * full customization (see
	 * {@link ApiClient#getDefaultHttpClient(int, int, int, int)}).
	 * 
	 * Notice API key header is set in this call, by reading it from OS environment.
	 * 
	 * @param resourceName Resource name for the Azure OpenAI Service to connect to.
	 *                     If null, it will be read from
	 *                     {@link #OS_ENV_VAR_NAME_RESOURCE} system environment
	 *                     variable.
	 */
	public AzureOpenAiClient(String resourceName, OkHttpClient http) {
		this(resourceName, null, http);
	}

	/**
	 * Constructor. This client uses provided OkHttpClient for API calls, to allow
	 * full customization (see
	 * {@link ApiClient#getDefaultHttpClient(int, int, int, int)}).
	 * 
	 * @param apiKey       OpenAI API key to use (will be set in the header).
	 * 
	 * 
	 * @param resourceName Resource name for the Azure OpenAI Service to connect to.
	 *                     If null, it will be read from
	 *                     {@link #OS_ENV_VAR_NAME_RESOURCE} system environment
	 *                     variable.
	 */
	public AzureOpenAiClient(String resourceName, String apiKey, OkHttpClient http) {

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
						.header("Content-Type", "application/json") //
						.header("api-key", (apiKey == null) ? getApiKey() : apiKey) //
						.build());
			}
		}).build();

		client = builder.build();
		azureResourceName = (resourceName == null) ? getResourceName() : resourceName;

		Retrofit retrofit = new Retrofit.Builder().baseUrl("https://" + azureResourceName + ".openai.azure.com/openai/")
				.client(client).addConverterFactory(JacksonConverterFactory.create(jsonMapper))
				.addCallAdapterFactory(RxJava2CallAdapterFactory.create()).build();

		api = retrofit.create(AzureOpenAiApi.class);
	}

	/**
	 * @return The API key from OS environment.
	 */
	public static String getApiKey() {
		String apiKey = System.getenv(OS_ENV_VAR_NAME_API_KEY);
		if (apiKey == null)
			throw new IllegalArgumentException("Azure OpenAI API key is not provided and it cannot be found in "
					+ OS_ENV_VAR_NAME_API_KEY + " system environment variable");
		return apiKey;
	}

	/**
	 * @return The API key from OS environment.
	 */
	public static String getResourceName() {
		String name = System.getenv(OS_ENV_VAR_NAME_RESOURCE);
		if (name == null)
			throw new IllegalArgumentException("Azure OpenAI Service name is not provided and it cannot be found in "
					+ OS_ENV_VAR_NAME_RESOURCE + " system environment variable");
		return name;
	}

	//////// API METHODS MAPPED INTO JAVA CALLS ////////////////////////////////////

	@Override
	public List<Model> listModels() {
		throw new UnsupportedOperationException(
				"This method is not supported by the Azure OpenAI Service client and is available only from  OpenAI API .");

	}

	@Override
	public Model retrieveModel(@NonNull String modelId) {
		throw new UnsupportedOperationException(
				"This method is not supported by the Azure OpenAI Service client and is available only from  OpenAI API .");

	}

	@Override
	public CompletionsResponse createCompletion(@NonNull CompletionsRequest req) {
		return callApi(api.completions(req.getModel(), stableApiVersion, req));
	}

	@Override
	public ChatCompletionsResponse createChatCompletion(@NonNull ChatCompletionsRequest req) {
		return callApi(api.chatCompletions(req.getModel(), stableApiVersion, req));
	}

	@Override
	public List<Image> createImage(@NonNull ImagesRequest req) {
		return callApi(api.imagesGenerations(req.getModel(), previewApiVersion, req)).getData();
	}

	@Override
	protected List<Image> createImageEdit(@NonNull MultipartBody body) {
		throw new UnsupportedOperationException(
				"This method is not supported by the Azure OpenAI Service client and is available only from  OpenAI API .");
	}

	@Override
	protected List<Image> createImageVariation(@NonNull MultipartBody body) {
		throw new UnsupportedOperationException(
				"This method is not supported by the Azure OpenAI Service client and is available only from  OpenAI API .");
	}

	@Override
	public EmbeddingsResponse createEmbeddings(@NonNull EmbeddingsRequest req) {
		return callApi(api.embeddings(req.getModel(), stableApiVersion, req));
	}

	@Override
	protected ResponseBody createSpeechResponse(@NonNull AudioSpeechRequest req) {
		return callApi(api.audioSpeech(req.getModel(), previewApiVersion, req));
	}

	@Override
	protected ResponseBody createTranscription(@NonNull String model, @NonNull MultipartBody body) {
		return callApi(api.audioTranscriptions(model, previewApiVersion, body));
	}

	@Override
	protected AudioResponse createTranslation(@NonNull String model, @NonNull MultipartBody body) {
		return callApi(api.audioTranslations(model, previewApiVersion, body));
	}

	@Override
	public List<File> listFiles() {
		return callApi(api.files(stableApiVersion)).getData();
	}

	@Override
	protected File uploadFile(@NonNull MultipartBody body) {
		return callApi(api.files(stableApiVersion, body));
	}

	@Override
	public DeleteResponse deleteFile(@NonNull String fileId) {
		return callApi(api.filesDelete(fileId, stableApiVersion));
	}

	@Override
	public File retrieveFile(@NonNull String fileId) {
		return callApi(api.files(fileId, stableApiVersion));
	}

	@Override
	protected ResponseBody retrieveFileContentResponse(@NonNull String fileId) {
		return callApi(api.filesContent(fileId, stableApiVersion));
	}

	@Override
	public FineTuningJob createFineTuningJob(@NonNull FineTuningRequest req) {
		throw new UnsupportedOperationException(
				"This method is not supported by the Azure OpenAI Service client and is available only from  OpenAI API .");

	}

	@Override
	public DataList<FineTuningJob> listFineTuningJobs(Integer limit, String after) {
		throw new UnsupportedOperationException(
				"This method is not supported by the Azure OpenAI Service client and is available only from  OpenAI API .");

	}

	@Override
	public DataList<FineTuningJobEvent> listFineTuningEvents(@NonNull String fineTuningJobId, Integer limit,
			String after) {
		throw new UnsupportedOperationException(
				"This method is not supported by the Azure OpenAI Service client and is available only from  OpenAI API .");

	}

	@Override
	public FineTuningJob retrieveFineTuningJob(@NonNull String fineTuningJobId) {
		throw new UnsupportedOperationException(
				"This method is not supported by the Azure OpenAI Service client and is available only from  OpenAI API .");

	}

	@Override
	public FineTuningJob cancelFineTuning(@NonNull String fineTuningJobId) {
		throw new UnsupportedOperationException(
				"This method is not supported by the Azure OpenAI Service client and is available only from  OpenAI API .");

	}

	@Override
	public DeleteResponse deleteFineTunedModel(@NonNull String model) {
		throw new UnsupportedOperationException(
				"This method is not supported by the Azure OpenAI Service client and is available only from  OpenAI API .");

	}

	@Override
	public ModerationsResponse createModeration(@NonNull ModerationsRequest req) {
		throw new UnsupportedOperationException(
				"This method is not supported by the Azure OpenAI Service client and is available only from  OpenAI API .");

	}

	@Override
	public Assistant createAssistant(@NonNull AssistantsRequest req) {
		return callApi(api.assistantsCreate(previewApiVersion, req));
	}

	@Override
	protected File createAssistantFile(@NonNull String assistantId, @NonNull Map<String, String> body) {
		return callApi(api.assistantsFiles(assistantId, previewApiVersion, body));
	}

	@Override
	public DataList<Assistant> listAssistants() {
		return callApi(api.assistants(previewApiVersion, null, null, null, null));
	}

	@Override
	public DataList<Assistant> listAssistants(SortOrder order, Integer limit, String before, String after) {
		return callApi(
				api.assistants(previewApiVersion, limit, (order == null) ? null : order.toString(), after, before));
	}

	@Override
	public DataList<File> listAssistantFiles(@NonNull String assistantId, SortOrder order, Integer limit, String before,
			String after) {
		return callApi(api.assistantsFiles(assistantId, previewApiVersion, limit,
				(order == null) ? null : order.toString(), after, before));
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
	@Override
	public Assistant retrieveAssistant(@NonNull String assistantId) {
		return callApi(api.assistantsGet(assistantId, previewApiVersion));
	}

	@Override
	public File retrieveAssistantFile(@NonNull String assistantId, @NonNull String fileId) {
		return callApi(api.assistantsFilesGet(assistantId, fileId, previewApiVersion));
	}

	@Override
	public Assistant modifyAssistant(@NonNull String assistantId, @NonNull AssistantsRequest req) {
		return callApi(api.assistantsModify(assistantId, previewApiVersion, req));
	}

	@Override
	public DeleteResponse deleteAssistant(@NonNull String assistantId) {
		return callApi(api.assistantsDelete(assistantId, previewApiVersion));
	}

	@Override
	public DeleteResponse deteAssistantFile(@NonNull String assistantId, @NonNull String fileId) {
		return callApi(api.assistantsFilesDelete(assistantId, fileId, previewApiVersion));
	}

	@Override
	public OpenAiThread createThread(@NonNull ThreadsRequest req) {
		return callApi(api.threads(previewApiVersion, req));
	}

	@Override
	public OpenAiThread retrieveThread(@NonNull String threadId) {
		return callApi(api.threadsGet(threadId, previewApiVersion));
	}

	@Override
	public OpenAiThread modifyThread(@NonNull String threadId, @NonNull Metadata metadata) {
		return callApi(api.threadsModify(threadId, previewApiVersion, metadata));
	}

	@Override
	public DeleteResponse deleteThread(@NonNull String threadId) {
		return callApi(api.threadsDelete(threadId, previewApiVersion));
	}

	@Override
	public Message createMessage(@NonNull String threadId, @NonNull MessagesRequest req) {
		return callApi(api.threadsMessagesCreate(threadId, previewApiVersion, req));
	}

	@Override
	public DataList<Message> listMessages(@NonNull String threadId, SortOrder order, Integer limit, String after,
			String before) {
		return callApi(api.threadsMessages(threadId, previewApiVersion, limit,
				(order == null) ? null : order.toString(), after, before));
	}

	@Override
	public DataList<MessageFile> listMessageFiles(@NonNull String threadId, @NonNull String messageId, SortOrder order,
			Integer limit, String after, String before) {
		return callApi(api.threadsMessagesFiles(threadId, messageId, previewApiVersion, limit,
				(order == null) ? null : order.toString(), after, before));
	}

	@Override
	public Message retrieveMessage(@NonNull String threadId, @NonNull String messageId) {
		return callApi(api.threadsMessagesGet(threadId, messageId, previewApiVersion));
	}

	@Override
	public MessageFile retrieveMessageFile(@NonNull String threadId, @NonNull String messageId,
			@NonNull String fileId) {
		return callApi(api.threadsMessagesFiles(threadId, messageId, fileId, previewApiVersion));
	}

	@Override
	public Message modifyMessage(@NonNull String threadId, @NonNull String messageId, @NonNull Metadata metadata) {
		return callApi(api.threadsMessagesModify(threadId, messageId, previewApiVersion, metadata));
	}

	@Override
	public Run createRun(@NonNull String threadId, @NonNull RunsRequest req) {
		return callApi(api.threadsRunsCreate(threadId, previewApiVersion, req));
	}

	@Override
	public Run createThreadAndRun(@NonNull ThreadAndRunRequest req) {
		return callApi(api.threadsRunsCreate(previewApiVersion, req));
	}

	@Override
	public DataList<Run> listRuns(@NonNull String threadId, SortOrder order, Integer limit, String after,
			String before) {
		return callApi(api.threadsRuns(threadId, previewApiVersion, limit, order.toString(), after, before));
	}

	@Override
	public DataList<RunStep> listRunSteps(@NonNull String threadId, @NonNull String runId, Integer limit,
			SortOrder order, String after, String before) {
		return callApi(api.threadsRunsSteps(threadId, runId, previewApiVersion, limit,
				(order == null) ? null : order.toString(), after, before));
	}

	@Override
	public Run retrieveRun(@NonNull String threadId, @NonNull String runId) {
		return callApi(api.threadsRunsGet(threadId, runId, previewApiVersion));
	}

	@Override
	public RunStep retrieveRunStep(@NonNull String threadId, @NonNull String runId, @NonNull String stepId) {
		return callApi(api.threadsRunsStepsGet(threadId, runId, stepId, previewApiVersion));
	}

	@Override
	public Run modifyRun(@NonNull String threadId, @NonNull String runId, @NonNull Metadata metadata) {
		return callApi(api.threadsRunsModify(previewApiVersion, threadId, runId, metadata));
	}

	@Override
	public Run submitToolOutputsToRun(@NonNull String threadId, @NonNull String runId,
			@NonNull ToolOutputsRequest req) {
		return callApi(api.threadsRunsSubmitToolOutputs(threadId, runId, previewApiVersion, req));
	}

	@Override
	public Run cancelRun(@NonNull String threadId, @NonNull String runId) {
		return callApi(api.threadsRunsCancel(threadId, runId, previewApiVersion));
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

	@Override
	public void close() {
		try {
			super.close();
			client.dispatcher().executorService().shutdown();
			client.connectionPool().evictAll();
			if (client.cache() != null)
				client.cache().close();
		} catch (Exception e) {
			LOG.warn("Error while closing client", e);
		}
	}
}