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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.github.mzattera.predictivepowers.examples.react.ReactAgent.Step;
import io.github.mzattera.predictivepowers.examples.react.ReactAgent.Step.Status;
import io.github.mzattera.predictivepowers.examples.react.ReactAgent.ToolCallStep;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatService;
import io.github.mzattera.predictivepowers.services.Capability.ToolAddedEvent;
import io.github.mzattera.predictivepowers.services.Tool;
import io.github.mzattera.predictivepowers.services.Tool.ToolParameter;
import io.github.mzattera.predictivepowers.services.ToolInitializationException;
import io.github.mzattera.predictivepowers.services.Toolset;
import io.github.mzattera.predictivepowers.services.messages.ChatCompletion;
import io.github.mzattera.predictivepowers.services.messages.FinishReason;
import io.github.mzattera.predictivepowers.services.messages.JsonSchema;
import io.github.mzattera.predictivepowers.services.messages.ToolCall;
import io.github.mzattera.predictivepowers.services.messages.ToolCallResult;
import io.github.mzattera.predictivepowers.util.StringUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * This agent is an executor component, part of a {@link ReactAgent}; its task
 * is to execute user's commands using the tools at its disposal.
 */
public class ExecutorModule extends OpenAiChatService {

	private final static Logger LOG = LoggerFactory.getLogger(ExecutorModule.class);

	/**
	 * After this number of steps, we stop execution (to avoid loops).
	 */
	// TODO Urgent: make this configurable
	public final static int MAX_STEPS = 40; 


