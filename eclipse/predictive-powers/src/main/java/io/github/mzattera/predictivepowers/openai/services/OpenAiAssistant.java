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
package io.github.mzattera.predictivepowers.openai.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mzattera.predictivepowers.examples.FunctionCallExample;
import io.github.mzattera.predictivepowers.openai.client.DataList;
import io.github.mzattera.predictivepowers.openai.client.SortOrder;
import io.github.mzattera.predictivepowers.openai.client.assistants.Assistant;
import io.github.mzattera.predictivepowers.openai.client.assistants.AssistantsRequest;
import io.github.mzattera.predictivepowers.openai.client.chat.OpenAiTool;
import io.github.mzattera.predictivepowers.openai.client.chat.OpenAiToolCall;
import io.github.mzattera.predictivepowers.openai.client.threads.Content;
import io.github.mzattera.predictivepowers.openai.client.threads.Content.Text.Annotation;
import io.github.mzattera.predictivepowers.openai.client.threads.Message;
import io.github.mzattera.predictivepowers.openai.client.threads.Message.Role;
import io.github.mzattera.predictivepowers.openai.client.threads.MessagesRequest;
import io.github.mzattera.predictivepowers.openai.client.threads.OpenAiThread;
import io.github.mzattera.predictivepowers.openai.client.threads.RequiredAction;
import io.github.mzattera.predictivepowers.openai.client.threads.Run;
import io.github.mzattera.predictivepowers.openai.client.threads.Run.Status;
import io.github.mzattera.predictivepowers.openai.client.threads.RunsRequest;
import io.github.mzattera.predictivepowers.openai.client.threads.ThreadsRequest;
import io.github.mzattera.predictivepowers.openai.client.threads.ToolOutput;
import io.github.mzattera.predictivepowers.openai.client.threads.ToolOutputsRequest;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.Agent;
import io.github.mzattera.predictivepowers.services.Capability;
import io.github.mzattera.predictivepowers.services.Tool;
import io.github.mzattera.predictivepowers.services.ToolInitializationException;
import io.github.mzattera.predictivepowers.services.Toolset;
import io.github.mzattera.predictivepowers.services.messages.ChatCompletion;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage.Author;
import io.github.mzattera.predictivepowers.services.messages.FilePart.ContentType;
import io.github.mzattera.predictivepowers.services.messages.FinishReason;
import io.github.mzattera.predictivepowers.services.messages.MessagePart;
import io.github.mzattera.predictivepowers.services.messages.TextPart;
import io.github.mzattera.predictivepowers.services.messages.ToolCall;
import io.github.mzattera.predictivepowers.services.messages.ToolCallResult;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;

