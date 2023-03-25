/**
 * 
 */
package io.github.mzattera.predictivepowers.client;

import io.reactivex.Single;
import retrofit2.http.GET;
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
	Single<Model> models(@Path("model") String modelId);
}
