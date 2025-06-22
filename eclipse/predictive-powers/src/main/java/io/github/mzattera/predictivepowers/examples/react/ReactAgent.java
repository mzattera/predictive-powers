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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;

import io.github.mzattera.predictivepowers.openai.client.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatService;
import io.github.mzattera.predictivepowers.services.Capability.ToolAddedEvent;
import io.github.mzattera.predictivepowers.services.CompletionService;
import io.github.mzattera.predictivepowers.services.Tool;
import io.github.mzattera.predictivepowers.services.Tool.ToolParameter;
import io.github.mzattera.predictivepowers.services.ToolInitializationException;
import io.github.mzattera.predictivepowers.services.Toolset;
import io.github.mzattera.predictivepowers.services.messages.ChatCompletion;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage;
import io.github.mzattera.predictivepowers.services.messages.JsonSchema;
import io.github.mzattera.predictivepowers.services.messages.ToolCall;
import io.github.mzattera.predictivepowers.services.messages.ToolCallResult;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

/**
 * This implements a generic ReAct agent.
 */
public class ReactAgent extends OpenAiChatService {

	private final static Logger LOG = LoggerFactory.getLogger(ReactAgent.class);

	public static final String DEFAULT_MODEL = "gpt-4.1";

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

	// TODO URGENT Remove actor

	@NoArgsConstructor
	@JsonSchemaDescription("This represent the final execution step performed by a ReAct agent.")
	public static class Step {

		public enum Status {
			COMPLETED, ERROR
		};

		@JsonPropertyDescription("If you finish the execution or you experience an unrecoverable error, set this to either COMPLETED or ERROR respectively.")
		public Status status;

		// Do not remove it's OK it stays here
		@JsonPropertyDescription("The tool or agent that executed this step. This is provided automatically, so you do not need to output it.")
		public @NonNull String actor;

		@JsonProperty(required = true)
		@JsonPropertyDescription("Your reasoning about why and how accomplish this step.")
		public @NonNull String thought;

		@JsonProperty(required = true)
		@JsonPropertyDescription("Any additional data, like step outcomes, error messages, etc..")
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
		public @NonNull String action;

		@JsonProperty(required = true)
		@JsonPropertyDescription("Input for the action.")
		public @NonNull String actionInput;

		@JsonProperty(required = true)
		@JsonPropertyDescription("In case the action for this step was delegated to another agent, this is the list of steps that agent performed to complete the action.")
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

	public static final String PROMPT_TEMPLATE = "# Identity\n\n" //
			+ "You are a ReAct (Reasoning and Acting) agent; you task is to execute the below user command in <user_command> tag.\n"
			+ "\n<user_command>\n{{question}}\n</user_command>\n" //
//			+ "You are running inside a bigger process, the steps you and other agents already executed in the process are listed below in the <steps> tag.\n"
			+ "\n# Additional Context and Information\n\n" //
			+ "{{context}}\n\n" //
			+ "\n# Instructions\n\n" //
			+ "  * Carefully plan the steps required to execute the user's command.\n"
			+ "  * At each new step, use the most suitable tool at your disposal to progress towards executing the user's command. **NEVER** output a step to indicate a tall call, but call the tool directly.\n"
			+ "  * When planning the next step, carefully consider all of the steps you already executed that are contained in the conversation. Consider the thought that caused you to call each tool, usually provided as \"thought\" field in \"actionInput\" field, and observe the result of the call, before planning next step.\n"
//			+ "  * When planning the next step, also carefully consider all of the steps already executed in the process which are stored inside <step> tag.\n"
			+ "  * When you are done with executing the command, output one final step with status=\"COMPLETED\".\n"
			+ "  * If you are experiencing an error, try to act differently and recover from it; if you are unable to recover, output one final step with status=\"ERROR\".\n"
			+ "  * **IMPORTANTLY**, When you output one final step with status=\"ERROR\", clearly and in detail describe in observation field the reason of your failure. If the command lacked any necessary information, list missing information clearly and in detail. Suggest to the user any change or additions they could do to the command to help you to execute it.\n"
			+ "  * The format of the last step to output is described in the below schema; use this very format when outputting the final step.\n" //
			+ "\n<schema>\n" + JsonSchema.fromSchema(Step.class).asJsonSchema() + "\n</schema>\n\n" //
//			+ "\n<step>\n{{steps}}\n</steps>\n";
	;

	/**
	 * Any additional context you want to provide to the agent.
	 */
	@Getter
	@Setter
	private @NonNull String context = "";

	/**
	 * Last question the agent needs to answer / command to execute.
	 */
	@Getter
	private String question;

	/**
	 * The list of steps executed so far. while answering to last question /
	 * executing last command.
	 */
	// TODO URGENT better engineering of this ? maybe return them with execute instead?
	protected final @NonNull List<Step> steps = new ArrayList<>();

