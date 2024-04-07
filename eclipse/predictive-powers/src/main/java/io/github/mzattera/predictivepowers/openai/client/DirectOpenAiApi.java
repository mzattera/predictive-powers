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
 * Retrofit definition for OpenAI API.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public interface DirectOpenAiApi {

	@GET("models")
	Single<DataList<Model>> models();

	@GET("models/{model}")
	Single<Model> models(@Path("model") @NonNull String modelId);

	@DELETE("models/{model}")
	Single<DeleteResponse> modelsDelete(@Path("model") @NonNull String model);

	@POST("completions")
	Single<CompletionsResponse> completions(@Body @NonNull CompletionsRequest req);

	@POST("chat/completions")
	Single<ChatCompletionsResponse> chatCompletions(@Body @NonNull ChatCompletionsRequest req);

	@POST("images/generations")
	Single<DataList<Image>> imagesGenerations(@Body @NonNull ImagesRequest req);

	@POST("images/edits")
	Single<DataList<Image>> imagesEdits(@Body @NonNull RequestBody req);

	@POST("images/variations")
	Single<DataList<Image>> imagesVariations(@Body @NonNull RequestBody req);

	@POST("embeddings")
	Single<EmbeddingsResponse> embeddings(@Body @NonNull EmbeddingsRequest req);

	@POST("audio/speech")
	Single<ResponseBody> audioSpeech(@Body @NonNull AudioSpeechRequest req);

	@POST("audio/transcriptions")
	Single<ResponseBody> audioTranscriptions(@Body @NonNull RequestBody req);

	@POST("audio/translations")
	Single<AudioResponse> audioTranslations(@Body @NonNull RequestBody req);

	@GET("files")
	Single<DataList<File>> files();

	@POST("files")
	Single<File> files(@Body @NonNull RequestBody file);

	@DELETE("files/{file_id}")
	Single<DeleteResponse> filesDelete(@Path("file_id") @NonNull String fileId);

	@GET("files/{file_id}")
	Single<File> files(@Path("file_id") @NonNull String fileId);

	@GET("files/{file_id}/content")
	Single<ResponseBody> filesContent(@Path("file_id") @NonNull String fileId);

	@POST("fine_tuning/jobs")
	Single<FineTuningJob> fineTuningJobsCreate(@Body @NonNull FineTuningRequest req);

	@GET("fine_tuning/jobs")
	Single<DataList<FineTuningJob>> fineTuningJobs(@Query("limit") Integer limit, @Query("after") String after);

	@GET("fine_tuning/jobs/{fine_tuning_job_id}/events")
	Single<DataList<FineTuningJobEvent>> fineTuningJobsEvents(
			@Path("fine_tuning_job_id") @NonNull String fineTuningJobId, @Query("limit") Integer limit,
			@Query("after") String after);

	@GET("fine_tuning/jobs/{fine_tuning_job_id}")
	Single<FineTuningJob> fineTuningJobsGet(@Path("fine_tuning_job_id") @NonNull String fineTuningJobId);

	@POST("fine_tuning/jobs/{fine_tuning_job_id}/cancel")
	Single<FineTuningJob> fineTuningJobsCancel(@Path("fine_tuning_job_id") @NonNull String fineTuningJobId);

	@POST("moderations")
	Single<ModerationsResponse> moderations(@Body @NonNull ModerationsRequest req);

	@POST("assistants")
	Single<Assistant> assistantsCreate(@Body @NonNull AssistantsRequest req);

	@POST("assistants/{assistant_id}/files")
	Single<File> assistantsFiles(@Path("assistant_id") @NonNull String assistantId,
			@Body @NonNull Map<String, String> body);

	@GET("assistants")
	Single<DataList<Assistant>> assistants(@Query("limit") Integer limit, @Query("order") String order,
			@Query("after") String after, @Query("before") String before);

	@GET("assistants/{assistant_id}/files")
	Single<DataList<File>> assistantsFiles(@Path("assistant_id") @NonNull String assistantId,
			@Query("limit") Integer limit, @Query("order") String order, @Query("after") String after,
			@Query("before") String before);

	@GET("assistants/{assistant_id}")
	Single<Assistant> assistantsGet(@Path("assistant_id") @NonNull String assistantId);

	@GET("assistants/{assistant_id}/files/{file_id}")
	Single<File> assistantsFilesGet(@Path("assistant_id") @NonNull String assistantId,
			@Path("file_id") @NonNull String fileId);

	@POST("assistants/{assistant_id}")
	Single<Assistant> assistantsModify(@Path("assistant_id") @NonNull String assistantId,
			@Body @NonNull AssistantsRequest req);

	@DELETE("assistants/{assistant_id}")
	Single<DeleteResponse> assistantsDelete(@Path("assistant_id") @NonNull String assistantId);

	@DELETE("assistants/{assistant_id}/files/{file_id}")
	Single<DeleteResponse> assistantsFilesDelete(@Path("assistant_id") @NonNull String assistantId,
			@Path("file_id") @NonNull String fileId);

	@POST("threads")
	Single<OpenAiThread> threads(@Body @NonNull ThreadsRequest req);

	@GET("threads/{thread_id}")
	Single<OpenAiThread> threadsGet(@Path("thread_id") @NonNull String threadId);

	@POST("threads/{thread_id}")
	Single<OpenAiThread> threadsModify(@Path("thread_id") @NonNull String threadId, @Body @NonNull Metadata metadata);

	@DELETE("threads/{thread_id}")
	Single<DeleteResponse> threadsDelete(@Path("thread_id") @NonNull String threadId);

	@POST("threads/{thread_id}/messages")
	Single<Message> threadsMessagesCreate(@Path("thread_id") @NonNull String threadId,
			@Body @NonNull MessagesRequest req);

	@GET("threads/{thread_id}/messages")
	Single<DataList<Message>> threadsMessages(@Path("thread_id") @NonNull String threadId,
			@Query("limit") Integer limit, @Query("order") String order, @Query("after") String after,
			@Query("before") String before);

	@GET("threads/{thread_id}/messages/{message_id}/files")
	Single<DataList<MessageFile>> threadsMessagesFiles(@Path("thread_id") @NonNull String threadId,
			@Path("message_id") @NonNull String messageId, @Query("limit") Integer limit, @Query("order") String order,
			@Query("after") String after, @Query("before") String before);

	@GET("threads/{thread_id}/messages/{message_id}")
	Single<Message> threadsMessagesGet(@Path("thread_id") @NonNull String threadId,
			@Path("message_id") @NonNull String messageId);

	@GET("threads/{thread_id}/messages/{message_id}/files/{file_id}")
	Single<MessageFile> threadsMessagesFiles(@Path("thread_id") @NonNull String threadId,
			@Path("message_id") @NonNull String messageId, @Path("file_id") @NonNull String fileId);

	@POST("threads/{thread_id}/messages/{message_id}")
	Single<Message> threadsMessagesModify(@Path("thread_id") @NonNull String threadId,
			@Path("message_id") @NonNull String messageId, @Body @NonNull Metadata metadata);

	@POST("threads/{thread_id}/runs")
	Single<Run> threadsRunsCreate(@Path("thread_id") @NonNull String threadId, @Body @NonNull RunsRequest req);

	@POST("threads/runs")
	Single<Run> threadsRunsCreate(@Body @NonNull ThreadAndRunRequest req);

	@GET("threads/{thread_id}/runs")
	Single<DataList<Run>> threadsRuns(@Path("thread_id") @NonNull String threadId, @Query("limit") Integer limit,
			@Query("order") String order, @Query("after") String after, @Query("before") String before);

	@GET("threads/{thread_id}/runs/{run_id}/steps")
	Single<DataList<RunStep>> threadsRunsSteps(@Path("thread_id") @NonNull String threadId,
			@Path("run_id") @NonNull String runId, @Query("limit") Integer limit, @Query("order") String order,
			@Query("after") String after, @Query("before") String before);

	@GET("threads/{thread_id}/runs/{run_id}")
	Single<Run> threadsRunsGet(@Path("thread_id") @NonNull String threadId, @Path("run_id") @NonNull String runId);

	@GET("threads/{thread_id}/runs/{run_id}/steps/{step_id}")
	Single<RunStep> threadsRunsStepsGet(@Path("thread_id") @NonNull String threadId,
			@Path("run_id") @NonNull String runId, @Path("step_id") @NonNull String stepId);

	@GET("threads/{thread_id}/runs/{run_id}")
	Single<Run> threadsRunsModify(@Path("thread_id") @NonNull String threadId, @Path("run_id") @NonNull String runId,
			@Body @NonNull Metadata metadata);

	@POST("threads/{thread_id}/runs/{run_id}/submit_tool_outputs")
	Single<Run> threadsRunsSubmitToolOutputs(@Path("thread_id") @NonNull String threadId,
			@Path("run_id") @NonNull String runId, @Body @NonNull ToolOutputsRequest req);

	@POST("threads/{thread_id}/runs/{run_id}/cancel")
	Single<Run> threadsRunsCancel(@Path("thread_id") @NonNull String threadId, @Path("run_id") @NonNull String runId);
}