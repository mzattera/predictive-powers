/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client;

import io.github.mzattera.predictivepowers.openai.client.audio.AudioResponse;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsResponse;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsResponse;
import io.github.mzattera.predictivepowers.openai.client.edits.EditsRequest;
import io.github.mzattera.predictivepowers.openai.client.edits.EditsResponse;
import io.github.mzattera.predictivepowers.openai.client.embeddings.EmbeddingsRequest;
import io.github.mzattera.predictivepowers.openai.client.embeddings.EmbeddingsResponse;
import io.github.mzattera.predictivepowers.openai.client.files.File;
import io.github.mzattera.predictivepowers.openai.client.files.FilesDeleteResponse;
import io.github.mzattera.predictivepowers.openai.client.files.FilesResponse;
import io.github.mzattera.predictivepowers.openai.client.images.ImagesRequest;
import io.github.mzattera.predictivepowers.openai.client.images.ImagesResponse;
import io.github.mzattera.predictivepowers.openai.client.models.Model;
import io.github.mzattera.predictivepowers.openai.client.models.ModelsResponse;
import io.reactivex.Single;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Retrofit definition for OpenAI /models API.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public interface OpenAiApi {

	@GET("models")
	Single<ModelsResponse> models();

	@GET("models/{model}")
	Single<Model> models(@Path("model") String modelId);

	@POST("completions")
	Single<CompletionsResponse> completions(@Body CompletionsRequest req);

	@POST("chat/completions")
	Single<ChatCompletionsResponse> chatCompletions(@Body ChatCompletionsRequest req);

	@POST("edits")
	Single<EditsResponse> edits(@Body EditsRequest req);

	@POST("images/generations")
	Single<ImagesResponse> imagesGenerations(@Body ImagesRequest req);

	@POST("images/edits")
	Single<ImagesResponse> imagesEdits(@Body RequestBody req);

	@POST("images/variations")
	Single<ImagesResponse> imagesVariations(@Body RequestBody req);

	@POST("embeddings")
	Single<EmbeddingsResponse> embeddings(@Body EmbeddingsRequest req);

	@POST("audio/transcriptions")
	Single<AudioResponse> audioTranscriptions(@Body RequestBody req);

	@POST("audio/translations")
	Single<AudioResponse> audioTranslations(@Body RequestBody req);

	@GET("files")
	Single<FilesResponse> files();

	@POST("files")
	Single<File> files(@Body RequestBody file);

	@DELETE("files/{file_id}")
	Single<FilesDeleteResponse> filesDelete(@Path("file_id") String fileId);

	@GET("files/{file_id}")
	Single<File> files(@Path("file_id") String fileId);

	@GET("files/{file_id}/content")
	Single<ResponseBody> filesContent(@Path("file_id") String fileId);
}