	private static final String PROMPT_TEMPLATE = "# Identity\n\n" //
			+ "You are a ReAct (Reasoning and Acting) agent; your task is to execute the below user command in <user_command> tag.\n"
			+ "\n<user_command>\n{{command}}\n</user_command>\n\n" //
			+ "You will be provided by the user with a potentially empty list of execution steps, in <steps> tag, that you have already performed in an attempt to execute the user's command. The format of these steps is provided as a JSON schema in <step_format> tag below.\n"
			+ "\n<step_format>\n" + JsonSchema.getJsonSchema(ToolCallStep.class) + "\n</step_format>\n\n" //
			+ "Together with the list of steps, the user might provide a suggestion about how to execute next step.\n"
			+ "\n# Additional Context and Information\n\n" //
			+ " * You are identified with actor=={{id}} in execution steps." //
			+ "{{context}}\n\n" //
			+ "\n# Instructions\n\n" //
			+ "  * Carefully plan the steps required to execute the user's command, think it step by step.\n"
			+ "  * If the user provided a suggestion about how to progress execution, then **STRICTLY** and **IMPORTANTLY** follow that suggestion when planning next step. "
			+ "Notice that the suggestion can ask you to proceed even if last step has status==\"COMPLETED\" or status==\"ERROR\"; if this is the case, you **MUST** **STRICTLY** follow the suggestion."
			+ " **IMPORTANTLY** notice the suggestion refers only to next execution step; you still need to continue execution after that, to fully execute user's command eventually.\n"
			+ "  * At each new step, use the most suitable tool at your disposal to progress towards executing the user's command. **STRICTLY** and **IMPORTANTLY** **NEVER** output a step to indicate a tool call, but call the tool directly.\n"
			+ "  * Your tools do not have access to steps in <steps>, therefore you must pass them all the parameters they require with their corresponding values. Be very detailed and specific each time you issue a tool call.\n"
			+ "  * When calling a tool, be specific on the task you want the tool to accomplish, do not mention why you are calling the tool and what your next steps will be.\n"
			+ "  * When planning the next step, carefully consider all of the steps already executed that are contained in <steps> tag. Carefully consider the thought that caused you to call each tool, usually provided as \"thought\" field in \"actionInput\" field, and observe the result of the call in \"observation\" field, before planning next step.\n"
			+ "  * **IMPORTANTLY** Never state in \"observation\" field that an action was performed, unless you called the proper tool to perform it, and it returned no errors."
			+ "  * When you are completely done done with executing the user's command and no further steps are needed, and only in that case, output one final step with status=\"COMPLETED\".\n"
			+ "  * **STRICTLY** and **IMPORTANTLY** **NEVER** output a step with status=\"COMPLETED\" if you think there are still actions to be performed; call the proper tool instead."
			+ "  * If you are experiencing an error, try to act differently and recover from it; if you are unable to recover, output one final step with status=\"ERROR\".\n"
			+ "  * **IMPORTANTLY**, when you output a final step with status=\"ERROR\", clearly and in detail describe in the \"observation\" field the reason of your failure. If the command lacked any necessary information, list missing information clearly and in detail. Suggest to the user any change or additions they could do to the command to help you to execute it.\n"
			+ "  * **IMPORTANTLY**, in all other cases, use status=\"IN_PROGRESS\", **STRICTLY** try to avoid this, rather use tool calls if you still have steps left to execute."
			+ "  * The format of the last step to output is described by the below JSON schema in <output_schema> tag; use this very format when outputting the final step.\n" //
			+ "\n<output_schema>\n" + JsonSchema.getJsonSchema(Step.class) + "\n</output_schema>\n" //
			+ "\n# Examples\n\n" //
			+ "Input & Context:\n\n" //
			+ "<user_command>Update J. Doe data with newest information.</user_command> and you realize data for J. Doe is already up-to-date.\n" //
			+ "\nCorrect Output:\n\n" //
			+ "		{\n" //
			+ "		  \"status\" : \"COMPLETED\",\n" //
			+ "		  \"actor\" : <your ID here>,\n" //
			+ "		  \"thought\" : \"The system record for J. Doe matches the provided data, no update is needed.\",\n" //
			+ "		  \"observation\" : \"No action needed, I have completed execution of the command.\",\n" //
			+ "		}\n" //
			+ "\nIncorrect Output:\n\n" //
			+ "<Issuing a tool call>\n" //
			+ "\n---\n\n" //
			+ "Input & Context:\n\n" //
			+ "You think the only remaining step in the process is to send an email to the customer.\n" //
			+ "\nCorrect Output:\n\n" //
			+ "<Issuing a tool call to send the email>\n" //
			+ "\nIncorrect Output:\n\n" //
			+ "		{\n" //
			+ "		  \"status\" : \"COMPLETED\",\n" //
			+ "		  \"actor\" : <your ID here>,\n" //
			+ "		  \"thought\" : \"All required steps in the process have been performed; The only remaining step is to send email to customer.\",\n" //
			+ "		  \"observation\" : \"All process steps completed. The only remaining action is to send an email.\"\n" //
			+ "		}\n" //
			+ "\n---\n\n" //
			+ "Input & Context:\n\n" //
			+ "<steps>[...<several steps before last one>\n" //
			+ "		{\n" //
			+ "		  \"status\" : \"COMPLETED\",\n" //
			+ "		  \"actor\" : <your ID here>,\n" //
			+ "		  \"thought\" : \"All steps up to this point have been completed as per the process. I only need to create the corresponding log entry.\",\n" //
			+ "		  \"observation\" : The process is complete up to the current stage.\"\n" //
			+ "		}]\n" //
			+ "</steps>\n" //
			+ "Suggestion: \"You must proceed with the next required steps: create corresponding log entry\","
			+ "\nCorrect Output:\n\n" //
			+ "<Issuing a tool call to create the log entry>\n" //
			+ "\nIncorrect Output:\n\n" //
			+ "		{\n" //
			+ "		  \"status\" : \"COMPLETED\",\n" //
			+ "		  \"actor\" : <your ID here>,\n" //
			+ "		  \"thought\" : \"I only need to create the corresponding log entry.\",\n" //
			+ "		  \"observation\" : The process is complete up to the current stage.\"\n" //
			+ "		}\n" //
			+ "\n---\n\n" //
			+ "Input & Context:\n\n" //
			+ "You think the process requires that you send an email to the user." //
			+ "\nCorrect Output:\n\n" //
			+ "<Issuing a tool call to send the email>\n" //
			+ "\nIncorrect Output:\n\n" //
			+ "{\n" //
			+ "	 \"status\" : \"IN_PROGRESS\",\n" //
			+ "  \"actor\" : <your ID here>,\n" //
			+ "  \"thought\" : \"The process requires that I must send an email to the user.\",\n" //
			+ "  \"observation\" : \"Proceeding to send an email to user.\"\n" //
			+ "}\n" //
			+ "\n---\n\n" //
			+ "Input & Context:\n\n" //
			+ "<steps>[...<several steps before last one>\n" //
			+ "{\n" //
			+ "	 \"status\" : \"IN_PROGRESS\",\n" //
			+ "  \"actor\" : <your ID here>,\n" //
			+ "  \"thought\" : \"The process requires that I must send an email to the user.\",\n" //
			+ "  \"observation\" : \"Proceeding to send an email to user.\"\n" //
			+ "}\n" //
			+ "</steps>\n" //
			+ "\nCorrect Output:\n\n" //
			+ "<Issuing a tool call to send the email>\n" //
			+ "\nIncorrect Output:\n\n" //
			+ "{\n" //
			+ "	 \"status\" : \"IN_PROGRESS\",\n" //
			+ "  \"actor\" : <your ID here>,\n" //
			+ "  \"thought\" : \"The process requires that I must send an email to the user.\",\n" //
			+ "  \"observation\" : \"Proceeding to send an email to user.\"\n" //
			+ "}\n" //
			+ "\n---\n\n" //		
			+ "Input & Context:\n\n" //
			+ "<steps>[{\n" //
			+ "  \"actor\" : <your ID here>,\n" //
			+ "  \"thought\" : \"I am starting execution of the below user's command in <user_command> tag.\\n\\n<user_command>\\nSend an email to J. Doe\\n</user_command>\",\n" //
			+ "  \"observation\" : \"Execution just started.\"\n" //
			+ "}]</steps>\n" //
			+ "\nCorrect Output:\n\n" //
			+ "<Issuing a tool call to send the email>\n" //
			+ "\nIncorrect Output:\n\n" + "{\n" //
			+ "  \"status\" : \"COMPLETED\",\n" //
			+ "  \"actor\" : <your ID here>,\n" //
			+ "  \"thought\" : \"The user's command is to send an email to J. Doe. The only required action is to send the email as instructed.\",\n" //
			+ "  \"observation\" : \"The email to J. Doe has been sent as requested.\"\n" //
			+ "}\n" //
			+ "\n---\n\n" //
			+ "Input & Context:\n\n" //
			+ "<user_command>Assign oldest task to operator 42.</user_command>\n" //
			+ "<steps>[...<several steps before last one>\n" //
			+ "		{\n" //
			+ "		  \"actor\" : <your ID here>,\n" //
			+ "  \"observation\" : \"OK, task assigned\",\n" //
			+ "  \"thought\" : \"I will assign task with ID 5656 (oldest task) to Operator ID 42 as requested.\",\n" //
			+ "  \"action\" : \"The tool \\\"assignTask\\\" has been called\",\n" //
			+ "  \"actionInput\" : \"{\\\"taskID\\\":\\\"5656\",\\\"operatorId\\\":\\\"42\\\"}\",\n" //
			+ "}]</steps>\n" //
			+ "\nCorrect Output:\n\n" //
			+ "{" //
			+ "  \"status\" : \"COMPLETED\",\n" //
			+ "  \"actor\" : <your ID here>,\n" //
			+ "  \"thought\" : \"The oldest task (ID=5656) has been assigned to Operator ID 42.\"\n" //
			+ "  \"outcome\" : \"The task with ID 5656 has been successfully assigned to Operator ID 42.\"\n" //
			+ "}" //
			+ "\nIncorrect Output:\n\n" //
			+ "{\n" //
			+ "  \"actor\" : <your ID here>,\n" //
			+ "  \"thought\" : \"I want to double-check that the task assignment is reflected in the current list of tasks for Operator ID 42, ensuring the process is complete and the correct task is now assigned.\",\n" //
			+ "  \"observation\" : \"List of tasks assigned to operator 42 = [5656]\",\n" //
			+ "  \"action\" : \"The tool \\\"getTasksForOperatot\\\" has been called\",\n" //
			+ "  \"actionInput\" : \"{\\\"operatorId\\\":\\\"42\\\"}\",\n" //
			+ "}\n" //
			+ "\n---\n\n" //
			+ "Input & Context:\n\n" //
			+ "You want to call \"getTasks\" tool.\n" //
			+ "\nCorrect Output:\n\n" //
			+ "{\n" //
			+ "  \"actor\" : <your ID here>,\n" //
			+ "  \"thought\" : \"I need to check if all tasks assigned to Operator with ID 90 are already closed. If not, I will write a reminder for the operator.\",\n" //
			+ "  \"observation\" : \"No open tasks are assigned to Operator ID 90 have been closed.\",\n" //
			+ "  \"action\" : \"The tool \\\"getTasks\\\" has been called\",\n" //
			+ "  \"actionInput\" : \"{\\\"question\\\":\\\"For all tasks assigned to Operator ID 90, list all that are still open.\\\"}\",\n" //
			+ "} \n" //
			+ "\nIncorrect Output:\n\n" //
			+ "{\n" //
			+ "  \"actor\" : <your ID here>,\n" //
			+ "  \"thought\" : \"I need to check if all tasks assigned to Operator with ID 90 are already closed. If not, I will write a reminder for the operator.\",\n" //
			+ "  \"observation\" : \"No open tasks are assigned to Operator ID 90 have been closed.\",\n" //
			+ "  \"action\" : \"The tool \\\"getTasks\\\" has been called\",\n" //
			+ "  \"actionInput\" : \"{\\\"question\\\":\\\"For all tasks assigned to Operator ID 90, list all that are still open. If any, I will send a reminder to the operator.\\\"}\",\n" //
			+ "} \n" //
			+ "\n---\n\n" //
			+ "Given the above examples, provide only the Correct Output for future inputs and context.\n" //
			+ "\n## Other Examples\n\n" //
			+ "{{examples}}\n";

