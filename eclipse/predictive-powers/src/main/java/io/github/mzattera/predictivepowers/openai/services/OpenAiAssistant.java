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
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.openai.client.OpenAIClient;
import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.ResponseFormatJsonObject;
import com.openai.models.ResponseFormatJsonSchema;
import com.openai.models.beta.assistants.Assistant;
import com.openai.models.beta.assistants.Assistant.ToolResources;
import com.openai.models.beta.assistants.Assistant.ToolResources.FileSearch;
import com.openai.models.beta.assistants.AssistantTool;
import com.openai.models.beta.assistants.AssistantUpdateParams;
import com.openai.models.beta.assistants.FunctionTool;
import com.openai.models.beta.threads.AssistantResponseFormatOption;
import com.openai.models.beta.threads.Thread;
import com.openai.models.beta.threads.messages.ImageFile;
import com.openai.models.beta.threads.messages.ImageFileContentBlock;
import com.openai.models.beta.threads.messages.ImageUrl;
import com.openai.models.beta.threads.messages.ImageUrlContentBlock;
import com.openai.models.beta.threads.messages.Message;
import com.openai.models.beta.threads.messages.Message.Role;
import com.openai.models.beta.threads.messages.MessageContent;
import com.openai.models.beta.threads.messages.MessageContentPartParam;
import com.openai.models.beta.threads.messages.MessageCreateParams;
import com.openai.models.beta.threads.messages.MessageListParams;
import com.openai.models.beta.threads.messages.MessageListParams.Order;
import com.openai.models.beta.threads.messages.TextContentBlockParam;
import com.openai.models.beta.threads.runs.RequiredActionFunctionToolCall;
import com.openai.models.beta.threads.runs.Run;
import com.openai.models.beta.threads.runs.RunCreateParams;
import com.openai.models.beta.threads.runs.RunCreateParams.TruncationStrategy;
import com.openai.models.beta.threads.runs.RunRetrieveParams;
import com.openai.models.beta.threads.runs.RunStatus;
import com.openai.models.beta.threads.runs.RunSubmitToolOutputsParams;
import com.openai.models.beta.threads.runs.RunSubmitToolOutputsParams.Builder;
import com.openai.models.beta.threads.runs.RunSubmitToolOutputsParams.ToolOutput;

