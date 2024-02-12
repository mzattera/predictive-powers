/**
 * 
 */
package io.github.mzattera.predictivepowers.services;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.github.mzattera.predictivepowers.openai.client.chat.Function;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * This is an abstract class that implementations of {@link Tool}s can extend.
 * 
 * Notice tools must provide a default constructor.
 */
@RequiredArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class AbstractTool implements Tool {
	
	@Getter
	private final String id;

	@Getter
	@Setter(AccessLevel.PROTECTED)
	private String description = "";

	@Getter
	@NonNull 
	private List<? extends ToolParameter> parameters = new ArrayList<>();

	protected void setParameters(List<? extends ToolParameter> parameters) {
		this.parameters = parameters;
	}

	/**
	 * This method allows setting the parameters of this tool using a JSON schema.
	 * 
	 * See {@link io.github.mzattera.predictivepowers.examples.FunctionCallExample}
	 * or
	 * {@linkplain https://platform.openai.com/docs/guides/text-generation/function-calling
	 * here}. for examples.
	 * 
	 * @param schema A class with JSON schema annotation, which schema will be used
	 *               to derive parameters for this tool.
	 * @throws JsonProcessingException
	 */
	protected void setParameters(Class<?> schema) throws JsonProcessingException {
		parameters = Function.getParametersFromSchema(schema);
	}

	@Getter
	@Setter
	private Capability capability;

	@Getter (AccessLevel.PROTECTED)
	@Setter (AccessLevel.PROTECTED)
	private Agent agent;

	@Getter
	@Setter(AccessLevel.PROTECTED)
	private boolean initialized = false;

	@Override
	public void init(@NonNull Agent agent) {
		if (initialized)
			throw new IllegalStateException("Tool not yet initialized");
		this.agent=agent;
		initialized=true;
	}

	protected AbstractTool(@NonNull String id, String description) {
		this(id, description, new ArrayList<>());
	}

	protected AbstractTool(@NonNull String id, String description, @NonNull List<ToolParameter> parameters) {
		this.id = id;
		this.description = description;
		this.parameters = parameters;
	}

	protected AbstractTool(@NonNull String id, String description, @NonNull Class<?> schema) {
		this.id = id;
		this.description = description;
		this.parameters = Function.getParametersFromSchema(schema);
	}

	@Override
	public void close() {
	}
}
