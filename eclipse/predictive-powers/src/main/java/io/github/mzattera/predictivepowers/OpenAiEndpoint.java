/**
 * 
 */
package io.github.mzattera.predictivepowers;

import io.github.mzattera.predictivepowers.client.openai.OpenAiClient;
import io.github.mzattera.predictivepowers.client.openai.completions.CompletionsRequest;
import lombok.Getter;
import lombok.NonNull;

/**
 * This represents an OpenAI end point, from which APIs can be created.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
public class OpenAiEndpoint {

	@Getter
	private final OpenAiClient client;

	private OpenAiEndpoint(@NonNull String apiKey) {
		this(new OpenAiClient(apiKey));
	}

	private OpenAiEndpoint(@NonNull OpenAiClient client) {
		this.client = client;
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
	public static OpenAiEndpoint getInstance(@NonNull String apiKey) {
		return new OpenAiEndpoint(apiKey);
	}

	/**
	 * Create a new end point.
	 * 
	 * @return An end point instance that uses given API client.
	 */
	public static OpenAiEndpoint getInstance(@NonNull OpenAiClient client) {
		return new OpenAiEndpoint(client);
	}

	public InventoryService getInventoryService() {
		return new InventoryService(this);
	}

	public CompletionService getCompletionService() {
		return new CompletionService(this, new CompletionsRequest());
	}

	public CompletionService getCompletionService(CompletionsRequest defaultParams) {
		return new CompletionService(this, defaultParams);
	}
}
