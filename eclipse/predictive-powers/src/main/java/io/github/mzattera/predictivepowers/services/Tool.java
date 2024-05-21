/*
 * Copyright 2023-2024 Massimiliano "Maxi" Zattera
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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonValue;

import io.github.mzattera.predictivepowers.services.messages.ToolCall;
import io.github.mzattera.predictivepowers.services.messages.ToolCallResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
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
	 * Describes one tool parameter.
	 */
	@NoArgsConstructor
	@RequiredArgsConstructor
	@AllArgsConstructor
	@Getter
	@Setter
	@ToString
	public static class ToolParameter {

		public static enum Type {
			INTEGER("integer"), DOUBLE("number"), BOOLEAN("boolean"), ENUMERATION("string"), STRING("string");

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

		/** Field name */
		@NonNull
		public String name;

		/** Field type */
		public Type type;

		/** If field type is ENUM, this lists allowed values */
		public List<String> emum;

		/** A description of the field */
		public String description;

		/** True if this is a required field */
		public boolean required;
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
	List<? extends ToolParameter> getParameters();

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
	 * This must be called by the agent once and only once before any invocation to
	 * this tool.
	 * 
	 * @param agent The agent that will then invoke this tool, eventually.
	 * @param IllegalStateException if the tool was already initialized.
	 */
	void init(@NonNull Agent agent) throws ToolInitializationException, IllegalStateException;

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