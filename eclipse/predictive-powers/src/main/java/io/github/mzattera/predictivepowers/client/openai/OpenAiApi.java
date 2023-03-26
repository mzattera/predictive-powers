/**
 * 
 */
package io.github.mzattera.predictivepowers.client.openai;

import io.github.mzattera.predictivepowers.client.openai.chatcompletion.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.client.openai.chatcompletion.ChatCompletionsResponse;
import io.github.mzattera.predictivepowers.client.openai.completions.CompletionsRequest;
import io.github.mzattera.predictivepowers.client.openai.completions.CompletionsResponse;
import io.github.mzattera.predictivepowers.client.openai.models.Model;
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
	Single<OpenAiList<Model>> models();

	@GET("models/{model}")
	Single<Model> models(@Path("model") @NonNull String modelId);

	@POST("completions")
	Single<CompletionsResponse> createCompletion(@Body CompletionsRequest request);

	@POST("chat/completions")
	Single<ChatCompletionsResponse> createChatCompletion(@Body ChatCompletionsRequest request);
}