	/**
	 * Immutable list of steps executed so far.
	 * 
	 * @return
	 */
	public List<Step> getSteps() {
		return Collections.unmodifiableList(steps);
	}

	/**
	 * @return Last step so far (or null).
	 */
	public Step getLastStep() {
		return (steps.size() == 0) ? null : steps.get(steps.size() - 1);
	}

	@Override
	public void onToolAdded(@NonNull ToolAddedEvent evt) {
		// Our tool needs at least to support these parameters
		Tool tool = evt.getTool();
		Set<String> names = tool.getParameters().stream().map(ToolParameter::getName).collect(Collectors.toSet());
		if (!names.contains("thought"))
			throw new IllegalArgumentException(
					"All tools available to ReactAgent instances must accepta a \"thought\" parameter as a minimum.");

		super.onToolAdded(evt);
		;
	}

	// TODO Make it a wrapper of another agent instead? A lot of code forwarding...

	public ReactAgent(@NonNull String id, @NonNull OpenAiEndpoint enpoint, @NonNull List<? extends Tool> tools)
			throws ToolInitializationException {

		// TODO URGENT Better constructor or builder that takes context as well

		super(id, enpoint, DEFAULT_MODEL);
		addCapability(new Toolset(tools));
		setTemperature(0d);
		setResponseFormat(Step.class);

		// TODO URGENT Set max output (?)
	}

	public Step execute(@NonNull String question) throws JsonProcessingException {
		this.question = question;
		this.steps.clear();
		clearConversation();
		Map<String, String> map = new HashMap<>();
		map.put("question", question);
		map.put("context", context);
		map.put("steps", JsonSchema.JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(steps));

		setPersonality(CompletionService.fillSlots(PROMPT_TEMPLATE, map));

		Step step = new Step.Builder() //
				.actor(getId()) //
				.thought(CompletionService.fillSlots(
						"I am starting execution of the below user's command in <user_command> tag.\n\n<user_command>\n{{question}}\n</user_command>",
						map)) //
				.observation("Execution just started") //
				.build();
		steps.add(step);
//		LOG.debug("Agent's prompt:\n\n" + getPersonality());
		LOG.debug(JsonSchema.JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(step));

		// execution loop
		while ((getLastStep() == null) || (getLastStep().status == null)) {

			// Update steps

			// Trigger execution further
			ChatCompletion reply = chat("Continue execution");

			// Check if agent generated a function call
			while (reply.hasToolCalls()) {

				List<ToolCallResult> results = new ArrayList<>();

				for (ToolCall call : reply.getToolCalls()) {

					// Execute call, handling errors nicely
					ToolCallResult result;
					try {
						result = call.execute();
					} catch (Exception e) {
						result = new ToolCallResult(call, e);
					}
					results.add(result);

					// Store the call and the results in steps
					@NonNull
					Map<String, Object> args = new HashMap<>(call.getArguments());
					Object thought = args.remove("thought"); // Should always be provided
					step = new ToolCallStep.Builder() //
							.actor(getId()) //
							.thought(thought == null ? "I decided to call this tool to progress execution"
									: thought.toString()) //
							.action("The tool \"" + call.getTool().getId() + "\" has been called") //
							.actionInput(JsonSchema.JSON_MAPPER.writeValueAsString(args)) //
							.actionSteps( // If the tool was another agent, store its steps too
									(call.getTool() instanceof ReactAgent) ? ((ReactAgent) call.getTool()).getSteps()
											: new ArrayList<>()) //
//							.actionInput(args.entrySet().stream() //
//									.map(e -> e.getKey() + "=" + e.getValue()) //
//									.collect(java.util.stream.Collectors.joining("\n"))) //
							.observation(result.getResult().toString()).build();

					steps.add(step);
					LOG.debug(JsonSchema.JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(step));
				}

				// Pass results back to the agent
				// Notice this might in principle generate
				// other tool calls, hence the loop
				reply = chat(new ChatMessage(results));

			} // while we serviced all calls

			// In case this is not a tool call (strange if it is not the last one), we store
			// it anyway
			try {
				step = reply.getObject(Step.class);
				step.actor = getId();
				steps.add(step);
				LOG.debug(JsonSchema.JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(step));
			} catch (JsonProcessingException e) { // Paranoid
				step = new Step.Builder() //
						.actor(getId()) //
						.thought("I stopped because I encountered this error: " + e.getMessage()) //
						.observation(reply.getText()) //
						.status(Step.Status.ERROR).build();
				steps.add(step);
				LOG.debug(JsonSchema.JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(step));
			}

		} // While the conversation is not completed

		return getLastStep();
	}
}
