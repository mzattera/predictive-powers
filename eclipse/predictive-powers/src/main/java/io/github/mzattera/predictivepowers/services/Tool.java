/*
 * Copyright 2023-2025 Massimiliano "Maxi" Zattera
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

package io.github.mzattera.predictivepowers.services;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonValue;

import io.github.mzattera.predictivepowers.services.messages.JsonSchema;
import io.github.mzattera.predictivepowers.services.messages.ToolCall;
import io.github.mzattera.predictivepowers.services.messages.ToolCallResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * This is a tool that an {@link Agent} can invoke to perform a task.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
public interface Tool extends AutoCloseable {

	/**
	 * Describes a parameter used in tool calls. Notice this same data structure is
	 * also used to represent JSON Schema (see {@link JsonSchema}).
	 * 
	 * {@link JsonSchema} provide methods to convert JSON schema into parameters and
	 * vice versa, including using classes as schema templates.
	 */
	@NoArgsConstructor
	@AllArgsConstructor
	@Getter
	@Setter
	@ToString
	public static class ToolParameter {

		public static enum Type {
			INTEGER("integer"), DOUBLE("number"), BOOLEAN("boolean"), ENUMERATION("string"), STRING("string"),
			ARRAY("array"), OBJECT("object");

			private final String label;

			private Type(String label) {
				this.label = label;
			}

			@Override
			@JsonValue
			public String toString() { // Notice we rely on labels not to change
				return label;
			}
		}

		/**
		 * Field name. This can be null for unnamed parameters which are typically used
		 * when defining ARRAY item type (see {@link #getArrayItemType()}).
		 */
		private String name;

		/** Field type */
		private Type type;

		/** If field type is ENUM, this lists allowed values */
		private List<String> enumValues;

		/**
		 * If field type is an OBJECT defined through $ref, this is the referenced
		 * schema.
		 * 
		 * If field type is ARRAY, this contains a definition of its item type; notice
		 * that for items which are not OBJECTs, this is an unnamed parameter defining
		 * the type (e.g. STRING, or ARRAY).
		 */
		private ToolParameter objectType;

		/**
		 * If field type is OBJECT and it was not defined through $ref, this lists its
		 * fields.
		 */
		private List<ToolParameter> objectFields = new ArrayList<>();

		/** A description of the field */
		private String description;

		/** True if this is a required field */
		private boolean required;
	}

	/**
	 * Unique identifier for the tool.
	 * 
	 * @return
	 */
	String getId();

	/**
	 * 
	 * @return A verbose description of what the tool does does, so that the agent
	 *         knows when to call it.
	 */
	String getDescription();

	/**
	 * 
	 * @return A List with a description of each parameter needed when the tool is
	 *         invoked.
	 */
	List<ToolParameter> getParameters();

	/**
	 * Sets the capability to which this tool belongs.
	 * 
	 * This is called once after a tool is instantiated and before
	 * {@link #init(Agent)} is called.
	 * 
	 * @param capability
	 */
	void setCapability(Capability capability);

	/**
	 * 
	 * @return The capability to which the agent belongs.
	 */
	Capability getCapability();

	/**
	 * 
	 * @return True if the tool was already initialized.
	 */
	boolean isInitialized();

	/**
	 * 
	 * @return True if the tool was already closed.
	 */
	boolean isClosed();

	/**
	 * This must be called by the agent once and only once before any invocation to
	 * this tool.
	 * 
	 * @param agent The agent that will then invoke this tool, eventually.
	 * @throws ToolInitializationException If an error occurs while initializing the
	 *                                     agent, or the agent was already
	 *                                     initialized or closed.
	 */
	void init(@NonNull Agent agent) throws ToolInitializationException;

	/**
	 * Invokes (executes) the tool. This can be called several times.
	 * 
	 * @param call The call to the tool, created by the calling agent.
	 * 
	 * @return The result of calling the tool.
	 * 
	 * @param IllegalStateException if the tool was not yet initialized.
	 */
	ToolCallResult invoke(@NonNull ToolCall call) throws Exception;
}