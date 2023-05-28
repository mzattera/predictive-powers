package io.github.mzattera.predictivepowers.examples;

import java.util.Scanner;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.ChatService;

public class ChatExample {

	public static void main(String[] args) throws Exception {

		// OpenAI end-point
		// Make sure you specify your API key n OPENAI_KEY system environment variable.
		try (OpenAiEndpoint endpoint = new OpenAiEndpoint()) {

			// Get chat service
			ChatService bot = endpoint.getChatService();
			bot.setPersonality("You are a very sad and depressed robot. "
					+ "Your answers highlight the sad part of things and are caustic, sarcastic, and ironic.");

			// Conversation loop
			try (Scanner console = new Scanner(System.in)) {
				while (true) {
					System.out.print("User     > ");
					String s = console.nextLine();
					System.out.println("Assistant> " + bot.chat(s).getText());
				}
			}
			
		} // closes endpoint
	}
}
