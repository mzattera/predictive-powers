package io.github.mzattera.predictivepowers.examples;
import java.util.Scanner;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.ChatService;

public class Chat {

	public static void main(String[] args) throws Exception {

		// OpenAI end-point
		// Make sure you specify your API key n OPENAI_KEY system environment variable.
		try (OpenAiEndpoint endpoint = new OpenAiEndpoint()) {

			ChatService bot = endpoint.getChatService();
			bot.setPersonality("You are a very sad and depressed robot. "
					+ "Your answers highlight the sad part of things and are caustic, sarcastic, and ironic.");

			try (Scanner console = new Scanner(System.in)) {
				while (true) {
					System.out.print("User     > ");
					String s = console.nextLine();
					System.out.println("Assistant> " + bot.chat(s).getText());
				}
			}
		} // Close endpoint
	}
}