/**
 * OpenAI Assistant.
 * 
 * This uses OpenAI assistants API to implement an {@link Agent}.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class OpenAiAssistant implements Agent {

	private final static Logger LOG = LoggerFactory.getLogger(OpenAiAssistant.class);

	private static final int SLEEP_TIME_MILLIS = 1000;

	@Getter
	private final OpenAiEndpoint endpoint;

	/**
	 * Assistant data on OpenAI servers.
	 */
	private Assistant openAiAssistant;

	// Current conversation thread
	@Getter(AccessLevel.PROTECTED)
	private OpenAiThread thread;

	// Current conversation run, this is used to return tool call results
	@Getter(AccessLevel.PROTECTED)
	private Run run;

	// Last message in current conversation run, this is used to return tool call
	// results
	@Getter(AccessLevel.PROTECTED)
	private Message usrMsg;

	@Override
	public String getModel() {
		synch();
		return openAiAssistant.getModel();
	}

	@Override
	public void setModel(@NonNull String model) {
		synch();
		openAiAssistant.setModel(model);
		update();
	}

	// Must do like this, as openAiAssistan might be non initialized sometimes.
	@Getter
	private final String id;

	@Override
	public String getName() {
		synch();
		return openAiAssistant.getName();
	}

	@Override
	public void setName(String name) {
		synch();
		openAiAssistant.setName(name);
		update();
	}

	@Override
	public String getDescription() {
		synch();
		return openAiAssistant.getDescription();
	}

	@Override
	public void setDescription(String description) {
		synch();
		openAiAssistant.setDescription(description);
		update();
	}

	@Override
	public String getPersonality() {
		synch();
		return openAiAssistant.getInstructions();
	}

	@Override
	public void setPersonality(String personality) {
		synch();
		openAiAssistant.setInstructions(personality);
		update();
	}

	// Conversation history for the current thread
	@Getter
	List<ChatMessage> history = new ArrayList<>();

	@Override
	public void clearConversation() {
		if (thread != null) {
			endpoint.getClient().deleteThread(thread.getId());
			thread = null;
			run = null;
			usrMsg = null;
			history.clear();
		}
	}

	@Override
	public ChatCompletion chat(String msg) {
		return chat(new ChatMessage(msg));
	}

	// TODO Runs give you option to override tools: maybe add a method that take a
	// list of tools as well

	@Override
	public ChatCompletion chat(ChatMessage msg) {

		// Get current conversation thread
		if (thread == null) {
			thread = endpoint.getClient().createThread(ThreadsRequest.builder().build());
			history.clear();
		}

		if ((run != null) && (run.getStatus() == Status.REQUIRES_ACTION)) {

			// Current run is waiting for tool call results, we must provide them
			if (!msg.hasToolCallResults())
				throw new IllegalArgumentException("Agent is waiting for tool call results");

			List<? extends ToolCallResult> results = msg.getToolCallResults();
			if (results.size() != msg.getParts().size())
				throw new IllegalArgumentException("Tool call results cannot contain other parts in the message.");

			ToolOutputsRequest req = new ToolOutputsRequest();
			for (ToolCallResult result : results)
				req.getToolOutputs().add(new ToolOutput(result));

			run = endpoint.getClient().submitToolOutputsToRun(thread.getId(), run.getId(), req);
			history.add(msg);
		} else {

			// Add message to thread
			try {
				usrMsg = endpoint.getClient().createMessage(thread.getId(), MessagesRequest.getInstance(msg, endpoint));
				history.add(msg);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			// Create new run for the message
			run = endpoint.getClient().createRun(thread.getId(), new RunsRequest(openAiAssistant.getId()));
		}

		// Wait for completion
		while (run.getStatus() == Status.QUEUED || run.getStatus() == Status.IN_PROGRESS) {

			// TODO URGENT Asynch method that returns at each run step?
			// maybe return run steps as results, instead of messages, so the results can be
			// progressed easiliy and caller always looks at ChatMessage?
			// Or have a onRunProgress() that returns the list of new run steps and
			// onMessageCompleted() that return a ChatCompletion

			try { // wait a bit
				Thread.sleep(SLEEP_TIME_MILLIS);
			} catch (InterruptedException e) {
			}

			// poll run status
			run = endpoint.getClient().retrieveRun(thread.getId(), run.getId());
		}

		switch (run.getStatus()) {

		case REQUIRES_ACTION:
			RequiredAction action = run.getRequiredAction();
			switch (action.getType()) {
			case SUBMIT_TOOL_OUTPUTS:
				// Assistant generated tool calls
				ChatMessage message = fromMessages(retrieveNewMessages(thread, usrMsg));
				message.getParts().addAll(fromToolCalls(action.getSubmitToolOutputs().getToolCalls()));
				history.add(message);
				return new ChatCompletion(FinishReason.COMPLETED, message);
			default:
				throw new IllegalArgumentException("Unsupported action type.");
			}

		case COMPLETED:
			ChatMessage message = fromMessages(retrieveNewMessages(thread, usrMsg));
			history.add(message);
			return new ChatCompletion(FinishReason.COMPLETED, message);

		case CANCELLING:
		case CANCELLED:
			message = fromMessages(retrieveNewMessages(thread, usrMsg));
			history.add(message);
			return new ChatCompletion(FinishReason.TRUNCATED, message);

		// TODO URGENT throw better exceptions and declare one which is not runtime?
		case FAILED:
			throw new RuntimeException(run.getLastError().getMessage());
		case EXPIRED:
			throw new RuntimeException("Run expired");

		default:
			throw new IllegalArgumentException("Unsupported run type");
		}
	}

	/**
	 * 
	 * @param thread
	 * @param last   Last message, this can be null to get all messages in the
	 *               thread.
	 * @return All messages added to the thread after last one.
	 */
	private List<Message> retrieveNewMessages(OpenAiThread thread, Message last) {
		List<Message> result = new ArrayList<>();

		String lastId = (last == null) ? null : last.getId();
		while (true) {
			DataList<Message> msgs = endpoint.getClient().listMessages(thread.getId(), SortOrder.ASCENDING, null,
					lastId, null);
			result.addAll(msgs.getData());
			if (!msgs.hasMore())
				break;
			lastId = msgs.getLastId();
		}

		return result;
	}

	// Cached tools
	@Getter(AccessLevel.PROTECTED)
	private Map<String, OpenAiTool> toolMap = new HashMap<>();

	@Getter(AccessLevel.PROTECTED)
	private Map<String, Capability> capabilityMap = new HashMap<>();

	// TODO URGENT add tests to check all the methods to manipulate tools

	@Override
	public List<String> getCapabilities() {
		return Collections.unmodifiableList(new ArrayList<>(capabilityMap.keySet()));
	}

	@Override
	public void addCapability(@NonNull Capability capability) throws ToolInitializationException {

		synch();

		// Dispose any existing version of the capability
		Capability old = capabilityMap.get(capability.getId());
		if (old != null)
			removeCapability(old.getId());
		capability.init(this);

		for (String toolId : capability.getToolIds()) {
			putTool(capability.getNewToolInstance(toolId));
		}

		capabilityMap.put(capability.getId(), capability);

		update();
	}

	@Override
	public void removeCapability(@NonNull String capabilityId) {
		synch();
		removeCapability(capabilityMap.get(capabilityId));
		update();
	}

	private void removeCapability(Capability capability) {
		if (capability == null)
			return;

		for (String toolId : capability.getToolIds())
			removeTool(toolId);
		try {
			capability.close();
		} catch (Exception e) {
		}

		capabilityMap.remove(capability.getId());
	}

	@Override
	public void clearCapabilities() {
		for (Capability capability : capabilityMap.values())
			removeCapability(capability);
		capabilityMap.clear();
	}

	private void putTool(@NonNull Tool tool) throws ToolInitializationException {

		// Tries if init goes
		tool.init(this);

		// Closes older version of this tool, if any
		removeTool(tool.getId());

		// Adds new one
		try {
			toolMap.put(tool.getId(), (OpenAiTool) tool);
		} catch (ClassCastException e) {
			// Wrap the Tool into an OpenAiTool, so we can use any tool with function calls
			toolMap.put(tool.getId(), new OpenAiTool(tool));
		}
	}

	private void removeTool(@NonNull String toolId) {
		OpenAiTool old = toolMap.remove(toolId);
		if (old != null) {
			try {
				old.close();
			} catch (Exception e) {
				LOG.warn("Error closing tool: {1}", e.getMessage());
			}
		}
	}

	/**
	 * Creates a new Assistant, on the OpenAi server side.
	 */
	public static OpenAiAssistant createAssistant(@NonNull OpenAiEndpoint endpoint) {
		return createAssistant(endpoint, //
				AssistantsRequest.builder() //
						.description("\"Default\" OpenAI Assistant") //
						.model("gpt-4-turbo-preview") //
						.name(UUID.randomUUID().toString()) //
						.instructions("You are an helpful assistant") //
						.build());
	}

	/**
	 * Creates a new Assistant, on the OpenAi server side.
	 */
	public static OpenAiAssistant createAssistant(@NonNull OpenAiEndpoint endpoint, @NonNull AssistantsRequest req) {
		return new OpenAiAssistant(endpoint, endpoint.getClient().createAssistant(req).getId());
	}

	OpenAiAssistant(@NonNull OpenAiEndpoint endpoint, @NonNull String assistantId) {
		this.endpoint = endpoint;
		this.id = assistantId;
		synch();
	}

	OpenAiAssistant(@NonNull OpenAiEndpoint endpoint, @NonNull String agentId, @NonNull String threadId) {
		this(endpoint, agentId);
		this.thread = endpoint.getClient().retrieveThread(threadId);
	}

	/**
	 * Synchronizes the configuration in this instance with latest data from server.
	 * 
	 * @throws ToolInitializationException
	 */
	private void synch() {

		// Get agent from server
		openAiAssistant = endpoint.getClient().retrieveAssistant(id);

		// Updated list of tools, in synch with agent
		Map<String, OpenAiTool> newTools = new HashMap<>();
		for (OpenAiTool tool : openAiAssistant.getTools()) {
			String id = tool.getId();
			OpenAiTool newTool = toolMap.get(id);
			if (newTool == null) // New tool added by some other user, for now use a proxy
				newTool = tool;
			newTools.put(id, newTool);
		}

		// Disposes tools that are no longer needed
		for (OpenAiTool oldTool : toolMap.values()) {
			if (!newTools.containsKey(oldTool.getId())) // tool removed by some other user
				try {
					oldTool.close();
				} catch (Exception e) {
					LOG.warn("Error closing tool: {1}", e.getMessage());
				}
		}

		// Synch map
		toolMap = newTools;
	}

	/**
	 * Updates assistant with the configuration in this instance.
	 */
	private void update() {

		// Updates assistant config with latest tools too (fetched configuration cannot
		// de-serialize tools properly
		openAiAssistant.setTools(new ArrayList<>(toolMap.values()));

		openAiAssistant = endpoint.getClient().modifyAssistant( //
				openAiAssistant.getId(), //
				AssistantsRequest.getInstance(openAiAssistant));
	}

	/**
	 * Translates a list of Messages created in a run into a ChatMessage we can
	 * return.
	 * 
	 * @param newMessages
	 * @return
	 */
	private @NonNull ChatMessage fromMessages(List<Message> messages) {
		List<MessagePart> parts = new ArrayList<>();

		for (Message msg : messages) {
			if (msg.getRole() != Role.ASSISTANT)
				throw new IllegalArgumentException();

			for (Content content : msg.getContent()) {

				switch (content.getType()) {
				case IMAGE_FILE:
					parts.add(new OpenAiFilePart(ContentType.IMAGE, content.getImageFile().get("file_id"), endpoint));
					break;
				case TEXT:
					StringBuilder sb = new StringBuilder(content.getText().getValue());
					List<OpenAiFilePart> files = new ArrayList<>();

					List<Annotation> notes = content.getText().getAnnotations();
					if ((notes != null) && (notes.size() > 0)) {

						// TODO handle annotations as separate parts?

						sb.append("\n\n============");
						for (Annotation a : content.getText().getAnnotations()) {

							sb.append("\n\n[").append(a.getText()).append("]");
							if (a.getFileCitation() != null)
								sb.append(" - File: [").append(a.getFileCitation().getFileId()).append("]");
							sb.append("\n\n").append(a.getFileCitation().getQuote());
							files.add(new OpenAiFilePart(ContentType.GENERIC, a.getFilePath().getFileId(), endpoint));
							if (a.getFilePath() != null) {
								sb.append(" - File: [").append(a.getFilePath().getFileId()).append("]");
								files.add(
										new OpenAiFilePart(ContentType.GENERIC, a.getFilePath().getFileId(), endpoint));
							}
							sb.append("\n\n------------");
						}
					}

					parts.add(new TextPart(sb.toString()));
					parts.addAll(files);
					break;
				default:
					throw new IllegalArgumentException();
				}
			}
		}

		return new ChatMessage(Author.BOT, parts);
	}

	/**
	 * Translates a list of tool calls into message parts.
	 */
	private List<ToolCall> fromToolCalls(@NonNull List<OpenAiToolCall> toolCalls) {
		List<ToolCall> calls = new ArrayList<>();
		for (OpenAiToolCall call : toolCalls) {
			ToolCall toolCall = ToolCall.builder() //
					.id(call.getId()) //
					.tool(toolMap.get(call.getFunction().getName())) //
					.arguments(call.getFunction().getArguments()) //
					.build();
			calls.add(toolCall);
		}
		return calls;
	}

	public static void main(String[] args) throws ToolInitializationException {
		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {

			OpenAiAssistant bot = OpenAiAssistant.createAssistant(ep);
			Toolset tools = new Toolset();
			tools.putTool("getCurrentWeather", FunctionCallExample.GetCurrentWeatherTool.class);
			bot.addCapability(tools);

			// Conversation loop
			try (Scanner console = new Scanner(System.in)) {
				while (true) {
					System.out.print("User     > ");
					String s = console.nextLine();

					if ("history".equals(s)) {
						for (ChatMessage msg:bot.getHistory())
							System.out.println("HISTORY " + " > " + msg.getAuthor() + ": " +msg.getContent());
						continue;
					}
					
					ChatCompletion reply = bot.chat(s);

					// Check if bot generated a function call
					while (reply.hasToolCalls()) {

						List<ToolCallResult> results = new ArrayList<>();

						for (ToolCall call : reply.getToolCalls()) {
							// The bot generated tool calls, print them
							System.out.println("CALL " + " > " + call);

							// Execute calls and handle errors nicely
							ToolCallResult result;
							try {
								result = call.getTool().invoke(call);
							} catch (Exception e) {
								result = new ToolCallResult(call, "Error: " + e.getMessage());
							}
							results.add(result);
						}

						// Pass results back to the bot
						// Notice this can generate other tool calls, hence the loop
						reply = bot.chat(new ChatMessage(results));
					}

					System.out.println("Assistant> " + reply.getText());
				}
			}
		}
	}
}