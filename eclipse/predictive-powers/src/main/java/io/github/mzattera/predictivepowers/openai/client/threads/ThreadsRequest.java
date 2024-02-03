package io.github.mzattera.predictivepowers.openai.client.threads;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.mzattera.predictivepowers.openai.services.OpenAiChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents a conversation thread with an OpenAI assistant.
 * 
 * @author Massimiliano "Maxi" Zattera
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
//@RequiredArgsConstructor
@AllArgsConstructor
@ToString
public class ThreadsRequest {

	@Builder.Default
	private List<OpenAiChatMessage> messages = new ArrayList<>();

	/**
	 * Set of 16 key-value pairs that can be attached to an object. This can be
	 * useful for storing additional information about the object in a structured
	 * format. Keys can be a maximum of 64 characters long and values can be a
	 * maximum of 512 characters long.
	 */
	@Builder.Default
	Map<String, String> metadata = new HashMap<>();
}
