package io.github.mzattera.predictivepowers;

import java.util.ArrayList;
import java.util.List;

import io.github.mzattera.predictivepowers.client.openai.chatcompletion.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.client.openai.chatcompletion.ChatCompletionsResponse;
import io.github.mzattera.predictivepowers.client.openai.chatcompletion.ChatMessage;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * A class that exposes text completion services (prompt execution).
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@RequiredArgsConstructor
public class ChatService {

	@NonNull
	private final OpenAiEndpoint ep;

	/** Personality opf the agent. */
	@NonNull
	@Getter
	private String personality = "You are a helpful assistant.";
	
	/**
	 * Set agent personlity.
	 * 
	 * @param personality A string describing the agrnt's personality (e.g. 'You are a helpful assistant.').
	 */
	public void setPersonality(String personality) {
		this.personality=personality;
	}
	
	/**
	 * This request, with its parameters, is used as default setting for each call.
	 * 
	 * You can change any parameter to change these defaults (e.g. the model used)
	 * and the change will apply to all subsequent calls.
	 */
	@Getter
	@NonNull
	private final ChatCompletionsRequest defaultReq;

	public String oneTurn(String prompt) {
		return oneTurn(prompt, defaultReq);
	}

	public String oneTurn(String prompt, ChatCompletionsRequest params) {

		ChatCompletionsRequest req = (ChatCompletionsRequest) defaultReq.clone();

		List<ChatMessage> msg = new ArrayList<>();
		msg.add(new ChatMessage("system", "You are a helpful assistant."));
		msg.add(new ChatMessage("user", prompt));
		req.setMessages(msg);

		System.out.println(req.toString());

		ChatCompletionsResponse resp = ep.getClient().createChatCompletion(req);
		if ((resp.getChoices() == null) || (resp.getChoices().size() == 0))
			return "";
		return resp.getChoices().get(0).getMessage().getContent();
	}

}
