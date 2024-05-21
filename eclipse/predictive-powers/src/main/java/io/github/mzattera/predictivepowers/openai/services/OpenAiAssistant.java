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
package io.github.mzattera.predictivepowers.openai.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mzattera.predictivepowers.openai.client.AzureOpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.DataList;
import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;
import io.github.mzattera.predictivepowers.openai.client.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.SortOrder;
import io.github.mzattera.predictivepowers.openai.client.assistants.Assistant;
import io.github.mzattera.predictivepowers.openai.client.assistants.AssistantsRequest;
import io.github.mzattera.predictivepowers.openai.client.chat.OpenAiTool;
import io.github.mzattera.predictivepowers.openai.client.chat.OpenAiToolCall;
import io.github.mzattera.predictivepowers.openai.client.files.File;
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
import io.github.mzattera.predictivepowers.services.AbstractAgent;
import io.github.mzattera.predictivepowers.services.Agent;
import io.github.mzattera.predictivepowers.services.Capability;
import io.github.mzattera.predictivepowers.services.Tool;
import io.github.mzattera.predictivepowers.services.ToolInitializationException;
import io.github.mzattera.predictivepowers.services.messages.ChatCompletion;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage.Author;
import io.github.mzattera.predictivepowers.services.messages.FilePart;
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
public class OpenAiAssistant extends AbstractAgent {

	private final static Logger LOG = LoggerFactory.getLogger(OpenAiAssistant.class);

	private static final int SLEEP_TIME_MILLIS = 1000;

	@Getter
	private final OpenAiAgentService service;

	protected OpenAiEndpoint getEndpoint() {
		return service.getEndpoint();
	}

	protected OpenAiClient getClient() {
		return (OpenAiClient) service.getEndpoint().getClient();
	}

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

