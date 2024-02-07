package io.github.mzattera.predictivepowers.openai.client.threads;

import java.util.ArrayList;
import java.util.List;

import io.github.mzattera.predictivepowers.openai.client.Metadata;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatMessage.Role;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * A Thread Message from the OpenAI threads API.
 * 
 * @author GPT-4
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Getter
@Setter
@ToString
public class Message extends Metadata {

	/**
	 * The role of the entity that is creating the message. Required.
	 */
	@NonNull
	private final Role role = Role.USER;

	/**
	 * The content of the message. Required.
	 */
	@NonNull
	private String content;

	/**
	 * A list of File IDs that the message should use. Optional. Defaults to an
	 * empty list. There can be a maximum of 10 files attached to a message.
	 */
	@Builder.Default
	private List<String> fileIds = new ArrayList<>();
}
