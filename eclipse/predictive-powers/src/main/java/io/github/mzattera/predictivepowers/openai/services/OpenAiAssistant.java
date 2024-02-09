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
import java.util.Collection;
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
import io.github.mzattera.predictivepowers.openai.client.threads.Content;
import io.github.mzattera.predictivepowers.openai.client.threads.Content.Text.Annotation;
import io.github.mzattera.predictivepowers.openai.client.threads.Message;
import io.github.mzattera.predictivepowers.openai.client.threads.Message.Role;
import io.github.mzattera.predictivepowers.openai.client.threads.MessagesRequest;
import io.github.mzattera.predictivepowers.openai.client.threads.OpenAiThread;
import io.github.mzattera.predictivepowers.openai.client.threads.Run;
import io.github.mzattera.predictivepowers.openai.client.threads.Run.Status;
import io.github.mzattera.predictivepowers.openai.client.threads.RunsRequest;
import io.github.mzattera.predictivepowers.openai.client.threads.ThreadsRequest;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.Agent;
import io.github.mzattera.predictivepowers.services.Tool;
import io.github.mzattera.predictivepowers.services.ToolInitializationException;
import io.github.mzattera.predictivepowers.services.ToolProvider;
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
import lombok.Setter;

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

	private OpenAiThread thread;

	@Override
	public String getModel() {
		synch();
		return openAiAssistant.getModel();
	}

	@Override
	public void setModel(@NonNull String model) {
		openAiAssistant.setModel(model);
		update();
	}

	// Must do like this, as openAiAssistan might be non initialized sometimes.
	@Getter
	private final String id;

	@Override
	public String getName() {
		return openAiAssistant.getName();
	}

	@Override
	public void setName(String name) {
		openAiAssistant.setName(name);
		update();
	}

	@Override
	public String getDescription() {
		return openAiAssistant.getDescription();
	}

	@Override
	public void setDescription(String description) {
		openAiAssistant.setDescription(description);
		update();
	}

	@Override
	public String getPersonality() {
		return openAiAssistant.getInstructions();
	}

	@Override
	public void setPersonality(String personality) {
		openAiAssistant.setInstructions(personality);
		update();
	}

	@Override
	public void clearConversation() {
		// TODO URGENT implement
		throw new UnsupportedOperationException();
	}

	@Override
	public ChatCompletion chat(String msg) {
		return chat(new ChatMessage(msg));
	}

	@Override
	public ChatCompletion chat(ChatMessage msg) {

		// Get current conversation thread
		if (thread == null)
			thread = endpoint.getClient().createThread(ThreadsRequest.builder().build());

		// TODO URGENT if the message has tool call results, handle them here

		// Add message to thread
		Message usrMsg = null;
		try {
			usrMsg = endpoint.getClient().createMessage(thread.getId(), MessagesRequest.getInstance(msg, endpoint));
		} catch (IOException e) {
			// TODO URGENT fix
			e.printStackTrace();
		}

		// Create run
		Run run = endpoint.getClient().createRun(thread.getId(), new RunsRequest(openAiAssistant.getId()));

		// Wait for completion
		while (run.getStatus() == Status.QUEUED || run.getStatus() == Status.IN_PROGRESS) {
			try { // wait a bit
				Thread.sleep(SLEEP_TIME_MILLIS);
			} catch (InterruptedException e) {
			}

			// poll run status
			run = endpoint.getClient().retrieveRun(thread.getId(), run.getId());
		}

		switch (run.getStatus()) {
		case REQUIRES_ACTION: // TODO URGENT Add support for this
		case COMPLETED:
			return new ChatCompletion(FinishReason.COMPLETED, fromMessages(retrieveNewMessages(thread, usrMsg)));

		case CANCELLING:
		case CANCELLED:
			return new ChatCompletion(FinishReason.TRUNCATED, fromMessages(retrieveNewMessages(thread, usrMsg)));

		case FAILED:
		case EXPIRED:
			// TODO URGENT throw better exceptions and declare one which is not runtime?
			throw new RuntimeException("An error happened while generating the message.");

		default:
			throw new IllegalArgumentException();
		}
	}

	/**
	 * 
	 * @param thread
	 * @param last
	 * @return All messages added to a thread after last one.
	 */
	private List<Message> retrieveNewMessages(OpenAiThread thread, Message last) {
		List<Message> result = new ArrayList<>();

		String lastId = last.getId();
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

	@Setter
	@Getter(AccessLevel.PROTECTED)
	private ToolProvider toolProvider;

	// Cached tools
	@Getter(AccessLevel.PROTECTED)
	private Map<String, OpenAiTool> toolMap = new HashMap<>();

	@Override
	public List<String> getTools() {
		return Collections.unmodifiableList(new ArrayList<>(toolMap.keySet()));
	}

	@Override
	public void setTools(@NonNull Collection<String> list) throws ToolInitializationException {

		synch();
		for (String id : new ArrayList<>(toolMap.keySet()))
			removeTool(id, false);
		for (String id : list)
			addTool(id, false);
		update();
	}

	@Override
	public void addTool(@NonNull String toolId) throws ToolInitializationException {
		addTool(toolId, true);
	}

	public void addTool(@NonNull String toolId, boolean synch) throws ToolInitializationException {

		if (synch)
			synch();
		OpenAiTool tool = newToolInstance(toolId);

		OpenAiTool old = toolMap.remove(toolId);
		if (old != null) {
			try {
				old.close();
			} catch (Exception e) {
				LOG.warn("Error closing tool: {1}", e.getMessage());
			}
		}

		toolMap.put(tool.getId(), tool);
		if (synch)
			update();
	}

	@Override
	public void addTools(@NonNull Collection<String> tools) throws ToolInitializationException {
		synch();
		for (String id : tools)
			addTool(id, false);
		update();
	}

	@Override
	public void removeTool(@NonNull String id) {
		removeTool(id, true);
	}

	public void removeTool(@NonNull String id, boolean synch) {
		if (synch)
			synch();

		OpenAiTool old = toolMap.remove(id);
		if (old != null) {
			try {
				old.close();
			} catch (Exception e) {
				LOG.warn("Error closing tool: {1}", e.getMessage());
			}

			if (synch)
				update();
		}
	}

	@Override
	public void clearTools() {
		synch();
		for (String id : new ArrayList<>(toolMap.keySet()))
			removeTool(id, false);
		update();
	}

	/**
	 * Gets a new, initialized, instance of a tool from the tool provider.
	 * 
	 * @throws ToolInitializationException
	 */
	private OpenAiTool newToolInstance(String id) throws ToolInitializationException {
		if (toolProvider == null)
			throw new ToolInitializationException("A tool provider for the agent must be provided.");

		Tool tool = toolProvider.getTool(id);
		if (tool == null)
			throw new ToolInitializationException("Missing tool: " + id);

		OpenAiTool result;
		try {
			result = (OpenAiTool) tool;
		} catch (ClassCastException e) {
			result = new OpenAiTool(tool); // wrapper around another tool
		}

		result.init(this);
		return result;
	}

	/**
	 * Creates a new Assistant, on the OpenAi server side.
	 * 
	 * @throws ToolInitializationException
	 */
	public static OpenAiAssistant createAssistant(@NonNull OpenAiEndpoint endpoint, ToolProvider provider)
			throws ToolInitializationException {
		return createAssistant(endpoint, //
				AssistantsRequest.builder() //
						.description("\"Default\" OpenAI Assistant") //
						.model("gpt-4-turbo-preview") //
						.name(UUID.randomUUID().toString()) //
						.build(), //
				provider);
	}

	/**
	 * Creates a new Assistant, on the OpenAi server side.
	 * 
	 * @throws ToolInitializationException
	 */
	public static OpenAiAssistant createAssistant(@NonNull OpenAiEndpoint endpoint, @NonNull AssistantsRequest req,
			ToolProvider provider) throws ToolInitializationException {
		return new OpenAiAssistant(endpoint, endpoint.getClient().createAssistant(req).getId(), provider);
	}

	OpenAiAssistant(@NonNull OpenAiEndpoint endpoint, @NonNull String assistantId, ToolProvider provider)
			throws ToolInitializationException {
		this.endpoint = endpoint;
		this.toolProvider = provider;
		this.id = assistantId;
		synch();
	}

	OpenAiAssistant(@NonNull OpenAiEndpoint endpoint, @NonNull String agentId, @NonNull String threadId,
			ToolProvider provider) throws ToolInitializationException {
		this(endpoint, agentId, provider);
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
			if (newTool == null) // New tool added by some other user, must get an instance
				try {
					newTool = newToolInstance(id);
				} catch (ToolInitializationException e) {
					// Use existing proxy, even if it cannot be executed
					LOG.error("Error initializing tool " + id + ": " + e.getMessage());
					newTool = tool;
				}
			newTools.put(id, tool);
		}

		// Disposes tools that are no longer needed
		for (OpenAiTool oldTool : toolMap.values()) {
			if (!newTools.containsKey(oldTool.getId())) // tool no longer needed
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

	public static void main(String[] args) throws ToolInitializationException {
		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {

			OpenAiAssistant bot = OpenAiAssistant.createAssistant(ep, FunctionCallExample.PROVIDER);
			bot.addTools(FunctionCallExample.PROVIDER.getToolIds());

			bot.setPersonality("You are a helpful assistant that knows how to use function calls.");

			// Conversation loop
			try (Scanner console = new Scanner(System.in)) {
				while (true) {
					System.out.print("User     > ");
					String s = console.nextLine();

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