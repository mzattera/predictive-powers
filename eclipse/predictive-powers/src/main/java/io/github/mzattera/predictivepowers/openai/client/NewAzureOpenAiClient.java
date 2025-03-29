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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatRequestAssistantMessage;
import com.azure.ai.openai.models.ChatRequestFunctionMessage;
import com.azure.ai.openai.models.ChatRequestMessage;
import com.azure.ai.openai.models.ChatRequestSystemMessage;
import com.azure.ai.openai.models.ChatRequestToolMessage;
import com.azure.ai.openai.models.ChatRequestUserMessage;
import com.azure.ai.openai.models.Embeddings;
import com.azure.ai.openai.models.EmbeddingsOptions;
import com.azure.core.credential.AccessToken;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.http.HttpHeaderName;
import com.azure.core.http.rest.RequestOptions;
import com.azure.json.JsonProviders;
import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;

import io.github.mzattera.predictivepowers.openai.client.assistants.Assistant;
import io.github.mzattera.predictivepowers.openai.client.assistants.AssistantsRequest;
import io.github.mzattera.predictivepowers.openai.client.audio.AudioResponse;
import io.github.mzattera.predictivepowers.openai.client.audio.AudioSpeechRequest;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsResponse;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsResponse;
import io.github.mzattera.predictivepowers.openai.client.embeddings.Embedding;
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
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatMessage;
import lombok.NonNull;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody;

/**
 * New approach to OpenAI Azure Cognitive Service access.
 * 
 * @author Massimiliano "Maxi" Zattera
 */
public class NewAzureOpenAiClient extends OpenAiClient {

	private final static Logger LOG = LoggerFactory.getLogger(AzureOpenAiClient.class);

	/**
	 * Name of the OS environment variable containing the API key.
	 */
	public static final String OS_ENV_VAR_NAME_API_KEY = "AZURE_OPENAI_API_KEY";

	/**
	 * Name of the OS environment variable containing Azure OpenAI endpoint.
	 */
	public static final String OS_ENV_VAR_NAME_RESOURCE = "AZURE_OPENAI_RESOURCE";

	public final static int DEFAULT_TIMEOUT_MILLIS = 3 * 60 * 1000;
	public final static int DEFAULT_MAX_RETRIES = 10;
	public final static int DEFAULT_KEEP_ALIVE_MILLIS = 5 * 60 * 1000;
	public final static int DEFAULT_MAX_IDLE_CONNECTIONS = 5;

	private OpenAIClient cli;
	private AccessToken token;

	/**
	 * Constructor, using default parameters for OkHttpClient.
	 *
	 * OpenAI API key is read from {@link #OS_ENV_VAR_NAME_API_KEY} system
	 * environment variable.
	 *
	 * Azure OpenAI Service resource name is read from
	 * {@link #OS_ENV_VAR_NAME_RESOURCE} system environment variable.
	 */
	public NewAzureOpenAiClient(OpenAIClient cli, AccessToken token) {
		this.cli = cli;
		this.token = token;
	}

