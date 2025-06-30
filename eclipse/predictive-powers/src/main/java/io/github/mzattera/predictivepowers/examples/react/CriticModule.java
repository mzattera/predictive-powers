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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.github.mzattera.predictivepowers.examples.react.ReactAgent.Step;
import io.github.mzattera.predictivepowers.examples.react.ReactAgent.ToolCallStep;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatService;
import io.github.mzattera.predictivepowers.services.CompletionService;
import io.github.mzattera.predictivepowers.services.Tool;
import io.github.mzattera.predictivepowers.services.Tool.ToolParameter;
import io.github.mzattera.predictivepowers.services.ToolInitializationException;
import io.github.mzattera.predictivepowers.services.messages.JsonSchema;
import lombok.Getter;
import lombok.NonNull;

/**
 * This agent is a reviewer component, part of a {@link ReactAgent}; its task is
 * to review the performances of the executor component and provide suggestions
 * for it to correct its behavior.
 */
class CriticModule extends OpenAiChatService {

	private final static Logger LOG = LoggerFactory.getLogger(CriticModule.class);

	@Getter
	private final @NonNull ReactAgent agent;

	// We keep them here because we do not want the reviewer to actually call any
	// tool.
	private final @NonNull List<Tool> tools;

	public static final String PROMPT_TEMPLATE = "# Identity\n\n" //
			+ "You are a reviewer agent; your task is to monitor how an executor agent tries to execute user's commands and provide suggestion to improve execution.\n" //
			+ "The specific user's command the executor is trying to execute is provided in the below <user_command> tag.\n" //
			+ "\n<user_command>\n{{command}}\n</user_command>\n\n" //
			+ "You will be provided by the user with a potentially empty list of execution steps, in <steps> tag, that have been already performed by the executor in its attempt to execute the user's command. The format of these steps is provided as a JSON schema in <step_format> tag below. In these steps, the executor agent is identified with actor==\"{{executor_id}}\".\n"
			+ "\n<step_format>\n" + JsonSchema.getJsonSchema(ToolCallStep.class) + "\n</step_format>\n\n" //
			+ "\n# Additional Context and Information\n\n" //
			+ "  * In order to execute the command, the executor agent has the tools described in the below <tools> tag at its disposal:\n\n"
			+ "<tools>\n{{tools}}\n</tools>\n\n" //
			+ "{{context}}\n";

	public static final String REVIEW_TOOL_CALL_TEMPLATE = PROMPT_TEMPLATE //
			+ "\n# Instructions\n\n" //
			+ "  * If the steps contain evidence that the executor entered a loop calling same tool repeatedly with same parameters, provide a suggestion to strictly call another tool to execute next step.\n"
			+ "  * If and only if last execution step contains a tool call resulting in an error, inspect the tool definition and check for any missing or unsupported parameter in last call; try to understand if missing parameters values are already present in some execution step \"observation\" field. Suggest to the executor to repeat the call, listing the values you find for missed parameters and flagging the unsupported parameters.\n"
			+ "  * **IMPORTANTLY**, in all other case, or if you do not have any relevant suggestion about how to change the executor behavior, just output \"CONTINUE\"."
			+ "  * **IMPORTANTLY** your output is either a suggestion or \"CONTINUE\", do not add any comment if you emit \"CONTINUE\" and do not use \"CONTINUE\" if you have a suggestion.";

