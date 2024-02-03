/**
 * 
 */
package io.github.mzattera.predictivepowers.services;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * This is a message exchanged with an {@link Agent}.
 */
@NoArgsConstructor
@ToString
public class AgentMessage extends ChatMessage {

	/** Agents understand files as part of the message */
	@Getter
	@NonNull
	// TODO URGENT Use RemoteFile instead of File
	// TODO URGENT New OpenAI message extending this for Assistants and possibly
	// later on for chat. Add metadata to that.
	private final List<File> files = new ArrayList<>();

	public AgentMessage(Author author, String content) {
		super(author, content);
	}

	public AgentMessage(Author author, String content, List<File> files) {
		super(author, content);
		files.addAll(files);
	}
}