	/**
	 * Constructor. This client uses an underlying OkHttpClient for API calls, which
	 * parameters can be specified.
	 *
	 * @param endPoint Endpoint for the Azure OpenAI Service to connect to. If null,
	 *                 it will be read from {@link #OS_ENV_VAR_NAME_RESOURCE} system
	 *                 environment variable.
	 * @param apiKey   OpenAI API key. If this is null, it will try to read it from
	 *                 {@link #OS_ENV_VAR_NAME_API_KEY} system environment variable.
	 */
	public NewAzureOpenAiClient(String endPoint, String apiKey, AccessToken token) {
		this(new OpenAIClientBuilder() //
				.credential(new AzureKeyCredential((apiKey == null) ? getApiKey() : apiKey)) //
				.endpoint((endPoint == null) ? getResourceName() : endPoint) //
				.buildClient(), token);
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

	// TODO : Check the naming here

	/**
	 * @return Azure Resource Name from OS environment.
	 */
	public static String getResourceName() {
		String name = System.getenv(OS_ENV_VAR_NAME_RESOURCE);
		if (name == null)
			throw new IllegalArgumentException("Azure OpenAI Service name is not provided and it cannot be found in "
					+ OS_ENV_VAR_NAME_RESOURCE + " system environment variable");
		return name;
	}

	/**
	 * Converts a POJO to JSON then the JSON back to an instance of given class.
	 */
	private static <T> T fromJson(JsonSerializable<?> sdkPojo, Class<T> cls) {
		try {
			Writer w = new StringWriter();
			sdkPojo.toJson(JsonProviders.createWriter(w)).flush();
			return jsonMapper.readValue(w.toString(), cls);
		} catch (IOException e) {
			// Should never happen
			LOG.error(e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Gets a JsonReader ready to read to turn a POJO to JSON.
	 */
	private static JsonReader toJsonReader(Object pojo) {
		try {
			return JsonProviders.createReader(jsonMapper.writeValueAsString(pojo));
		} catch (IOException e) {
			// Should never happen
			LOG.error(e.getMessage(), e);
			return null;
		}
	}

	@Override
	public ChatCompletionsResponse createChatCompletion(@NonNull ChatCompletionsRequest req) {
		try {
			ChatCompletionsOptions ops = ChatCompletionsOptions.fromJson(toJsonReader(req));

			// Workaround as JSON deserialization does not work properly ATM
			List<ChatRequestMessage> msgs = ops.getMessages();
			msgs.clear();
			for (OpenAiChatMessage msg : req.getMessages()) {
				switch (msg.getRole()) {
				case SYSTEM:
					msgs.add(new ChatRequestSystemMessage(msg.getContent()));
					break;
				case USER:
					msgs.add(new ChatRequestUserMessage(msg.getContent()));
					break;
				case ASSISTANT:
					msgs.add(new ChatRequestAssistantMessage(msg.getContent()));
					break;
				case TOOL:
					msgs.add(new ChatRequestToolMessage(msg.getContent(), msg.getToolCallId()));
					break;
				case FUNCTION:
					msgs.add(new ChatRequestFunctionMessage(msg.getName(), msg.getContent()));
					break;
				default:
					throw new IllegalArgumentException();
				}
			}

			RequestOptions reqOpt = new RequestOptions();
			if (token != null) {
				reqOpt.addHeader(HttpHeaderName.AUTHORIZATION, token.getToken());
			}

			ChatCompletions resp = cli.getChatCompletionsWithResponse(req.getModel(), ops, reqOpt).getValue();
			ChatCompletionsResponse result = fromJson(resp, ChatCompletionsResponse.class);
			return result;
		} catch (IOException e) {
			// Should never happen
			throw new RuntimeException(e);
		}
	}

	@Override
	public EmbeddingsResponse createEmbeddings(@NonNull EmbeddingsRequest req) {
		EmbeddingsOptions ops = new EmbeddingsOptions(req.getInput());
		ops.setModel(req.getModel());

		RequestOptions reqOpt = new RequestOptions();
		if (token != null) {
			reqOpt.addHeader(HttpHeaderName.AUTHORIZATION, token.getToken());
		}

		Embeddings resp = cli.getEmbeddingsWithResponse(req.getModel(), ops, reqOpt).getValue();
		EmbeddingsResponse result = EmbeddingsResponse.builder() //
				.model(req.getModel()) //
				.object("x") // TODO fix
				.usage(fromJson(resp.getUsage(), Usage.class)) //
				.build();

		List<Embedding> e = resp.getData().stream() //
				.map(d -> new Embedding("obj", // TODO Fix
						d.getEmbedding().stream().map(f -> (double) f).collect(Collectors.toList()), //
						d.getPromptIndex())) //
				.collect(Collectors.toList());

		result.setData(e);
		return result;
	}

	@Override
	public List<Model> listModels() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model retrieveModel(@NonNull String modelId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletionsResponse createCompletion(@NonNull CompletionsRequest req) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Image> createImage(@NonNull ImagesRequest req) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected List<Image> createImageEdit(@NonNull MultipartBody body) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected List<Image> createImageVariation(@NonNull MultipartBody body) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ResponseBody createSpeechResponse(@NonNull AudioSpeechRequest req) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ResponseBody createTranscription(@NonNull String model, @NonNull MultipartBody body) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected AudioResponse createTranslation(@NonNull String model, @NonNull MultipartBody body) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<File> listFiles() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected File uploadFile(@NonNull MultipartBody body) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DeleteResponse deleteFile(@NonNull String fileId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File retrieveFile(@NonNull String fileId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ResponseBody retrieveFileContentResponse(@NonNull String fileId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FineTuningJob createFineTuningJob(@NonNull FineTuningRequest req) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataList<FineTuningJob> listFineTuningJobs(Integer limit, String after) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataList<FineTuningJobEvent> listFineTuningEvents(@NonNull String fineTuningJobId, Integer limit,
			String after) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FineTuningJob retrieveFineTuningJob(@NonNull String fineTuningJobId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FineTuningJob cancelFineTuning(@NonNull String fineTuningJobId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DeleteResponse deleteFineTunedModel(@NonNull String model) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ModerationsResponse createModeration(@NonNull ModerationsRequest req) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Assistant createAssistant(@NonNull AssistantsRequest req) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected File createAssistantFile(@NonNull String assistantId, @NonNull Map<String, String> body) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataList<Assistant> listAssistants(SortOrder order, Integer limit, String before, String after) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataList<File> listAssistantFiles(@NonNull String assistantId, SortOrder order, Integer limit, String before,
			String after) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Assistant retrieveAssistant(@NonNull String assistantId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File retrieveAssistantFile(@NonNull String assistantId, @NonNull String fileId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Assistant modifyAssistant(@NonNull String assistantId, @NonNull AssistantsRequest req) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DeleteResponse deleteAssistant(@NonNull String assistantId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DeleteResponse deteAssistantFile(@NonNull String assistantId, @NonNull String fileId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OpenAiThread createThread(@NonNull ThreadsRequest req) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OpenAiThread retrieveThread(@NonNull String threadId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OpenAiThread modifyThread(@NonNull String threadId, @NonNull Metadata metadata) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DeleteResponse deleteThread(@NonNull String threadId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Message createMessage(@NonNull String threadId, @NonNull MessagesRequest req) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataList<Message> listMessages(@NonNull String threadId, SortOrder order, Integer limit, String after,
			String before) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataList<MessageFile> listMessageFiles(@NonNull String threadId, @NonNull String messageId, SortOrder order,
			Integer limit, String after, String before) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Message retrieveMessage(@NonNull String threadId, @NonNull String messageId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MessageFile retrieveMessageFile(@NonNull String threadId, @NonNull String messageId,
			@NonNull String fileId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Message modifyMessage(@NonNull String threadId, @NonNull String messageId, @NonNull Metadata metadata) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Run createRun(@NonNull String threadId, @NonNull RunsRequest req) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Run createThreadAndRun(@NonNull ThreadAndRunRequest req) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataList<Run> listRuns(@NonNull String threadId, @NonNull SortOrder order, Integer limit, String after,
			String before) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataList<RunStep> listRunSteps(@NonNull String threadId, @NonNull String runId, Integer limit,
			SortOrder order, String after, String before) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Run retrieveRun(@NonNull String threadId, @NonNull String runId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RunStep retrieveRunStep(@NonNull String threadId, @NonNull String runId, @NonNull String stepId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Run modifyRun(@NonNull String threadId, @NonNull String runId, @NonNull Metadata metadata) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Run submitToolOutputsToRun(@NonNull String threadId, @NonNull String runId,
			@NonNull ToolOutputsRequest req) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Run cancelRun(@NonNull String threadId, @NonNull String runId) {
		// TODO Auto-generated method stub
		return null;
	}

	/////////////////////////////////////////////////////////////////////////////////

	@Override
	public void close() {
		try {
		} catch (Exception e) {
			LOG.warn("Error while closing client", e);
		}
	}

}
