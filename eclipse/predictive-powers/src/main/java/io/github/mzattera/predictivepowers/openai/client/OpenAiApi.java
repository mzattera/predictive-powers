/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client;

import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsResponse;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsResponse;
import io.github.mzattera.predictivepowers.openai.client.edits.EditsRequest;
import io.github.mzattera.predictivepowers.openai.client.edits.EditsResponse;
import io.github.mzattera.predictivepowers.openai.client.embeddings.EmbeddingsRequest;
import io.github.mzattera.predictivepowers.openai.client.embeddings.EmbeddingsResponse;
import io.github.mzattera.predictivepowers.openai.client.images.ImagesGenerationsRequest;
import io.github.mzattera.predictivepowers.openai.client.images.ImagesResponse;
import io.github.mzattera.predictivepowers.openai.client.models.Model;
import io.github.mzattera.predictivepowers.openai.client.models.ModelsResponse;
import io.reactivex.Single;
import lombok.NonNull;
import okhttp3.RequestBody;
import retrofit2.http.Body;
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
	Single<Model> models(@Path("model") @NonNull String modelId);

	@POST("completions")
	Single<CompletionsResponse> completions(@Body CompletionsRequest req);

	@POST("chat/completions")
	Single<ChatCompletionsResponse> chatCompletions(@Body ChatCompletionsRequest req);

	@POST("edits")
	Single<EditsResponse> edits(@Body EditsRequest req);

	@POST("images/generations")
	Single<ImagesResponse> imagesGenerations(@Body ImagesGenerationsRequest req);

	@POST("images/edits")
	Single<ImagesResponse> imagesEdits(@Body RequestBody req);

	@POST("images/variations")
	Single<ImagesResponse> imagesVariations(@Body RequestBody req);

	@POST("embeddings")
	Single<EmbeddingsResponse> embeddings(@Body EmbeddingsRequest req);
}