import io.github.mzattera.predictivepowers.openai.client.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.util.OpenAiUtil;
import io.github.mzattera.predictivepowers.services.AbstractAgent;
import io.github.mzattera.predictivepowers.services.Agent;
import io.github.mzattera.predictivepowers.services.Capability;
import io.github.mzattera.predictivepowers.services.Capability.ToolAddedEvent;
import io.github.mzattera.predictivepowers.services.Capability.ToolRemovedEvent;
import io.github.mzattera.predictivepowers.services.Tool;
import io.github.mzattera.predictivepowers.services.ToolInitializationException;
import io.github.mzattera.predictivepowers.services.messages.ChatCompletion;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage.Author;
import io.github.mzattera.predictivepowers.services.messages.FilePart;
import io.github.mzattera.predictivepowers.services.messages.FinishReason;
import io.github.mzattera.predictivepowers.services.messages.JsonSchema;
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
 * <p>
 * This uses OpenAI Assistants API to implement an {@link Agent}. These agents
 * (assistants) are therefore persisted on the OpenAI side. Notice assistants
 * are identified by their unique ID and that you can have multiple instances of
 * the same assistant returned by {@link OpenAiAgentService}.
 * </p>
 * <p>
 * Assistants possess {@link OpenAiAssistantTools} {@link Capability} which is
 * populated by default with any server-side tool (e.g. File Search) that are
 * already attached to the agent at the time an instance of the assistant is
 * created. You can use {@link #getOpenAiAssistantTools()} to access the
 * capability and manipulate its tools.
 * </p>
 * <p>
 * You can use the OpenAI File Search tool by attaching one or more instances of
 * {@link OpenAiFIleSearchTool} to the agent. The agent will search across all
 * vector stores attached to any of these instances. Notice you can attach
 * multiple instances of the tool for same vector store without harm and that
 * any instance you attach will appear in the _OpenAiTool$ {@link Capability}.
 * </p>
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class OpenAiAssistant extends AbstractAgent {

	private final static Logger LOG = LoggerFactory.getLogger(OpenAiAssistant.class);

	private static final int SLEEP_TIME_MILLIS = 1000;

	@Getter
	private final OpenAiAgentService service;

	@Override
	public OpenAiEndpoint getEndpoint() {
		return service.getEndpoint();
	}

	private OpenAIClient getClient() {
		return getEndpoint().getClient();
	}

	private final OpenAiModelService modelService;

	/**
	 * ID of the assistant.
	 */
	@Getter
	private final String id;

	OpenAiAssistant(@NonNull OpenAiAgentService service, @NonNull String assistantId) {
		this.id = assistantId;
		this.service = service;
		this.modelService = getEndpoint().getModelService();

		Assistant assistant = getAssistantData();
		openAiAssistantTools = new OpenAiAssistantTools();
		
		// TODO URGENT
		/*
		 * aggiungila come capability normale rendila6 una inner class controlla in
		 * init() che non sia 6attaccata a 6un altro agent come gestimao gli eventi e la
		 * list a6di tools? -> mi sa meglio lasciare cosi'...
		 */				
		try {
			// Not really required
			openAiAssistantTools.init(this);
		} catch (ToolInitializationException e) {
		} 
		

		// TODO URGENT this need to go in getAssistantData()
		
		// If required, enables and configures the built-in tools
		for (AssistantTool tool : assistant.tools()) {
			if (tool.isFileSearch()) {
				openAiAssistantTools.getFileSearchTool() //
						.enable() //
						.setVectorStoreIds(assistant.toolResources() //
								.flatMap(ToolResources::fileSearch) //
								.flatMap(FileSearch::vectorStoreIds) //
								.orElse(Collections.emptyList()) //
						);
				// TODO URGENT also add search parameters
			}
		}
	}

	/**
	 * Returns data for this assistant, as used by the OpenAI Assistants API.
	 */
	public Assistant getAssistantData() {

		// Get agent from server
		return getClient().beta().assistants().retrieve(id);

		// TODO URGENT update responseFormat too -> can be in metadata?
		
		// TODO URGENT handle remote OpenAI tools properly.....see below
		// Beside that, the source of truth is this agent tools
		// If the user adds function tools, all users must add same tools
		// The user should not add remote tools, unless he is an "admin" of a persisted
		// agent
	}

	/**
	 * Updates data for this assistant, as used by the OpenAI Assistants API.
	 */
	@SuppressWarnings("unchecked")
	public void setAssistantData(Assistant openAiAssistant) {

		// Set agent parameters
		// Unfortunately OpenAI SDK forces us into this madness
		// TODO https://github.com/openai/openai-java/issues/508
		AssistantUpdateParams.Builder b = AssistantUpdateParams.builder() //
				.assistantId(openAiAssistant.id()) //
				.model(openAiAssistant.model()) //
				.name(openAiAssistant.name()) //
				.description(openAiAssistant.description()) //
				.instructions(openAiAssistant.instructions()) //
				.topP(openAiAssistant.topP()) //
				.temperature(openAiAssistant.temperature()); //

		if (openAiAssistant.metadata().isPresent())
			b.metadata(AssistantUpdateParams.Metadata.builder() //
					.additionalProperties( //
							openAiAssistant.metadata().get()._additionalProperties() //
					).build());

		// TODO URGENT support?
//		b.reasoningEffort(???) //
//		b.toolResources(???); //

		// Response format must be set here since it depends on the model
		AssistantResponseFormatOption format = null;
		if (responseFormat == null) {
			format = AssistantResponseFormatOption.ofAuto();
		} else {
			if (modelService.supportsStructuredOutput(openAiAssistant.model(), false)) { // guard
				// Structured outputs supported
				format = AssistantResponseFormatOption.ofResponseFormatJsonSchema( //
						ResponseFormatJsonSchema.builder() //
								.jsonSchema( //
										ResponseFormatJsonSchema.JsonSchema.builder() //
												.name(responseFormat.getTitle() == null ? "NoName"
														: responseFormat.getTitle().replaceAll("[^a-zA-Z0-9_-]", "")) //
												.description(responseFormat.getDescription() == null ? "No description"
														: responseFormat.getDescription()) //
												.schema(JsonValue.from(responseFormat.asMap(true))) //
												.build() //
								).build());
			} else {
				// Only JSON Mode supported
				format = AssistantResponseFormatOption
						.ofResponseFormatJsonObject(ResponseFormatJsonObject.builder().build());
			}
		}
		b.responseFormat(format);

		// TODO URGENT insert built-in tools
		
		// Make sure the list of tools is updated
		boolean strict = modelService.supportsStrictModeToolCall(openAiAssistant.model(), false);
		for (Tool tool : toolMap.values()) {
			b.addTool(AssistantTool.ofFunction(FunctionTool.builder() //
					.function(FunctionDefinition.builder() //
							.description(tool.getDescription()) //
							.name(tool.getId()) //
							.parameters(OpenAiUtil.toFunctionParameters(tool.getParameters(), strict)) //
							.strict(strict) //
							.build())
					.build()));
		}

		getClient().beta().assistants().update(b.build());
	}

	@Override
	public Integer getTopK() {
		return null;
	}

	@Override
	public void setTopK(Integer topK) {
		if (topK != null)
			throw new UnsupportedOperationException();
	}

	@Override
	public Double getTopP() {
		return getAssistantData().topP().orElse(null);
	}

	@Override
	public void setTopP(Double topP) {
		setAssistantData(getAssistantData().toBuilder().topP(topP).build());
	}

	@Override
	public Double getTemperature() {
		// Must scale from [0-2] to [0-100] considering default value as well
		Optional<Double> t = getAssistantData().temperature();
		return t.isEmpty() ? null : (t.get() * 50);
	}

	@Override
	public void setTemperature(Double temperature) {
		// Must scale from [0-2] to [0-100] considering default value as well
		if (temperature == null)
			setAssistantData(getAssistantData().toBuilder().temperature((Double) null).build());
		else
			setAssistantData(getAssistantData().toBuilder().temperature(temperature / 50).build());
	}

	@Override
	public void setMaxHistoryLength(int maxHistoryLength) {
		super.setMaxHistoryLength(maxHistoryLength);
		addHistory(null);
	}

	/**
	 * Adds a message to the history.
	 * 
	 * @param msg
	 */
	private void addHistory(ChatMessage msg) {
		if (msg != null)
			history.add(msg);
		// TODO trim more nicely
		while (history.size() > getMaxHistoryLength())
			history.remove(0);
	}

	@Override
	public int getBaseTokens() {
		return modelService.getTokenizer(getModel()).count(getPersonality());
	}

	@Getter
	private JsonSchema responseFormat;

	@Override
	public void setResponseFormat(JsonSchema responseFormat) {
		Assistant assistant=getAssistantData();
		this.responseFormat = responseFormat;
		setAssistantData(assistant);
	}

	@Override
	public String getModel() {
		return getAssistantData().model();
	}

	@Override
	public void setModel(@NonNull String model) {
		setAssistantData(getAssistantData().toBuilder().model(model).build());
	}

	@Override
	public String getName() {
		return getAssistantData().name().orElse(id);
	}

	@Override
	public void setName(String name) {
		setAssistantData(getAssistantData().toBuilder().name(name).build());
	}

	@Override
	public String getDescription() {
		return getAssistantData().description().orElse(null);
	}

	@Override
	public void setDescription(String description) {
		setAssistantData(getAssistantData().toBuilder().description(description).build());
	}

	@Override
	public String getPersonality() {
		return getAssistantData().instructions().orElse(null);
	}

	@Override
	public void setPersonality(String personality) {
		setAssistantData(getAssistantData().toBuilder().instructions(personality).build());
	}

	// Conversation history for the current thread
	@Getter
	List<ChatMessage> history = new ArrayList<>();

	// Current conversation thread
	@Getter(AccessLevel.PROTECTED)
	private Thread thread;

	// Current conversation run, this is used to return tool call results
	@Getter(AccessLevel.PROTECTED)
	private Run run;

	// Last message in current conversation run, this is used to return tool call
	// results
	@Getter(AccessLevel.PROTECTED)
	private Message usrMsg;

	@Override
	public ChatCompletion chat(String msg) {
		return chat(new ChatMessage(msg));
	}

	// TODO Runs give you option to override tools: maybe add a method that take a
	// list of tools as well

	// TODO does it make sense to have a method the supports specific OpenAI
	// messages, to support their parameters?

	/**
	 * {@inheritDoc}
	 * <p>
	 * Note that this method supports only image files attached to the message, either
	 * as {@link OpenAiFilePart} or {@link FilePart}s with an URL. If you want files
	 * to be available in the File Search tool, you need to add them to any vector
	 * store attached to {@link #getOpenAiAssistantTool()}.
	 * </p>
	 * 
	 */
	@Override
	public ChatCompletion chat(ChatMessage msg) {

		if (msg.getAuthor() != Author.USER)
			// Notice this assumption is used below
			throw new IllegalArgumentException("Message must come from user");

		// Get current conversation thread
		if (thread == null) {
			thread = getClient().beta().threads().create();
			history.clear();
			run = null;
			usrMsg = null;
		}

		if ((run != null) && (RunStatus.REQUIRES_ACTION.equals(run.status()))) {

			// Current run is waiting for tool call results, we must provide them
			if (!msg.hasToolCallResults())
				throw new IllegalArgumentException("Agent is waiting for tool call results");

			List<ToolCallResult> results = msg.getToolCallResults();
			if (results.size() != msg.getParts().size())
				// TODO Maybe silently ignore?
				throw new IllegalArgumentException("Tool call results cannot contain other parts in the message.");

			Builder params = RunSubmitToolOutputsParams.builder() //
					.threadId(thread.id()) //
					.runId(run.id());
			for (ToolCallResult result : results)
				params.addToolOutput(ToolOutput.builder() //
						.toolCallId(result.getToolCallId()) //
						.output(result.getResult().toString()).build());
			run = getClient().beta().threads().runs().submitToolOutputs(params.build());
		} else {

			// Add message to thread
			try {
				usrMsg = getClient().beta().threads().messages().create(MessageCreateParams.builder() //
						.threadId(thread.id()) //
						.role(MessageCreateParams.Role.USER) //
						.contentOfArrayOfContentParts(fromChatMessage(msg)).build() //
				);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			// Create new run for the message
			RunCreateParams.Builder b = RunCreateParams.builder() //
					.threadId(thread.id()) //
					.assistantId(id);
			if (getMaxNewTokens() != null)
				b.maxCompletionTokens(getMaxNewTokens().intValue());
			// See https://platform.openai.com/docs/assistants/deep-dive on prompt length
			b.maxPromptTokens(Math.max(50_000, getMaxConversationTokens()));
			b.truncationStrategy(TruncationStrategy.builder() //
					.lastMessages(getMaxConversationSteps()).build());
			run = getClient().beta().threads().runs().create(b.build());
		}

		addHistory(msg);

		// Wait for completion
		while (RunStatus.QUEUED.equals(run.status()) || RunStatus.IN_PROGRESS.equals(run.status())) {

			// TODO Add asynch method that returns at each run step?
			// maybe return run steps as results, instead of messages, so the results can be
			// progressed easily and caller always looks at ChatMessage?
			// Or have a onRunProgress() that returns the list of new run steps and
			// onMessageCompleted() that return a ChatCompletion

			try { // wait a bit
				java.lang.Thread.sleep(SLEEP_TIME_MILLIS);
			} catch (InterruptedException e) {
			}

			// poll run status
			run = getClient().beta().threads().runs().retrieve(RunRetrieveParams.builder() //
					.threadId(thread.id()) //
					.runId(run.id()).build());
		}

		switch (run.status().value()) {

		case REQUIRES_ACTION:
			// Create the message
			ChatMessage message;
			try {
				message = fromMessages(retrieveNewMessages(thread, usrMsg));
				message.getParts().addAll(fromToolCalls(run.requiredAction().get().submitToolOutputs().toolCalls()));
			} catch (MalformedURLException | URISyntaxException | JsonProcessingException e) {
				throw new RuntimeException(e);
			}
			addHistory(message);
			return new ChatCompletion(FinishReason.COMPLETED, message);

		case COMPLETED:
			try {
				message = fromMessages(retrieveNewMessages(thread, usrMsg));
			} catch (MalformedURLException | URISyntaxException e) {
				throw new RuntimeException(e);
			}
			addHistory(message);
			return new ChatCompletion(FinishReason.COMPLETED, message);

		case CANCELLING:
		case CANCELLED:
			throw new IllegalArgumentException("Run was cancelled");
		case FAILED:
			throw new RuntimeException(run.lastError().get().message());
		case EXPIRED:
			throw new RuntimeException("Run expired");

		default:
			throw new IllegalArgumentException("Unsupported run type: " + run.status().value());
		}
	}

	/**
	 * 
	 * @param thread
	 * @param last   Last message, this can be null to get all messages in the
	 *               thread.
	 * @return All messages added to the thread after last one.
	 */
	private List<Message> retrieveNewMessages(Thread thread, Message last) {

		MessageListParams.Builder b = MessageListParams.builder();
		if (last != null) {
			b.order(Order.ASC);
			b.after(last.id());
		}
		return getClient().beta().threads().messages().list(thread.id(), b.build()).data();
	}

	/**
	 * Translates one chat message into some content that can be used in a thread to
	 * create messages.
	 * 
	 * @param msg
	 * @param endpoint
	 * @return
	 * @throws IOException
	 */
	private static @NonNull List<MessageContentPartParam> fromChatMessage(ChatMessage msg) throws IOException {

		if (msg.getAuthor() != Author.USER)
			throw new IllegalArgumentException("Only user messages are supported");

		if (msg.hasToolCalls())
			throw new IllegalArgumentException("Only API can generate tool/function calls");

		// Paranoid guard, this should be handled in code already.
		if (msg.hasToolCallResults())
			throw new IllegalArgumentException("Tool call results should be handled separatly");

		// TODO URGENT support attachment parameters in create run request (or is this
		// an override?)

		List<MessageContentPartParam> content = new ArrayList<>();
		for (MessagePart part : msg.getParts()) {

			// Notice only text and images are supported.
			// We do not check file types as it might be time and resource consuming and the
			// model will fail anyway

			if (part instanceof TextPart) {
				content.add(MessageContentPartParam.ofText(TextContentBlockParam.builder() //
						.text(part.getContent()).build()));

			} else if (part instanceof OpenAiFilePart) {

				// TODO URGENT Tools cannot access image content unless specified. To pass image
				// files to Code Interpreter, add the file ID in the message attachments list to
				// allow the tool to read and analyze the input. Image URLs cannot be downloaded
				// in Code Interpreter today.
				content.add(MessageContentPartParam.ofImageFile(ImageFileContentBlock.builder() //
						.imageFile(ImageFile.builder() //
								.fileId(((OpenAiFilePart) part).getFileId()).build() //
						).build()) //
				);
			} else if (part instanceof FilePart) {

				// TODO Add automatic upload of files?
				// When creating image files, pass purpose="vision" to allow you to later
				// download and display the input content.
				// Tools cannot access image content unless specified. To pass image files to
				// Code Interpreter, add the file ID in the message attachments list to allow
				// the tool to read and analyze the input. Image URLs cannot be downloaded in
				// Code Interpreter today.
				FilePart file = (FilePart) part;
				if (!file.isWebFile())
					throw new IllegalArgumentException("Only file uploaded via the file API or web URL are supported");

				content.add(MessageContentPartParam.ofImageUrl(ImageUrlContentBlock.builder() //
						.imageUrl(ImageUrl.builder().url(file.getUrl().toString()).build() //
						).build()) //
				);
			}
		}

		return content;
	}

	/**
	 * Translates a list of Messages created in a run into a ChatMessage we can
	 * return.
	 * 
	 * @param newMessages
	 * @return
	 * @throws URISyntaxException
	 * @throws MalformedURLException
	 */
	private @NonNull ChatMessage fromMessages(@NonNull List<Message> messages)
			throws MalformedURLException, URISyntaxException {
		List<MessagePart> parts = new ArrayList<>();

		for (Message msg : messages) {
			if (!Role.ASSISTANT.equals(msg.role()))
				throw new IllegalArgumentException("Message must be generated by the assistant");

			for (MessageContent content : msg.content()) {
				if (content.isImageFile()) {
					parts.add(new OpenAiFilePart(content.asImageFile().imageFile().fileId()));
				} else if (content.isImageUrl()) {
					parts.add(FilePart.fromUrl(content.asImageUrl().imageUrl().url()));
				} else if (content.isRefusal()) {
					parts.add(new TextPart("**The model generated a refusal**\n\n" + content.asRefusal().refusal()));
				} else if (content.isText()) {
					parts.add(new TextPart(content.asText().text().value()));
				} else {
					throw new IllegalArgumentException("Unsupported message type"); // Paranoid
				}
			}
		}

		// TODO Handle annotations
		// https://platform.openai.com/docs/assistants/deep-dive?lang=python
		// However they are not mentioned in the Message API nor available in OpenAI SDK

		return new ChatMessage(Author.BOT, parts);
	}

	/**
	 * Translates a list of tool calls into message parts.
	 * 
	 * @throws JsonProcessingException
	 */
	private List<ToolCall> fromToolCalls(@NonNull List<RequiredActionFunctionToolCall> toolCalls)
			throws JsonProcessingException {
		List<ToolCall> calls = new ArrayList<>();
		for (RequiredActionFunctionToolCall call : toolCalls) {
			ToolCall toolCall = ToolCall.builder() //
					.id(call.id()) //
					.tool(toolMap.get(call.function().name())) //
					.arguments(call.function().arguments()) //
					.build();
			calls.add(toolCall);
		}
		return calls;
	}

	@Override
	public ChatCompletion complete(ChatMessage prompt) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearConversation() {

		if (thread != null) {
			// Currently, there is no way to cleanup threads when an agent is deleted. Since
			// we are not managing conversations at this stage, we simply delete the old
			// thread.
			try {
				getClient().beta().threads().delete(thread.id());
			} catch (Exception e) {
				LOG.warn("Error deleting conversation thread", e);
			}
		}
		thread = null;
		run = null;
		usrMsg = null;
		history.clear();
	}

	@Getter
	private final OpenAiAssistantTools openAiAssistantTools;

	@Override
	public void onToolAdded(@NonNull ToolAddedEvent evt) {
		// Make sure reading the assisant data does not reset tools; paranoid
		Assistant assistant=getAssistantData();
		super.onToolAdded(evt);
		setAssistantData(assistant);
	}

	@Override
	public void onToolRemoved(@NonNull ToolRemovedEvent evt) {
		// Make sure reading the assisant data does not reset tools; paranoid
		Assistant assistant=getAssistantData();
		super.onToolRemoved(evt);
		setAssistantData(assistant);
	}

	@Override
	public void close() {
		super.close();
		modelService.close();
		if (thread != null) {
			// Currently, there is no way to cleanup threads when an agent is deleted. Since
			// we are not managing conversations at this stage, we simply delete the old
			// thread.
			try {
				getClient().beta().threads().delete(thread.id());
			} catch (Exception e) {
				LOG.warn("Error deleting conversation thread", e);
			}
		}
	}
}