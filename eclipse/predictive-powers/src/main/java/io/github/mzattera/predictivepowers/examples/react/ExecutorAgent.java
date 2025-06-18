/*
 * Copyright 2023 Massimiliano "Maxi" Zattera
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * 
 */
package io.github.mzattera.predictivepowers.examples.react;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;

import io.github.mzattera.predictivepowers.openai.client.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.Agent;
import io.github.mzattera.predictivepowers.services.Capability;
import io.github.mzattera.predictivepowers.services.Tool;
import io.github.mzattera.predictivepowers.services.ToolInitializationException;
import io.github.mzattera.predictivepowers.services.messages.JsonSchema;
import io.github.mzattera.predictivepowers.services.messages.ToolCall;
import io.github.mzattera.predictivepowers.services.messages.ToolCallResult;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * This is a {@link ReactAgent} wrapped into a {@link Tool}, so it can be used
 * as a tool by other agents.
 */
public class ExecutorAgent extends ReactAgent implements Tool {

	// TODO Maybe merge with ReactAgent? Might be a 6good idea to keep 7tool
	// interface separated though
	// TODO Maybe better to have a Tool wrapped as inner class rather than implement
	// all get methods?

	@JsonSchemaDescription("This describes parameters needed to call an execution agent")
	public static class Parameters extends ReactAgent.Parameters {

		@JsonProperty(required = true)
		@JsonPropertyDescription("A question that this tool must answer or a command it must execute.")
		public String question;
	}

	@Getter
	@Setter
	private String description = "";

	@Getter
	@NonNull
	private List<ToolParameter> parameters = new ArrayList<>();

	// Protects us from user removing thought and question from parameters

	protected void setParameters(List<? extends ToolParameter> parameters) {
		// Our tool needs at least to support these parameters
		Set<String> names = parameters.stream().map(ToolParameter::getName).collect(Collectors.toSet());
		if (!names.contains("thought") || !names.contains("question"))
			throw new IllegalArgumentException("Execution agents must accept thought and question parameters");
		this.parameters = new ArrayList<>(parameters);
	}

	protected void setParameters(Class<?> c) {
		setParameters(JsonSchema.getParameters(c));
	}

	@Getter
	@Setter
	private Capability capability;

	@Getter(AccessLevel.PROTECTED)
	@Setter(AccessLevel.PROTECTED)
	private Agent agent;

	@Getter
	@Setter(AccessLevel.PROTECTED)
	private boolean closed = false;

	@Override
	public boolean isInitialized() {
		return (agent != null);
	}

	@Override
	public void init(@NonNull Agent agent) throws ToolInitializationException {
		if (isInitialized())
			throw new ToolInitializationException("Tool " + getId() + " is already initialized");
		if (closed)
			throw new ToolInitializationException("Tool " + getId() + " is already closed");
		this.agent = agent;
	}

	public ExecutorAgent(@NonNull String id, @NonNull String description, @NonNull OpenAiEndpoint enpoint,
			@NonNull List<? extends Tool> tools) throws ToolInitializationException, JsonProcessingException {
		super(id, enpoint, tools);
		this.description = description;
		setParameters(Parameters.class);
	}

	@Override
	public ToolCallResult invoke(@NonNull ToolCall call) throws Exception {
		if (!isInitialized())
			throw new IllegalStateException("Tool must be initialized.");

		Step result = execute(getString("question", call.getArguments()));
		switch (result.status) {

		case ERROR:
			return new ToolCallResult(call, "ERROR: " + result.observation);
		default:
			return new ToolCallResult(call, result.observation);
		}
	}

	@Override
	public void close() {
		closed = true;
		super.close();
	}

	// Utility methods to read parameters
	// TODO URGENT be smarter
	// ////////////////////////////////////////////////////////////////////////////////////////////

	protected static boolean getBoolean(String name, Map<String, ? extends Object> args) {
		if (args.containsKey(name))
			return getBoolean(name, args.get(name));
		throw new IllegalArgumentException("Missing required parameter \"" + name + "\".");
	}

	protected static boolean getBoolean(String name, Map<String, ? extends Object> args, boolean def) {
		if (!args.containsKey(name))
			return def;
		return getBoolean(name, args.get(name));
	}

	private static boolean getBoolean(String name, Object value) {
		String s = value.toString();
		if ("true".equals(s.trim().toLowerCase()))
			return true;
		if ("false".equals(s.trim().toLowerCase()))
			return false;

		throw new IllegalArgumentException(
				"Parameter \"" + name + "\" is expected to be a boolean value but it is not.");
	}

	protected static long getLong(String name, Map<String, ? extends Object> args) {
		if (args.containsKey(name))
			return getLong(name, args.get(name));
		throw new IllegalArgumentException("Missing required parameter \"" + name + "\".");
	}

	protected static long getLong(String name, Map<String, ? extends Object> args, long def) {
		if (!args.containsKey(name))
			return def;
		return getLong(name, args.get(name));
	}

	private static long getLong(String name, Object value) {
		try {
			return Long.parseLong(value.toString());
		} catch (Exception e) {
			throw new IllegalArgumentException(
					"Parameter \"" + name + "\" is expected to be a integer value but it is not.");
		}
	}

	protected static double getDouble(String name, Map<String, ? extends Object> args) {
		if (args.containsKey(name))
			return getDouble(name, args.get(name));
		throw new IllegalArgumentException("Missing required parameter \"" + name + "\".");
	}

	protected static double getDouble(String name, Map<String, ? extends Object> args, double def) {
		if (!args.containsKey(name))
			return def;
		return getDouble(name, args.get(name));
	}

	protected static double getDouble(String name, Object value) {
		try {
			return Double.parseDouble(value.toString());
		} catch (Exception e) {
			throw new IllegalArgumentException(
					"Parameter \"" + name + "\" is expected to be a decimal number but it is not.");
		}
	}

	protected static String getString(String name, Map<String, ? extends Object> args) {
		Object result = args.get(name);
		if (result == null)
			return null;
		return result.toString();
	}

	protected static String getString(String name, Map<String, ? extends Object> args, String def) {
		if (!args.containsKey(name))
			return def;
		return getString(name, args);
	}
}