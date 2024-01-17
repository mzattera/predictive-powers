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
 * This is a message exchanged with an {@link AgentService}.
 */
@NoArgsConstructor
@ToString
public class AgentMessage extends ChatMessage {

	/** Agents understand files as part of the message */
	@Getter
	@NonNull
	private final List<File> files = new ArrayList<>();

	public AgentMessage(Author author, String content) {
		super(author, content);
	}

	public AgentMessage(Author author, String content, List<File> files) {
		super(author, content);
		files.addAll(files);
	}
}
