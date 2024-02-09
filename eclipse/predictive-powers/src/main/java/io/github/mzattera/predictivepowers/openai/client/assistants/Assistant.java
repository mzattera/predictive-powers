package io.github.mzattera.predictivepowers.openai.client.assistants;

import java.util.ArrayList;
import java.util.List;

import io.github.mzattera.predictivepowers.openai.client.Metadata;
import io.github.mzattera.predictivepowers.openai.client.chat.OpenAiTool;
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
 * Represents an assistant that can call the model and use tools.
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
public class Assistant extends Metadata {

	/**
	 * The identifier, which can be referenced in API endpoints.
	 */
	@NonNull
	private String id;

	/**
	 * The object type, which is always assistant.
	 */
	@NonNull
	private String object;

	/**
	 * The Unix timestamp (in seconds) for when the assistant was created.
	 */
	private long createdAt;

	/**
	 * The name of the assistant. Optional. The maximum length is 256 characters.
	 */
	private String name;

	/**
	 * The description of the assistant. Optional. The maximum length is 512
	 * characters.
	 */
	private String description;

	/**
	 * ID of the model to use. Mandatory. You can use the List models API to see all
	 * of your available models, or see our Model overview for descriptions of them.
	 */
	@NonNull
	private String model;

	/**
	 * The system instructions that the assistant uses. Optional. The maximum length
	 * is 32768 characters.
	 */
	private String instructions;

	/**
	 * A list of tools enabled on the assistant. Mandatory. There can be a maximum
	 * of 128 tools per assistant. Tools can be of types code_interpreter,
	 * retrieval, or function.
	 */
	@NonNull
	@Builder.Default
	private List<OpenAiTool> tools = new ArrayList<>();

	/**
	 * A list of file IDs attached to this assistant. Mandatory. There can be a
	 * maximum of 20 files attached to the assistant. Files are ordered by their
	 * creation date in ascending order.
	 */
	@NonNull
	@Builder.Default
	private List<String> fileIds = new ArrayList<>();
}
