package io.github.mzattera.predictivepowers.openai.client.assistants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.mzattera.predictivepowers.openai.client.chat.OpenAiTool;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents the configuration of an assistant, including details such as name,
 * description, instructions, tools, functions, and attached files.
 * 
 * @author GPT-4
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
@ToString
public class AssistantsRequest {

	/**
	 * The ID of the model to use. This is a required field. You can use the List
	 * models API to see all of your available models, or see our Model overview for
	 * descriptions of them.
	 */
	@NonNull
	private String model;

	/**
	 * The name of the assistant. This is an optional field. The maximum length is
	 * 256 characters.
	 */
	private String name;

	/**
	 * The description of the assistant. This is an optional field. The maximum
	 * length is 512 characters.
	 */
	private String description;

	/**
	 * The system instructions that the assistant uses. This is an optional field.
	 * The maximum length is 32768 characters.
	 */
	private String instructions;

	/**
	 * A list of tools enabled on the assistant. This is an optional field. Defaults
	 * to an empty list. There can be a maximum of 128 tools per assistant. Tools
	 * can be of types code_interpreter, retrieval, or function.
	 */
	@Builder.Default
	private List<OpenAiTool> tools = new ArrayList<>();

	/**
	 * A list of file IDs attached to this assistant. This is an optional field.
	 * Defaults to an empty list. There can be a maximum of 20 files attached to the
	 * assistant. Files are ordered by their creation date in ascending order.
	 */
	@Builder.Default
	private List<String> fileIds = new ArrayList<>();

	/**
	 * A map of metadata, consisting of key-value pairs, that can be attached to the
	 * assistant. This is an optional field. Keys can be a maximum of 64 characters
	 * long, and values can be a maximum of 512 characters long.
	 */
	private Map<String, String> metadata;
}