	public void setModel(@NonNull String model) {
		synch();
		openAiAssistant.setModel(model);
		register();
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

	public Map<String, String> getMetadata() {
		synch();
		return openAiAssistant.getMetadata();
	}

	public void setMetadata(Map<String, String> metadata) {
		synch();
		openAiAssistant.setMetadata(metadata);
		update();
	}

	// Conversation history for the current thread
	@Getter
	List<ChatMessage> history = new ArrayList<>();

	@Override
	public void clearConversation() {

		if (thread != null) {
			// Currently, there is no way to cleanup threads when an agent is deleted. Since
			// we are not managing conversations at this stage, we simply delete the old
			// thread.
			try {
				getClient().deleteThread(thread.getId());
			} catch (Exception e) {
				LOG.warn("Error deleting conversation thread", e);
			}
		}
		thread = null;
		run = null;
		usrMsg = null;
		history.clear();
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
			thread = getClient().createThread(ThreadsRequest.builder().build());
			history.clear();
			run = null;
			usrMsg = null;
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

			run = getClient().submitToolOutputsToRun(thread.getId(), run.getId(), req);
		} else {

			// Add message to thread
			try {
				usrMsg = getClient().createMessage(thread.getId(), MessagesRequest.getInstance(msg, getEndpoint()));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			// Create new run for the message
			run = getClient().createRun(thread.getId(), new RunsRequest(openAiAssistant.getId()));
		}

		history.add(msg);

		// Wait for completion
		while (run.getStatus() == Status.QUEUED || run.getStatus() == Status.IN_PROGRESS) {

			// TODO Add asynch method that returns at each run step?
			// maybe return run steps as results, instead of messages, so the results can be
			// progressed easily and caller always looks at ChatMessage?
			// Or have a onRunProgress() that returns the list of new run steps and
			// onMessageCompleted() that return a ChatCompletion

			try { // wait a bit
				Thread.sleep(SLEEP_TIME_MILLIS);
			} catch (InterruptedException e) {
			}

			// poll run status
			run = getClient().retrieveRun(thread.getId(), run.getId());
		}

		switch (run.getStatus()) {

		case REQUIRES_ACTION:
			RequiredAction action = run.getRequiredAction();
			switch (action.getType()) {
			case SUBMIT_TOOL_OUTPUTS:
				// The assistant generated tool calls
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
			DataList<Message> msgs = getClient().listMessages(thread.getId(), SortOrder.ASCENDING, null, lastId, null);
			result.addAll(msgs.getData());
			if (!msgs.hasMore())
				break;
			lastId = msgs.getLastId();
		}

		return result;
	}

	@Override
	public void addCapability(@NonNull Capability capability) throws ToolInitializationException {

		synch();
		super.addCapability(capability);
		update();
	}

	@Override
	public void removeCapability(@NonNull String capabilityId) {
		synch();
		super.removeCapability(capabilityId);
		update();
	}

	@Override
	protected void putTool(@NonNull Tool tool) throws ToolInitializationException {
		// Notice we store only instances of OpenAiTool
		OpenAiTool t = (tool instanceof OpenAiTool) ? (OpenAiTool) tool : new OpenAiTool(tool);
		super.putTool(t);
	}

	/**
	 * 
	 * @return List of files attached to this agent.
	 */
	public List<? extends FilePart> listFiles() {

		List<OpenAiFilePart> result = new ArrayList<>();
		String last = null;
		while (true) {
			DataList<File> search = getClient().listAssistantFiles(id, SortOrder.ASCENDING, null, null, last);
			for (File f : search.getData())
				result.add(new OpenAiFilePart(f.getId()));

			if (!search.hasMore())
				break;
			last = search.getLastId();
		}
		return result;
	}

	/**
	 * Uploads a file and attach it to this agent, to make it available to tools
	 * like retrieval.
	 * 
	 * @param file File to attach to the agent. If it is not an
	 *             {@link OpenAiFilePart} already, the file is uploaded to OpenAI
	 *             first.
	 * 
	 * @return A representation of the uploaded file.
	 * @throws IOException If an error occurs reading the file.
	 */
	public FilePart addFile(FilePart file) throws IOException {

		File oaiFile = null;
		if (file instanceof OpenAiFilePart) {
			oaiFile = getClient().createAssistantFile(id, ((OpenAiFilePart) file).getFileId());
		} else {
			oaiFile = getClient().uploadFile(file, "assistants");
			oaiFile = getClient().createAssistantFile(id, oaiFile.getId());
		}

		return new OpenAiFilePart(file.getContentType(), oaiFile);
	}

	/**
	 * Removes a file that was attached to the agent.
	 * 
	 * @param file File to remove.
	 * @return True if the file was successfully removed, false otherwise.
	 */
	public boolean removeFile(FilePart file) throws IOException {

		if (file instanceof OpenAiFilePart) {
			return removeFile(((OpenAiFilePart) file).getFileId());
		} else {
			return false;
		}
	}

	/**
	 * Removes a file that was attached to the agent.
	 * 
	 * @param fileId ID of the file to remove.
	 * @return True if the file was successfully removed, false otherwise.
	 */
	public boolean removeFile(String fileId) throws IOException {

		return getClient().deteAssistantFile(id, fileId).isDeleted();
	}

	@Override
	public void close() {
		super.close();
		if (thread != null) {
			// Currently, there is no way to cleanup threads when an agent is deleted. Since
			// we are not managing conversations at this stage, we simply delete the old
			// thread.
			try {
				getClient().deleteThread(thread.getId());
			} catch (Exception e) {
				LOG.warn("Error deleting conversation thread", e);
			}
		}
	}

	OpenAiAssistant(@NonNull OpenAiAgentService service, @NonNull Assistant assistant) {
		this(service, assistant.getId());
	}

	OpenAiAssistant(@NonNull OpenAiAgentService service, @NonNull String assistantId) {
		this.service = service;
		this.id = assistantId;
		synch(); // Notice this will register the deploy ID under Azure, do not remove
	}

	/**
	 * Synchronizes the configuration in this instance with latest data from server.
	 * 
	 * @throws ToolInitializationException
	 */
	private void synch() {

		// Get agent from server
		openAiAssistant = getClient().retrieveAssistant(id);
		
		// Register model
		register();

		// Updated list of tools, in synch with agent
		Map<String, Tool> newTools = new HashMap<>();
		for (OpenAiTool tool : openAiAssistant.getTools()) {
			String id = tool.getId();
			Tool newTool = toolMap.get(id);
			if (newTool == null) // New tool added by some other user, for now use a proxy
				newTool = tool;
			newTools.put(id, newTool);
		}

		// Disposes tools that are no longer needed
		for (Tool oldTool : toolMap.values()) {
			if (!newTools.containsKey(oldTool.getId())) // tool removed by some other user
				try {
					oldTool.close();
				} catch (Exception e) {
					LOG.warn("Error closing tool", e);
				}
		}

		// Synch map
		toolMap = newTools;
	}

	/**
	 * Updates assistant with the configuration in this instance.
	 */
	private void update() {

		// Updates assistant config with latest tools too
		openAiAssistant.setTools(toolMap.values().stream() //
				.map(tool -> (OpenAiTool) tool) //
				.collect(Collectors.toList()));

		openAiAssistant = getClient().modifyAssistant( //
				openAiAssistant.getId(), //
				AssistantsRequest.getInstance(openAiAssistant));
	}

	/**
	 * Register the deploy ID if we are running in MS Azure See
	 * {@link AzureOpenAiModelService}.
	 */
	private void register() {
		if (getEndpoint() instanceof AzureOpenAiEndpoint) {
			getEndpoint().getChatService(openAiAssistant.getModel());
		}
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
					parts.add(new OpenAiFilePart(ContentType.IMAGE, content.getImageFile().get("file_id"),
							getEndpoint()));
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
							files.add(new OpenAiFilePart(ContentType.GENERIC, a.getFilePath().getFileId(),
									getEndpoint()));
							if (a.getFilePath() != null) {
								sb.append(" - File: [").append(a.getFilePath().getFileId()).append("]");
								files.add(new OpenAiFilePart(ContentType.GENERIC, a.getFilePath().getFileId(),
										getEndpoint()));
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
}