	@Getter
	private final @NonNull ReactAgent agent;

	/**
	 * If true, it will call the reviewer on last step before exiting. We do this to
	 * save tokens.
	 */
	// TODO have a more elegant way of setting this?
	@Getter
	@Setter
	private boolean checkLastStep;

	// TODO move command and steps to the main agent ....everything accessible globally should go there
	
	/**
	 * Current command being executed.
	 */
	@Getter
	private String command;

	/**
	 * The list of steps executed so far while executing current command.
	 */
	// TODO URGENT better engineering of this ? maybe return them with execute
	// instead?
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
					"All tools available to ReactAgent instances must accept a \"thought\" parameter as a minimum.");

		super.onToolAdded(evt);
	}

	// TODO Make it a wrapper of another agent instead? A lot of code forwarding...

	ExecutorModule(@NonNull ReactAgent agent, @NonNull List<? extends Tool> tools, boolean checkLastStep)
			throws ToolInitializationException {

		super(agent.getId() + "-executor", agent.getEndpoint(), ReactAgent.DEFAULT_MODEL);
		this.agent = agent;
		this.checkLastStep = checkLastStep;
		addCapability(new Toolset(tools));
		if (!"o3".equals(ReactAgent.DEFAULT_MODEL)) {
			setTemperature(0d);
			// let's have one tool at a time if we can
			setDefaultRequest(getDefaultRequest().toBuilder().parallelToolCalls(false).build());
		}
		setResponseFormat(Step.class);

		// TODO Set max output (?)
	}

	public Step execute(@NonNull String command) throws JsonProcessingException {

		// TODO Need to clarify who does what....
		this.command = command;
		steps.clear();

		Map<String, String> map = new HashMap<>();
		map.put("command", command);
		map.put("id", getId());
		map.put("context", agent.getContext());
		map.put("examples", agent.getExamples());

		setPersonality(StringUtil.fillSlots(PROMPT_TEMPLATE, map));

		Step step = new Step.Builder() //
				.actor(getId()) //
				.status(Status.IN_PROGRESS) //
				.thought(StringUtil.fillSlots(
						"I am starting execution of the below user's command in <user_command> tag.\n\n<user_command>\n{{command}}\n</user_command>",
						map)) //
				.observation("Execution just started.") //
				.build();
		steps.add(step);
		LOG.debug(JsonSchema.JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(step));

		// execution loop
		String instructions = "<steps>\n{{steps}}\n</steps>\n\nSuggestion: {{suggestion}}";
		String suggestion = "No suggestions. Proceed as you see best, using the tools at your disposal.";
		while ((steps.size() < MAX_STEPS) && ((getLastStep() == null) || (getLastStep().status == null)
				|| (getLastStep().status == Status.IN_PROGRESS))) {

			clearConversation();
			map.put("steps", JsonSchema.JSON_MAPPER.writerWithView(Step.Views.Compact.class).writeValueAsString(steps));
			map.put("suggestion", suggestion);
			String message = StringUtil.fillSlots(instructions, map);
			ChatCompletion reply = null;
			Exception ex = null;
			try {
				reply = chat(message);
			} catch (Exception e) { // Exception calling the LLM
				ex = e;
				LOG.error(e.getMessage(), e);
			}

			if ((ex != null) || (reply.getFinishReason() != FinishReason.COMPLETED)) { // Something went wrogn calling the LLM
				step = new ToolCallStep.Builder() //
						.actor(getId()) //
						.status(Status.ERROR) //
						.thought("I had something in mind...") //
						.action("LLM was called but this resulted in "
								+ ((ex != null) ? "an error." : "a truncated message.")) //
						.actionInput(message) //
						.actionSteps(new ArrayList<>()) //
						.observation(
								(ex != null) ? ex.getMessage() : "Response finish reason: " + reply.getFinishReason()) //
						.build();
				steps.add(step);
				LOG.debug(JsonSchema.JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(step));
				break;
			}

			// Check if agent generated a function call
			if (reply.hasToolCalls()) { // Agent called a tool

				List<ToolCallResult> results = new ArrayList<>();
				boolean withError = false; // See below
				for (ToolCall call : reply.getToolCalls()) {

					// Execute each call, handling errors nicely
					ToolCallResult result;
					try {
						result = call.execute();
					} catch (Exception e) {
						result = new ToolCallResult(call, e);
						withError = true;
					}
					results.add(result);
					// TODO We should use a more generic way?
					withError |= result.getResult().toString().toLowerCase().contains("error");

					// Store the call and the results in steps
					Map<String, Object> args = new HashMap<>(call.getArguments());
					Object thought = args.remove("thought"); // Should always be provided
					step = new ToolCallStep.Builder() //
							.actor(getId()) //
							.status(Status.IN_PROGRESS) //
							.thought(thought == null ? "No thought passed explicitely."
									: thought.toString()) //
							.action("The tool \"" + call.getTool().getId() + "\" has been called") //
							.actionInput(JsonSchema.JSON_MAPPER.writeValueAsString(args)) //
							.actionSteps( // If the tool was another agent, store its steps too
									(call.getTool() instanceof ReactAgent) ? ((ReactAgent) call.getTool()).getSteps()
											: new ArrayList<>()) //
							.observation(result.getResult().toString()).build();

					steps.add(step);
					LOG.debug(JsonSchema.JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(step));

					if (steps.size() > MAX_STEPS)
						break;
				} // for each tool call, in case of parallel calls

				if (steps.size() <= MAX_STEPS) {
					// Trick to save time and tokens; maybe remove :)
					if (withError)
						suggestion = agent.getReviewer().reviewToolCall(steps);
					else
						suggestion = "CONTINUE";
				}
			} else { // Agent output something different than a tool call

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

				// Check the result
				if (getLastStep().status == Status.IN_PROGRESS) {
					suggestion = "**STRICTLY** proceed with next steps, by calling appropriate tools.";
				} else if (checkLastStep) { // Configurable, to decide in which component we check
					// Try to recover errors
					suggestion = agent.getReviewer().reviewConclusions(steps);
					if (!suggestion.toLowerCase().contains("continue")) {
						// forces the conversation to continue
//						steps.remove(getLastStep());
						getLastStep().status = Status.IN_PROGRESS;
//						getLastStep().status = null;
					}
				}
			}
		} // loop until the command is executed

		// If execution was interrupted, output a final error message
		if (steps.size() >= MAX_STEPS) {
			step = new Step.Builder() //
					.actor(getId()) //
					.thought("Execution was stopped because it exceeded maximum number of steps (" + MAX_STEPS + ").") //
					.observation("I probably entered some kind of loop.") //
					.status(Step.Status.ERROR).build();
			steps.add(step);
			LOG.error(JsonSchema.JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(step));
		}

		return getLastStep();
	}
}
