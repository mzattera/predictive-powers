/*
 * Copyright 2024-2025 Massimiliano "Maxi" Zattera
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
package io.github.mzattera.predictivepowers.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.github.mzattera.predictivepowers.services.messages.JsonSchema;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * This is an abstract class that implementations of {@link Tool}s can extend.
 * 
 * @author Massimiliano "Maxi" Zattera
 */
public abstract class AbstractTool implements Tool {

	@Getter
	private final String id;

	@Getter
	@Setter(AccessLevel.PROTECTED)
	private String description = "";

	@Getter
	@NonNull
	private List<ToolParameter> parameters = new ArrayList<>();

	protected void setParameters(List<? extends ToolParameter> parameters) {
		this.parameters = new ArrayList<>(parameters);
	}

	/**
	 * This method allows setting the parameters of this tool using a JSON schema.
	 * 
	 * See {@link io.github.mzattera.predictivepowers.examples.FunctionCallExample}
	 * for examples.
	 * 
	 * @see <a href=
	 *      "https://platform.openai.com/docs/guides/function-calling?api-mode=chat">function
	 *      calling guide</a>
	 * 
	 * @param schema A class with JSON schema annotation, which schema will be used
	 *               to derive parameters for this tool.
	 * @throws JsonProcessingException
	 */
	protected void setParameters(Class<?> schema) throws JsonProcessingException {
		parameters = JsonSchema.getParameters(schema);
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
			throw new ToolInitializationException("Tool " + id + " is already initialized");
		if (closed)
			throw new ToolInitializationException("Tool " + id + " is already closed");
		this.agent = agent;
	}

	protected AbstractTool(@NonNull String id) {
		this(id, id, new ArrayList<>());
	}

	protected AbstractTool(@NonNull String id, String description) {
		this(id, description, new ArrayList<>());
	}

	protected AbstractTool(@NonNull String id, String description, @NonNull List<? extends ToolParameter> parameters) {
		this.id = id;
		this.description = description;
		this.parameters = new ArrayList<>(parameters);
	}

	protected AbstractTool(@NonNull String id, String description, @NonNull Class<?> schema) {
		this.id = id;
		this.description = description;
		this.parameters = JsonSchema.getParameters(schema);
	}

	@Override
	public void close() {
		closed = true;
	}

	// Utility methods to read parameters
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
