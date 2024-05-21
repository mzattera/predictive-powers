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

package io.github.mzattera.predictivepowers.openai.client;

import java.util.Map;

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
import io.github.mzattera.predictivepowers.openai.client.images.Image;
import io.github.mzattera.predictivepowers.openai.client.images.ImagesRequest;
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
import lombok.NonNull;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit definition for Mirosoft Azure OpenAI Service API.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public interface AzureOpenAiApi {

	@POST("deployments/{deployment_id}/completions")
	Single<CompletionsResponse> completions(@Path("deployment_id") @NonNull String deploymentId, //
			@Query("api-version") @NonNull String apiVersion, //
			@Body @NonNull CompletionsRequest req);

	@POST("deployments/{deployment_id}/chat/completions")
	Single<ChatCompletionsResponse> chatCompletions(@Path("deployment_id") @NonNull String deploymentId, //
			@Query("api-version") @NonNull String apiVersion, //
			@Body @NonNull ChatCompletionsRequest req);

	@POST("deployments/{deployment_id}/embeddings")
	Single<EmbeddingsResponse> embeddings(@Path("deployment_id") @NonNull String deploymentId, //
			@Query("api-version") @NonNull String apiVersion, //
			@Body @NonNull EmbeddingsRequest req);

	@POST("deployments/{deployment_id}/audio/speech")
	Single<ResponseBody> audioSpeech(@Path("deployment_id") @NonNull String deploymentId, //
			@Query("api-version") @NonNull String apiVersion, //
			@Body @NonNull AudioSpeechRequest req);

	@POST("deployments/{deployment_id}/audio/transcriptions")
	Single<ResponseBody> audioTranscriptions(@Path("deployment_id") @NonNull String deploymentId, //
			@Query("api-version") @NonNull String apiVersion, //
			@Body @NonNull RequestBody req);

	@POST("deployments/{deployment_id}/audio/translations")
	Single<AudioResponse> audioTranslations(@Path("deployment_id") @NonNull String deploymentId, //
			@Query("api-version") @NonNull String apiVersion, //
			@Body @NonNull RequestBody req);

	@POST("deployments/{deployment_id}/images/generations")
	Single<DataList<Image>> imagesGenerations(@Path("deployment_id") @NonNull String deploymentId, //
			@Query("api-version") @NonNull String apiVersion, //
			@Body @NonNull ImagesRequest req);

	@GET("files")
	Single<DataList<File>> files(@Query("api-version") @NonNull String apiVersion);

	@POST("files")
	Single<File> files(@Query("api-version") @NonNull String apiVersion, //
			@Body @NonNull RequestBody file);

	@DELETE("files/{file_id}")
	Single<DeleteResponse> filesDelete(@Path("file_id") @NonNull String fileId,
			@Query("api-version") @NonNull String apiVersion);

	@GET("files/{file_id}")
	Single<File> files(@Path("file_id") @NonNull String fileId, @Query("api-version") @NonNull String apiVersion);

	@GET("files/{file_id}/content")
	Single<ResponseBody> filesContent(@Path("file_id") @NonNull String fileId,
			@Query("api-version") @NonNull String apiVersion);

	@POST("assistants")
	Single<Assistant> assistantsCreate(@Query("api-version") @NonNull String apiVersion, //
			@Body @NonNull AssistantsRequest req);

	@POST("assistants/{assistant_id}/files")
	Single<File> assistantsFiles(@Path("assistant_id") @NonNull String assistantId, //
			@Query("api-version") @NonNull String apiVersion, //
			@Body @NonNull Map<String, String> body);

	@GET("assistants")
	Single<DataList<Assistant>> assistants(@Query("api-version") @NonNull String apiVersion, //
			@Query("limit") Integer limit, @Query("order") String order, @Query("after") String after,
			@Query("before") String before);

	@GET("assistants/{assistant_id}/files")
	Single<DataList<File>> assistantsFiles(@Path("assistant_id") @NonNull String assistantId, //
			@Query("api-version") @NonNull String apiVersion, //
			@Query("limit") Integer limit, @Query("order") String order, @Query("after") String after,
			@Query("before") String before);

	@GET("assistants/{assistant_id}")
	Single<Assistant> assistantsGet(@Path("assistant_id") @NonNull String assistantId, //
			@Query("api-version") @NonNull String apiVersion);

	@GET("assistants/{assistant_id}/files/{file_id}")
	Single<File> assistantsFilesGet(@Path("assistant_id") @NonNull String assistantId, //
			@Path("file_id") @NonNull String fileId, @Query("api-version") @NonNull String apiVersion);

	@POST("assistants/{assistant_id}")
	Single<Assistant> assistantsModify(@Path("assistant_id") @NonNull String assistantId, //
			@Query("api-version") @NonNull String apiVersion, //
			@Body @NonNull AssistantsRequest req);

	@DELETE("assistants/{assistant_id}")
	Single<DeleteResponse> assistantsDelete(@Path("assistant_id") @NonNull String assistantId,
			@Query("api-version") @NonNull String apiVersion);

	@DELETE("assistants/{assistant_id}/files/{file_id}")
	Single<DeleteResponse> assistantsFilesDelete(@Path("assistant_id") @NonNull String assistantId,
			@Path("file_id") @NonNull String fileId, //
			@Query("api-version") @NonNull String apiVersion);

	@POST("threads")
	Single<OpenAiThread> threads(@Query("api-version") @NonNull String apiVersion, //
			@Body @NonNull ThreadsRequest req);

	@GET("threads/{thread_id}")
	Single<OpenAiThread> threadsGet(@Path("thread_id") @NonNull String threadId, //
			@Query("api-version") @NonNull String apiVersion);

	@POST("threads/{thread_id}")
	Single<OpenAiThread> threadsModify(@Path("thread_id") @NonNull String threadId,
			@Query("api-version") @NonNull String apiVersion, //
			@Body @NonNull Metadata metadata);

	@DELETE("threads/{thread_id}")
	Single<DeleteResponse> threadsDelete(@Path("thread_id") @NonNull String threadId, //
			@Query("api-version") @NonNull String apiVersion);

	@POST("threads/{thread_id}/messages")
	Single<Message> threadsMessagesCreate(@Path("thread_id") @NonNull String threadId, //
			@Query("api-version") @NonNull String apiVersion, //
			@Body @NonNull MessagesRequest req);

	@GET("threads/{thread_id}/messages")
	Single<DataList<Message>> threadsMessages(@Path("thread_id") @NonNull String threadId, //
			@Query("api-version") @NonNull String apiVersion, //
			@Query("limit") Integer limit, @Query("order") String order, @Query("after") String after,
			@Query("before") String before);

	@GET("threads/{thread_id}/messages/{message_id}/files")
	Single<DataList<MessageFile>> threadsMessagesFiles(@Path("thread_id") @NonNull String threadId,
			@Path("message_id") @NonNull String messageId, @Query("api-version") @NonNull String apiVersion, //
			@Query("limit") Integer limit, @Query("order") String order, @Query("after") String after,
			@Query("before") String before);

	@GET("threads/{thread_id}/messages/{message_id}")
	Single<Message> threadsMessagesGet(@Path("thread_id") @NonNull String threadId,
			@Path("message_id") @NonNull String messageId, @Query("api-version") @NonNull String apiVersion);

	@GET("threads/{thread_id}/messages/{message_id}/files/{file_id}")
	Single<MessageFile> threadsMessagesFiles(@Path("thread_id") @NonNull String threadId,
			@Path("message_id") @NonNull String messageId, @Path("file_id") @NonNull String fileId,
			@Query("api-version") @NonNull String apiVersion);

	@POST("threads/{thread_id}/messages/{message_id}")
	Single<Message> threadsMessagesModify(@Path("thread_id") @NonNull String threadId,
			@Path("message_id") @NonNull String messageId, @Query("api-version") @NonNull String apiVersion, //
			@Body @NonNull Metadata metadata);

	@POST("threads/{thread_id}/runs")
	Single<Run> threadsRunsCreate(@Path("thread_id") @NonNull String threadId, //
			@Query("api-version") @NonNull String apiVersion, //
			@Body @NonNull RunsRequest req);

	@POST("threads/runs")
	Single<Run> threadsRunsCreate(@Query("api-version") @NonNull String apiVersion, //
			@Body @NonNull ThreadAndRunRequest req);

	@GET("threads/{thread_id}/runs")
	Single<DataList<Run>> threadsRuns(@Path("thread_id") @NonNull String threadId,
			@Query("api-version") @NonNull String apiVersion, //
			@Query("limit") Integer limit, @Query("order") String order, @Query("after") String after,
			@Query("before") String before);

	@GET("threads/{thread_id}/runs/{run_id}/steps")
	Single<DataList<RunStep>> threadsRunsSteps(@Path("thread_id") @NonNull String threadId,
			@Path("run_id") @NonNull String runId, @Query("api-version") @NonNull String apiVersion, //
			@Query("limit") Integer limit, @Query("order") String order, @Query("after") String after,
			@Query("before") String before);

	@GET("threads/{thread_id}/runs/{run_id}")
	Single<Run> threadsRunsGet(@Path("thread_id") @NonNull String threadId, @Path("run_id") @NonNull String runId, //
			@Query("api-version") @NonNull String apiVersion);

	@GET("threads/{thread_id}/runs/{run_id}/steps/{step_id}")
	Single<RunStep> threadsRunsStepsGet(@Path("thread_id") @NonNull String threadId,
			@Path("run_id") @NonNull String runId, @Path("step_id") @NonNull String stepId,
			@Query("api-version") @NonNull String apiVersion);

	@GET("threads/{thread_id}/runs/{run_id}")
	Single<Run> threadsRunsModify(@Path("thread_id") @NonNull String threadId, @Path("run_id") @NonNull String runId,
			@Query("api-version") @NonNull String apiVersion, //
			@Body @NonNull Metadata metadata);

	@POST("threads/{thread_id}/runs/{run_id}/submit_tool_outputs")
	Single<Run> threadsRunsSubmitToolOutputs(@Path("thread_id") @NonNull String threadId,
			@Path("run_id") @NonNull String runId, @Query("api-version") @NonNull String apiVersion, //
			@Body @NonNull ToolOutputsRequest req);

	@POST("threads/{thread_id}/runs/{run_id}/cancel")
	Single<Run> threadsRunsCancel(@Path("thread_id") @NonNull String threadId, @Path("run_id") @NonNull String runId,
			@Query("api-version") @NonNull String apiVersion);
}