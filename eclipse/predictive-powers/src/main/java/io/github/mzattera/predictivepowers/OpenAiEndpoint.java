/**
 * 
 */
package io.github.mzattera.predictivepowers;

import java.util.List;

import io.github.mzattera.predictivepowers.client.Model;
import io.github.mzattera.predictivepowers.client.OpenAiClient;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * This represents an OpenAI end point, from which APIs can be created.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
@RequiredArgsConstructor
public class OpenAiEndpoint {

	@NonNull
	@Getter
	private final String apiKey;

	@NonNull
	private final OpenAiClient client;

	private OpenAiEndpoint(@NonNull String apiKey) {
		this.apiKey = apiKey;
		client = new OpenAiClient(apiKey);
	}

	// TODO add configuration of API client (e.g. timeout)

	/**
	 * Create a new end point.
	 * 
	 * @return An end point instance, reading API key from "OPENAI_API_KEY"
	 *         environment parameter.
	 */
	public static OpenAiEndpoint getInstance() {
		return new OpenAiEndpoint(System.getenv("OPENAI_API_KEY"));
	}

	/**
	 * Create a new end point.
	 * 
	 * @return An end point instance that uses given API key.
	 */
	public static OpenAiEndpoint getInstance(String apiKey) {
		return new OpenAiEndpoint(apiKey);
	}

	public List<Model> listModels() {
		return client.models();
	}

	public Model retrieveModel(String modelId) {
		return client.models(modelId);
	}
}
