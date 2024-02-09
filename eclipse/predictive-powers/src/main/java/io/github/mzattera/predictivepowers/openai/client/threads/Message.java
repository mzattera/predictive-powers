package io.github.mzattera.predictivepowers.openai.client.threads;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonValue;

import io.github.mzattera.predictivepowers.openai.client.Metadata;
import lombok.AccessLevel;
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
@SuperBuilder
@Getter
@Setter
@ToString
public class Message extends Metadata {

	public enum Role {
		USER("user"), ASSISTANT("assistant");

		private final String label;

		private Role(String label) {
			this.label = label;
		}

		@Override
		@JsonValue
		public String toString() {
			return label;
		}
	}

	/**
	 * The identifier, which can be referenced in API endpoints.
	 */
	// @NonNull can be null when a me
	private String id;

	/**
	 * The object type, which is always thread.message.
	 */
	@NonNull
	private String object;

	/**
	 * The Unix timestamp (in seconds) for when the message was created.
	 */
	private long createdAt;

	/**
	 * The thread ID that this message belongs to.
	 */
	@NonNull
	private String threadId;

	/**
	 * The entity that produced the message. One of user or assistant.
	 */
	@NonNull
	private Role role;

	/**
	 * The content of the message in array of text and/or images.
	 */
	@NonNull
	@Builder.Default
	private List<Content> content = new ArrayList<>();

	/**
	 * If applicable, the ID of the assistant that authored this message.
	 */
	private String assistantId;

	/**
	 * If applicable, the ID of the run associated with the authoring of this
	 * message.
	 */
	private String runId;

	/**
	 * A list of file IDs that the assistant should use. Useful for tools like
	 * retrieval and code_interpreter that can access files.
	 */
	@Builder.Default
	private List<String> fileIds = new ArrayList<>();
}
