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
public interface OpenAiApi {

	@GET("models")
	Single<DataList<Model>> models();

	@GET("models/{model}")
	Single<Model> models(@Path("model") String modelId);

	@DELETE("models/{model}")
	Single<DeleteResponse> modelsDelete(@Path("model") @NonNull String model);

	@POST("completions")
	Single<CompletionsResponse> completions(@Body CompletionsRequest req);

	@POST("chat/completions")
	Single<ChatCompletionsResponse> chatCompletions(@Body ChatCompletionsRequest req);

	@POST("images/generations")
	Single<DataList<Image>> imagesGenerations(@Body ImagesRequest req);

	@POST("images/edits")
	Single<DataList<Image>> imagesEdits(@Body RequestBody req);

	@POST("images/variations")
	Single<DataList<Image>> imagesVariations(@Body RequestBody req);

	@POST("embeddings")
	Single<EmbeddingsResponse> embeddings(@Body EmbeddingsRequest req);

	@POST("audio/speech")
	Single<ResponseBody> audioSpeech(@Body AudioSpeechRequest req);

	@POST("audio/transcriptions")
	Single<AudioResponse> audioTranscriptions(@Body RequestBody req);

	@POST("audio/translations")
	Single<AudioResponse> audioTranslations(@Body RequestBody req);

	@GET("files")
	Single<DataList<File>> files();

	@POST("files")
	Single<File> files(@Body RequestBody file);

	@DELETE("files/{file_id}")
	Single<DeleteResponse> filesDelete(@Path("file_id") String fileId);

	@GET("files/{file_id}")
	Single<File> files(@Path("file_id") String fileId);

	@GET("files/{file_id}/content")
	Single<ResponseBody> filesContent(@Path("file_id") String fileId);

	@POST("fine_tuning/jobs")
	Single<FineTuningJob> fineTuningJobsCreate(@Body FineTuningRequest req);

	@GET("fine_tuning/jobs")
	Single<DataList<FineTuningJob>> fineTuningJobs(@Query("after") String after, @Query("limit") Integer limit);

	@GET("fine_tuning/jobs/{fine_tuning_job_id}/events")
	Single<DataList<FineTuningJobEvent>> fineTuningJobsEvents(
			@Path("fine_tuning_job_id") @NonNull String fineTuningJobId, @Query("after") String after,
			@Query("limit") Integer limit);

	@GET("fine_tuning/jobs/{fine_tuning_job_id}")
	Single<FineTuningJob> fineTuningJobsGet(@Path("fine_tuning_job_id") @NonNull String fineTuningJobId);

	@POST("fine_tuning/jobs/{fine_tuning_job_id}/cancel")
	Single<FineTuningJob> fineTuningJobsCancel(@Path("fine_tuning_job_id") @NonNull String fineTuningJobId);

	@POST("moderations")
	Single<ModerationsResponse> moderations(@Body ModerationsRequest req);
}
