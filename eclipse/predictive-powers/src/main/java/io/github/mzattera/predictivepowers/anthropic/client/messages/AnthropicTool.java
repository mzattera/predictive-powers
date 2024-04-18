/**
 * 
 */
package io.github.mzattera.predictivepowers.anthropic.client.messages;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.mzattera.predictivepowers.services.Agent;
import io.github.mzattera.predictivepowers.services.Capability;
import io.github.mzattera.predictivepowers.services.Tool;
import io.github.mzattera.predictivepowers.services.ToolInitializationException;
import io.github.mzattera.predictivepowers.services.messages.JsonSchema;
import io.github.mzattera.predictivepowers.services.messages.ToolCall;
import io.github.mzattera.predictivepowers.services.messages.ToolCallResult;
import lombok.NonNull;

/**
 * This class is a {@link Tool} that Anthropic API can use.
 */
public class AnthropicTool implements Tool {

	/**
	 * Utility class to (de)serialize JSON schema.
	 * This hides parametrs that are not required.
	 */
	static class AnthropicJsonSchema extends JsonSchema {
		AnthropicJsonSchema(@NonNull List<? extends ToolParameter> parameters) {
			super(null, null, "object", null, parameters);
		}
	}

	/**
	 * For easier interoperability and abstraction, an AnthropicTool can be built as
	 * a wrapper around any Tool instance. If this was the case, this is the wrapped
	 * Tool.
	 */
	@JsonIgnore
	private final Tool wrappedTool;

	/**
	 * Builds an instance as a wrapper around an existing tool.
	 */
	public AnthropicTool(Tool tool) {
		wrappedTool = tool;
	}

	@Override
	public void close() throws Exception {
		wrappedTool.close();
	}

	@Override
	@JsonProperty("name")
	public String getId() {
		return wrappedTool.getId();
	}

	@Override
	public String getDescription() {
		return wrappedTool.getDescription();
	}

	@JsonIgnore
	@Override
	public List<? extends ToolParameter> getParameters() {
		return wrappedTool.getParameters();
	}

	// Used for serialization
	public AnthropicJsonSchema getInputSchema() {
		return new AnthropicJsonSchema(wrappedTool.getParameters());
	}

	@JsonIgnore
	@Override
	public void setCapability(Capability capability) {
		wrappedTool.setCapability(capability);
	}

	@JsonIgnore
	@Override
	public Capability getCapability() {
		return wrappedTool.getCapability();
	}
	
	@JsonIgnore
	@Override
	public boolean isInitialized() {
		return wrappedTool.isInitialized();
	}

	@Override
	public void init(@NonNull Agent agent) throws ToolInitializationException, IllegalStateException {
		wrappedTool.init(agent);
	}

	@Override
	public ToolCallResult invoke(@NonNull ToolCall call) throws Exception {
		return wrappedTool.invoke(call);
	}
}
