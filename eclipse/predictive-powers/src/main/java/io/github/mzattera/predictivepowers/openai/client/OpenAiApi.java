/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client;

import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsResponse;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsResponse;
import io.github.mzattera.predictivepowers.openai.client.embeddings.EmbeddingsRequest;
import io.github.mzattera.predictivepowers.openai.client.embeddings.EmbeddingsResponse;
import io.github.mzattera.predictivepowers.openai.client.models.Model;
import io.github.mzattera.predictivepowers.openai.client.models.ModelsResponse;
import io.reactivex.Single;
import lombok.NonNull;
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
	Single<CompletionsResponse> completions(@Body CompletionsRequest request);

	
	@POST("chat/completions")
	Single<ChatCompletionsResponse> chatCompletions(@Body ChatCompletionsRequest request);

    
    @POST("/embeddings")
	Single<EmbeddingsResponse> embeddings(@Body EmbeddingsRequest request);
}