	public static final String REVIEW_CONCLUSIONS_TEMPLATE = PROMPT_TEMPLATE //
			+ "\n# Instructions\n\n" //
			+ "  * If and only if last execution step has status=\"ERROR\", it means the executor is aborting execution because it encountered an error it cannot resolve. In this case, examine carefully the execution steps, try to determine what went wrong; if you are able to identify a possible error source and a remediation approach, output a suggestion detailing how the error can be avoided. For example, if the executor is trying to use the wrong tool to perform a task, suggest the proper tool to use.\n"
			+ "  * If and only if last execution step has status=\"COMPLETED\", check it carefully for indication in \"observation\" or \"thought\" fields that the executor has some more steps to perform to complete the user's command. "
			+ "For example, executor might mention it needs to proceed with next steps, or list tasks it still needs to perform. If this is the case, output a suggestion to proceed with execution of these steps.\n"
			+ "  * **IMPORTANTLY**, in all other cases, or if you do not have any relevant suggestion about how to change the executor behavior, just output \"CONTINUE\"."
			+ "  * The only evidence that the executor called a tool or performed a specific task is in the \"action\" field of any step; " //
			+ "do not trust the executor when it says in \"though\" or \"observation\" that an action has been performed, unless that step also contains an \"action\" field mentioning a relevant tool."
			+ "  * **IMPORTANTLY** your output is either a suggestion or \"CONTINUE\", do not add any comment if you emit \"CONTINUE\".\n"
			+ "\n# Examples\n\n" //
			+ "Input:\n\n" //
			+ "<steps>[...<several steps before last one>\n" //
			+ "{\n" //
			+ "  \"status\" : \"COMPLETED\",\n" //
			+ "  \"actor\" : <Executor ID here>,\n" //
			+ "  \"thought\" : \"The next required step is to send an email to the requester with required details. All other process steps have been completed, so I will now send the email and complete execution.\",\n" //
			+ "  \"observation\" : \"Email sent to requester with required details. All required steps in the process have been performed.\"\n" //
			+ "}]\n" //
			+ "</steps>\n" //
			+ "\nCorrect Output:\n\n" //
			+ "There is no evidence you sent the email as needed, no action was performed; call the proper tool to send the email.\n" //
			+ "\nIncorrect Output:\n\n" //
			+ "CONTINUE\n" //
			+ "\n---\n\n" //
			+ "Input:\n\n" //
			+ "<steps>[...<several steps before last one>\n" //
			+ "{\n" //
			+ "  \"status\" : \"COMPLETED\",\n" //
			+ "  \"actor\" : <Executor ID here>,\n" //
			+ "  \"thought\" : \"The only remaining step is to close the task. I will now call the tool to close the task.\",\n" //
			+ "  \"observation\" : \"Task with ID 123 has been successfully closed. All required steps in the process have been performed and the user's command is fully executed.\"\n" //
			+ "}\n" //
			+ "}]\n" //
			+ "</steps>\n" //
			+ "\nCorrect Output:\n\n" //
			+ "There is no evidence you closed the task as needed, no action was performed; call the proper tool to close the task.\n" //
			+ "\nIncorrect Output:\n\n" //
			+ "CONTINUE\n" //
			+ "\n---\n\n" //
			+ "Input:\n\n" //
			+ "<steps>[...<several steps before...>\n" //
			+ "{\n" //
			+ "  \"actor\" : <Executor ID here>,\n" //
			+ "  \"thought\" : \"I need to retrieve J. Doe email address and other data.\",\n" //
			+ "  \"observation\" : \"Data about client J. Doe:\\n email:joe@doe.com\\n customer number: 4567\\n\",\n" //
			+ "  \"action\" : \"The tool \\\"getCustomerData\\\" has been called\",\n" //
			+ "  \"actionInput\" : \"{\\\"question\\\":\\\"Retrieve data for customer J. Doe.\\\"}\",\n" //
			+ "},\n" //
			+ "{\n" //
			+ "  \"actor\" : <Executor ID here>,\n" //
			+ "  \"thought\" : \"The next step is to send an email to J. Doe.\",\n" //
			+ "  \"observation\" : \"ERROR: Missing required information: J. Doe's unique customer number.\",\n" //
			+ "  \"action\" : \"The tool \\\"sendEmail\\\" has been called\",\n" //
			+ "  \"actionInput\" : \"{\\\"question\\\":\\\"Send an email to J. Doe (joe@doe.com) with the following content: \\\"Order received.\\\"\\\"}\",\n" //
			+ "}]\n" //
			+ "</steps>\n" //
			+ "\nCorrect Output:\n\n" //
			+"The sendEmail tool requires a customer number; use the customer number 4567 as indicted in previous steps.\n" //
			+ "\nIncorrect Output:\n\n" //
			+"The sendEmail tool requires a customer number. If you have J. Doe's customer number from previous steps, use that value.\n" //
			+ "\n---\n\n" //
			+ "Given the above examples, provide only the Correct Output for future inputs.\n"; //

	// TODO Make it a wrapper of another agent instead? A lot of code forwarding...

	CriticModule(@NonNull ReactAgent agent, @NonNull List<? extends Tool> tools)
			throws ToolInitializationException {

		super(agent.getId() + "-reviewer", agent.getEndpoint(), ReactAgent.DEFAULT_MODEL);
		this.agent = agent;
		this.tools = new ArrayList<>(tools);
		setTemperature(0d);
	}

	public String reviewToolCall(@NonNull List<? extends Step> steps) throws JsonProcessingException {
		return review(REVIEW_TOOL_CALL_TEMPLATE, steps);
//		return "CONTINUE";
	}

	public String reviewConclusions(@NonNull List<? extends Step> steps) throws JsonProcessingException {
		return review(REVIEW_CONCLUSIONS_TEMPLATE, steps);
//		return "CONTINUE";
	}

	public String review(String template, @NonNull List<? extends Step> steps) throws JsonProcessingException {

		Map<String, String> map = new HashMap<>();
		map.put("command", agent.getExecutor().getCommand());
		map.put("executor_id", agent.getExecutor().getId());
		map.put("context", agent.getContext());
		map.put("tools", buildToolDescription(tools));
		map.put("steps", JsonSchema.JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(steps));
		setPersonality(CompletionService.fillSlots(template, map));
//		LOG.info(getPersonality());

		clearConversation();
		String suggestion = chat(CompletionService.fillSlots("<steps>\\n{{steps}}\\n</steps>", map)).getText();
		System.err.println("    **** " + suggestion);
		LOG.debug("    **** Suggestion: " + suggestion);
		return chat(CompletionService.fillSlots("<steps>\\n{{steps}}\\n</steps>", map)).getText();
	}

	private static String buildToolDescription(@NonNull List<Tool> tools) {
		StringBuilder sb = new StringBuilder();
		for (Tool t : tools) {
			sb.append("## Tool\n\n");
			sb.append("### Tool ID: ").append(t.getId()).append("\n");
			sb.append("### Tool Description\n").append(t.getDescription()).append("\n");

			// Remove "thougth" from list of paramters
			List<ToolParameter> params = new ArrayList<>();
			for (ToolParameter p : t.getParameters()) {
				if (!"thought".equals(p.getName()))
					params.add(p);
			}
			sb.append("### Tool Paramters (as JSON schema\n").append(JsonSchema.getJsonSchema(params)).append("\n\n");
		}
		return sb.toString();
	}

}
