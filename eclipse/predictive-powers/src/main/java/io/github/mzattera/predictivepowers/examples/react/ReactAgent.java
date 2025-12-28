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

/**
 * 
 */
package io.github.mzattera.predictivepowers.examples.react;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;

import io.github.mzattera.predictivepowers.openai.OpenAiChatService;
import io.github.mzattera.predictivepowers.openai.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.Tool;
import io.github.mzattera.predictivepowers.services.ToolInitializationException;
import io.github.mzattera.predictivepowers.services.messages.JsonSchema;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

/**
 * This implements a generic ReAct agent.
 */
public class ReactAgent extends OpenAiChatService {

	public static final String DEFAULT_MODEL = "gpt-4.1";
//	public static final String DEFAULT_MODEL = "gpt-4o";
//	public static final String DEFAULT_MODEL = "o3";

	/**
	 * Base class for {@link Tool} parameters for those tools that are available to
	 * a ReAct agent. They must all accept at least a thought parameter.
	 */
	@NoArgsConstructor
	public static class Parameters {

		@JsonProperty(required = true)
		@JsonPropertyDescription("Your reasoning about why this tool has been called.")
		public @NonNull String thought;
	}

	// TODO Remove actor

	@NoArgsConstructor
	@JsonSchemaDescription("This represent the final execution step performed by a ReAct agent.")
	public static class Step {

		public static class Views { public interface Compact {} public interface Complete extends Compact {} }
		
		public enum Status {
			IN_PROGRESS, COMPLETED, ERROR
		};

		@JsonPropertyDescription("If you finish the execution or you experience an unrecoverable error, set this to either COMPLETED or ERROR respectively.")
		@JsonView(Views.Compact.class)
		public Status status;

		// Do not remove it's OK it stays here
		@JsonPropertyDescription("The tool or agent that executed this step. This is provided automatically, so you do not need to output it.")
		@JsonView(Views.Compact.class)
		public @NonNull String actor;

		@JsonProperty(required = true)
		@JsonPropertyDescription("Your reasoning about why and how accomplish this step.")
		@JsonView(Views.Compact.class)
		public @NonNull String thought;

		@JsonProperty(required = true)
		@JsonPropertyDescription("Any additional data, like step outcomes, error messages, etc..")
		@JsonView(Views.Compact.class)
		public @NonNull String observation;

		// Private constructor to force use of the builder
		private Step(Builder builder) {
			this.status = builder.status;
			this.actor = Objects.requireNonNull(builder.actor, "id must not be null");
			this.thought = Objects.requireNonNull(builder.thought, "thought must not be null");
			this.observation = Objects.requireNonNull(builder.observation, "observation must not be null");
		}

		/**
		 * Builder for Step.
		 */
		public static class Builder {
			private Status status;
			private String actor;
			private String thought;
			private String observation;

			public Builder status(Status status) {
				this.status = status;
				return this;
			}

			public Builder actor(@NonNull String actor) {
				this.actor = actor;
				return this;
			}

			public Builder thought(@NonNull String thought) {
				this.thought = thought;
				return this;
			}

			public Builder observation(@NonNull String observation) {
				this.observation = observation;
				return this;
			}

			public Step build() {
				return new Step(this);
			}
		}
	}

	@NoArgsConstructor
	@JsonSchemaDescription("This extends execution step to cover function calls")
	public static class ToolCallStep extends Step {

		@JsonProperty(required = true)
		@JsonPropertyDescription("The action that was taken at this step. It is typically a tool invocation.")
		@JsonView(Views.Compact.class)
		public @NonNull String action;

		@JsonProperty(required = true, value="action_input")
		@JsonPropertyDescription("Input for the action.")
		@JsonView(Views.Compact.class)
		public @NonNull String actionInput;

		@JsonProperty(required = true, value="action_steps")
		@JsonPropertyDescription("In case the action for this step was delegated to another agent, this is the list of steps that agent performed to complete the action.")
		@JsonView(Views.Complete.class)
		public @NonNull List<Step> actionSteps;

		private ToolCallStep(Builder builder) {
			super(builder);
			this.action = Objects.requireNonNull(builder.action, "action must not be null");
			this.actionInput = Objects.requireNonNull(builder.actionInput, "actionInput must not be null");
			this.actionSteps = new ArrayList<>(builder.actionSteps);
		}

		/**
		 * Builder for ToolCallStep.
		 */
		public static class Builder extends Step.Builder {
			private String action;
			private String actionInput;
			public List<Step> actionSteps = new ArrayList<>();

			@Override
			public Builder status(Status status) {
				return (Builder) super.status(status);
			}

			@Override
			public Builder actor(@NonNull String actor) {
				return (Builder) super.actor(actor);
			}

			@Override
			public Builder thought(@NonNull String thought) {
				return (Builder) super.thought(thought);
			}

			@Override
			public Builder observation(@NonNull String observation) {
				return (Builder) super.observation(observation);
			}

			public Builder action(@NonNull String action) {
				this.action = action;
				return this;
			}

			public Builder actionInput(@NonNull String actionInput) {
				this.actionInput = actionInput;
				return this;
			}

			public Builder actionSteps(@NonNull List<? extends Step> steps) {
				this.actionSteps = new ArrayList<>(steps);
				return this;
			}

			public Builder addAllSteps(@NonNull List<? extends Step> steps) {
				this.actionSteps.addAll(steps);
				return this;
			}

			public Builder addStep(@NonNull Step step) {
				this.actionSteps.add(step);
				return this;
			}

			@Override
			public ToolCallStep build() {
				return new ToolCallStep(this);
			}
		}
	}

	/**
	 * Any additional context you want to provide to the agent.
	 */
	@Getter
	@Setter
	// TODO URGENT Check how this is used by executor and reviewer
	private @NonNull String context = "";

	/**
	 * Any additional examples you want to provide to the agent.
	 */
	@Getter
	@Setter
	// TODO URGENT Check how this is used by executor and reviewer
	private @NonNull String examples = "";

	@Getter(AccessLevel.PROTECTED)
	private final @NonNull ExecutorModule executor;

	@Getter(AccessLevel.PROTECTED)
	private final @NonNull CriticModule reviewer;

	// TODO Make it a wrapper of another agent instead? A lot of code forwarding...

	public ReactAgent(@NonNull String id, @NonNull OpenAiEndpoint enpoint, @NonNull List<? extends Tool> tools,
			boolean checkLastStep) throws ToolInitializationException {

		// TODO URGENT Better constructor or builder that takes context as well
		// PRobably we do not need to extend agents? Let's see.

		super(id, enpoint, DEFAULT_MODEL);
		this.executor = new ExecutorModule(this, tools, checkLastStep);
		this.reviewer = new CriticModule(this, tools);
		setTemperature(0d);

		// TODO Set max output (?)
	}

	public Step execute(@NonNull String command) throws JsonProcessingException {
		return executor.execute(command);
	}

	public List<Step> getSteps() {
		return executor.getSteps();
	}
	
	public static void main(String[] args) {
		System.out.println(JsonSchema.getJsonSchema(ToolCallStep.class));
	}
}
