/**
 * 
 */
package io.github.mzattera.predictivepowers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.github.mzattera.predictivepowers.openai.client.DirectOpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.AbstractTool;
import io.github.mzattera.predictivepowers.services.Agent;
import io.github.mzattera.predictivepowers.services.ToolInitializationException;
import io.github.mzattera.predictivepowers.services.Toolset;
import io.github.mzattera.predictivepowers.services.messages.ChatCompletion;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage;
import io.github.mzattera.predictivepowers.services.messages.ToolCall;
import io.github.mzattera.predictivepowers.services.messages.ToolCallResult;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * This class handles a conversation between two agents. The agents are put in
 * communication through this class and they can talk each other until one
 * decides to close the conversation or a maximum number of exchanges have been
 * reached.
 * 
 * This class implements a Tool to provide callback methods to agent to close
 * the conversation; corresponding capability is attached and detached when the
 * conversation is created or closed with {@link #close()} respectively.
 * 
 * @author Massimiliano "Maxi" Zattera
 */
public class Conversation implements AutoCloseable {

	private static final String TOOL_ID = Conversation.class.getName().replace('.', '_') + "_endConversation";
	private static final String INSTANCE_ID = Conversation.class.getName() + UUID.randomUUID().toString();

	public enum ConversationStatus {
		NEW, // Conversation noy yet started
		ONGOING, // Conversation in progress
		OK, // Conversation ended successfully
		ERROR, // Conversation ended with an error
		TIMEOUT // Conversation ended because it took too many turns
	};

	/**
	 * This is a tool that will be accessible to agents to end a conversation.
	 */
	public class EndConversationTool extends AbstractTool {

		private final Conversation container;

		// This is a schema describing the function parameters
		private class EndConversationParameters {

			@JsonProperty(required = true)
			@JsonPropertyDescription("The status in which the conversation ended. Use either 'OK' if the conversation ended properly after you achieved your goals, or 'ERROR' if you experienced an error condition or you are unsure about how to continue the conversation to reach your goal (this might depend on your user to be unresponsive or not understanding your instructions)")
			public ConversationStatus status;

			@JsonProperty(required = true)
			@JsonPropertyDescription("The end result of your conversation. This resuld should contain the outcome of the conversation, accordingly to your goals.")
			public String result;
		}

		public EndConversationTool(Conversation container) {

			// Create myself as a Tool for agents
			super(TOOL_ID, // Function name
					"Ends current conversation, allowing you to return a codition (OK or ERROR) and the result of the conversation", // Function
																																		// description
					EndConversationParameters.class); // Function parameters

			this.container = container;
		}

		/**
		 * Call back method that will be used by agents to terminate the conversation
		 * and return results.
		 */
		@Override
		public ToolCallResult invoke(@NonNull ToolCall call) throws Exception {
			container.conversationStatus = ConversationStatus.valueOf(getString("status", call.getArguments()));
			container.conversationResult = getString("result", call.getArguments());
			return new ToolCallResult(call, "Conversation ended with status: " + container.conversationStatus);
		}
	}

	// The agent that initiates the conversation
	private final Agent one;

	// Other agent conversing with this.
	private final Agent two;

	/**
	 * Maximum number of turns before the conversation fails.
	 */
	@Getter
	@Setter
	private int maxTurns = 50;

	/** Status of the conversation. */
	@Getter
	private ConversationStatus conversationStatus = ConversationStatus.NEW;

	/** Result of the conversation. */
	@Getter
	private String conversationResult;

	/**
	 * Creates a pipeline connecting two agents. Notice that a tool is made
	 * available to both agents so they can use it to close the conversation.
	 * 
	 * @param one The agent that will start the conversation.
	 * @param two The other agent in the conversation.
	 * @throws ToolInitializationException
	 */
	@SuppressWarnings("resource")
	public Conversation(Agent one, Agent two) throws ToolInitializationException {

		Toolset callBack = new Toolset(INSTANCE_ID,
				"This capability provides a callback function to end a conversation.");
		callBack.putTool(TOOL_ID, () -> new EndConversationTool(this));
		this.one = one;
		one.addCapability(callBack);

		callBack = new Toolset(INSTANCE_ID, "This capability provides a callback function to end a conversation.");
		callBack.putTool(TOOL_ID, () -> new EndConversationTool(this));
		this.two = two;
		two.addCapability(callBack);
	}

	/**
	 * Begins a conversation by passing the given message to the first agent. Method
	 * exits when the conversation is closed and {@link #getConversationResult()}
	 * and {@link #getConversationStatus()} can be used to inspect the outcome.
	 * 
	 * Notice the conversation history for each of the agents in the conversation is
	 * not cleared before starting the conversation.
	 * 
	 * @param msg
	 */
	public void start(String msg) {
		start(new ChatMessage(msg));
	}

	/**
	 * Begins a conversation by passing the given message to the first agent. Method
	 * exits when the conversation is closed and {@link #getConversationResult()}
	 * and {@link #getConversationStatus()} can be used to inspect the outcome.
	 * 
	 * Notice the conversation history for each of the agents in the conversation is
	 * not cleared before starting the conversation.
	 * 
	 * @param msg
	 */
	public void start(ChatMessage msg) {

		conversationStatus = ConversationStatus.ONGOING;
		Agent bot = one;
		int turns = 0;

		while (turns++ <= maxTurns) {
			ChatCompletion reply = bot.chat(msg);

			// Check if agent generated a function call
			while (reply.hasToolCalls()) {

				List<ToolCallResult> results = new ArrayList<>();

				for (ToolCall call : reply.getToolCalls()) {

					// Print call for illustrative purposes
					System.out.println("CALL " + " > " + call);

					// Execute call, handling errors nicely
					ToolCallResult result;
					try {
						result = call.execute();
					} catch (Exception e) {
						result = new ToolCallResult(call, e);
					}
					results.add(result);
				}

				if (conversationStatus != ConversationStatus.ONGOING)
					break; // We were requested to end conversation.

				// Pass results back to the agent
				// Notice this might in principle generate
				// other tool calls, hence the loop
				reply = bot.chat(new ChatMessage(results));

			} // while we serviced all calls

			if (conversationStatus != ConversationStatus.ONGOING)
				break; // We were requested to end conversation.

			msg = reply.getMessage();
			System.out.println((bot == one ? "ONE: " : "TWO: ") + msg.getContent());
			bot = (bot == one) ? two : one;
		}

		if (conversationStatus == ConversationStatus.ONGOING) {
			// Max turns exceeded
			conversationStatus = ConversationStatus.TIMEOUT;
			conversationResult = "Conversation truncated after " + (turns - 1) + " turns.";
		}
	}

	/**
	 * Clears this conversation, this also clears conversation history for both
	 * agents in the conversation.
	 */
	public void clear() {
		one.clearConversation();
		two.clearConversation();
		conversationStatus = ConversationStatus.NEW;
		conversationResult = null;
	}

	@Override
	public void close() {
		for (String c : one.getCapabilities())
			if (INSTANCE_ID.matches(c))
				one.removeCapability(c);
		for (String c : two.getCapabilities())
			if (INSTANCE_ID.matches(c))
				two.removeCapability(c);
	}

	public static void main(String[] args) throws Exception {
		try (DirectOpenAiEndpoint ep = new DirectOpenAiEndpoint();
				Agent one = ep.getChatService();
				Agent two = ep.getChatService();) {

			one.setPersonality(
					"You are a philosopher arguing that life is fun. Your goal is to make your user agree that life can be fun. You must immediately end this conversation with an 'OK' status when your user is convinced.");
			two.setPersonality(
					"You are in a conversation where your user tries to convince you that life is fun. You are convinced immediately after your user provides a reason why life can be fun. Tell your user you are convinced as soon as you become convinced, do not close the conversation yourself.");

			try (Conversation pip = new Conversation(one, two)) {
				pip.start("OK, let's start!");
				System.out.println(pip.getConversationStatus() + ": " + pip.getConversationResult());
			}
		}
	}
}
