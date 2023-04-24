/**
 * 
 */
package io.github.mzattera.predictivepowers;

import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.embeddings.EmbeddingsRequest;
import io.github.mzattera.predictivepowers.service.AnswerService;
import io.github.mzattera.predictivepowers.service.ChatService;
import io.github.mzattera.predictivepowers.service.CompletionService;
import io.github.mzattera.predictivepowers.service.EmbeddingService;
import io.github.mzattera.predictivepowers.service.QuestionService;
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

	public CompletionService getCompletionService() {
		return getCompletionService(new ChatCompletionsRequest());
	}

	public CompletionService getCompletionService(ChatCompletionsRequest defaultReq) {
		return new CompletionService(this, defaultReq);
	}

	public ChatService getChatService() {
		return getChatService(new ChatCompletionsRequest());
	}

	public ChatService getChatService(String personality) {
		return getChatService(new ChatCompletionsRequest(), personality);
	}

	public ChatService getChatService(ChatCompletionsRequest defaultReq) {
		return new ChatService(this, defaultReq);
	}

	public ChatService getChatService(ChatCompletionsRequest defaultReq, String personality) {
		ChatService s = getChatService(defaultReq);
		s.setPersonality(personality);
		return s;
	}

	public QuestionService getQuestionService() {
		return getQuestionService(getCompletionService());
	}

	public QuestionService getQuestionService(@NonNull CompletionService cs) {
		return new QuestionService(this, cs);
	}

	public AnswerService getAnswerService() {
		return getAnswerService(getCompletionService());
	}

	public AnswerService getAnswerService(@NonNull CompletionService cs) {
		return new AnswerService(this, cs);
	}

	public EmbeddingService getEmbeddingService() {
		return getEmbeddingService(new EmbeddingsRequest());
	}

	public EmbeddingService getEmbeddingService(@NonNull EmbeddingsRequest defaultReq) {
		return new EmbeddingService(this, defaultReq);
	}
}
