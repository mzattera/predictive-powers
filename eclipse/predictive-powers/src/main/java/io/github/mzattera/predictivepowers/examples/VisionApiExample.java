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

package io.github.mzattera.predictivepowers.examples;

import io.github.mzattera.predictivepowers.openai.services.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.Agent;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage;
import io.github.mzattera.predictivepowers.services.messages.FilePart;

public class VisionApiExample {

	public static void main(String[] args) throws Exception {

		try (OpenAiEndpoint endpoint = new OpenAiEndpoint();
				Agent bot = endpoint.getChatService("gpt-4-turbo");) {

			// Build the message to send
			ChatMessage msg = new ChatMessage("What is depicted in this image?");

			// Provide an URL to the the image to inspect
			msg.addPart(FilePart.fromUrl(
					"https://upload.wikimedia.org/wikipedia/commons/thumb/c/ce/Alan_turing_header.jpg/500px-Alan_turing_header.jpg"));

			// An image can also be created from file
//			msg.addPart(FilePart.fromFileName("C:\\myImage.png"));

			// Interact with the bot and print its response
			System.out.println(bot.chat(msg).getText());

		} // Close resources
	}
}