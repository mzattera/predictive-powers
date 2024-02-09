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
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mzattera.predictivepowers.openai.client.SortOrder;
import io.github.mzattera.predictivepowers.openai.client.assistants.Assistant;
import io.github.mzattera.predictivepowers.openai.client.assistants.AssistantsRequest;
import io.github.mzattera.predictivepowers.openai.client.threads.Content;
import io.github.mzattera.predictivepowers.openai.client.threads.Content.Text.Annotation;
import io.github.mzattera.predictivepowers.openai.client.threads.Message;
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
import io.github.mzattera.predictivepowers.services.messages.ChatCompletion;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage.Author;
import io.github.mzattera.predictivepowers.services.messages.FilePart.ContentType;
import io.github.mzattera.predictivepowers.services.messages.FinishReason;
import io.github.mzattera.predictivepowers.services.messages.MessagePart;
import io.github.mzattera.predictivepowers.services.messages.TextPart;
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

	private OpenAiThread thread;

	@Override
	public String getModel() {
		return openAiAssistant.getModel();
	}

	@Override
	public void setModel(@NonNull String model) {
		openAiAssistant.setModel(model);
		update();
	}

	@Override
	public String getId() {
		return openAiAssistant.getId();
	}

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
		return endpoint.getClient().listMessages(thread.getId(), SortOrder.ASCENDING, null, null, null).getData();

//		List<Message> result = new ArrayList<>();
//
//		while (true) {
//			DataList<Message> msgs = endpoint.getClient().listMessages(thread.getId(), SortOrder.ASCENDING, null, null,
//					last.getId());
//			result.addAll(msgs.getData());
//			if (!msgs.hasMore())
//				break;
//			last = result.get(result.size() - 1);
//		}
//
//		return result;
	}

	@Override
	public List<? extends Tool> getTools() {
		// TODO URGENT implement
		return null;
	}

	@Override
	public void setTools(@NonNull Collection<? extends Tool> tools) throws ToolInitializationException {
		// TODO URGENT implement

	}

	@Override
	public void addTool(@NonNull Tool tool) throws ToolInitializationException {
		// TODO URGENT implement

	}

	@Override
	public void addTools(@NonNull Collection<? extends Tool> tools) throws ToolInitializationException {
		// TODO URGENT implement

	}

	@Override
	public Tool removeTool(@NonNull String id) {
		// TODO URGENT implement
		return null;
	}

	@Override
	public Tool removeTool(@NonNull Tool tool) {
		// TODO URGENT implement
		return null;
	}

	@Override
	public void clearTools() {
		// TODO URGENT implement

	}

	/**
	 * Creates a new Assistant, on the OpenAi server side.
	 */
	public static OpenAiAssistant createAssistant(OpenAiEndpoint endpoint) {
		return createAssistant(endpoint, AssistantsRequest.builder() //
				.description("\"Default\" OpenAI Assistant") //
				.model("gpt-4-turbo-preview") //
				.name(UUID.randomUUID().toString()) //
				.build());
	}

	/**
	 * Creates a new Assistant, on the OpenAi server side.
	 */
	public static OpenAiAssistant createAssistant(OpenAiEndpoint endpoint, AssistantsRequest req) {
		Assistant openAiAssistant;
		openAiAssistant = endpoint.getClient().createAssistant(req);
		return new OpenAiAssistant(endpoint, openAiAssistant);
	}

	OpenAiAssistant(OpenAiEndpoint endpoint, Assistant openAiAssistant) {
		this.endpoint = endpoint;
		this.openAiAssistant = openAiAssistant;
	}

	OpenAiAssistant(OpenAiEndpoint endpoint, String agentId) {
		this.endpoint = endpoint;
		this.openAiAssistant = this.endpoint.getClient().retrieveAssistant(agentId);
	}

	OpenAiAssistant(OpenAiEndpoint endpoint, String agentId, String threadId) {
		this.endpoint = endpoint;
		this.openAiAssistant = endpoint.getClient().retrieveAssistant(agentId);
		this.thread = endpoint.getClient().retrieveThread(threadId);
	}

	/**
	 * Updates assistant with the configuration in this instance.
	 */
	private void update() {
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
			// TODO add back
//			if (msg.getRole() != Role.ASSISTANT)
//				throw new IllegalArgumentException();

			for (Content content : msg.getContent()) {
				switch (content.getType()) {
				case IMAGE_FILE:
					parts.add(new OpenAiFilePart(ContentType.IMAGE, content.getImageFile().get("file_id"), endpoint));
					break;
				case TEXT:
					parts.add(new TextPart(content.getText().getValue()));
					for (Annotation a : content.getText().getAnnotations()) {
						// TODO URGENT Add Support
					}
					break;
				default:
					throw new IllegalArgumentException();
				}
			}
		}

		return new ChatMessage(Author.BOT, parts);
	}

	public static void main(String[] args) {
		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {
			OpenAiAssistant bot = OpenAiAssistant.createAssistant(ep);
			bot.setPersonality("You are a very sad and depressed robot. "
					+ "Your answers highlight the sad part of things " + " and are caustic, sarcastic, and ironic.");

			// Conversation loop
			try (Scanner console = new Scanner(System.in)) {
				while (true) {
					System.out.print("User     > ");
					String s = console.nextLine();
					System.out.println("Assistant> " + bot.chat(s).getText());
				}
			}
		}
	}
}