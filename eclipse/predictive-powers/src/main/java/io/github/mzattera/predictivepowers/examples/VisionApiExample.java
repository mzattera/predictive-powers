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

package io.github.mzattera.predictivepowers.examples;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.Agent;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage;
import io.github.mzattera.predictivepowers.services.messages.FilePart;
import io.github.mzattera.predictivepowers.services.messages.FilePart.ContentType;

public class VisionApiExample {

	public static void main(String[] args) throws MalformedURLException, URISyntaxException {

		// Create agent using GPT vision model
		try (OpenAiEndpoint endpoint = new OpenAiEndpoint()) {
			Agent bot = endpoint.getChatService("gpt-4-vision-preview");

			// Build the message to send
			ChatMessage msg = new ChatMessage("Is there any grass in this image?");

			// Include the image to inspect from an URL in the message
			msg.getParts().add(FilePart.fromUrl(
					"https://upload.wikimedia.org/wikipedia/commons/thumb/d/dd/Gfp-wisconsin-madison-the-nature-boardwalk.jpg/2560px-Gfp-wisconsin-madison-the-nature-boardwalk.jpg",
					ContentType.IMAGE));

			// The below shows as you can do the same with a local file image
//			 msg.getParts().add(
//			 		new FilePart(new File("YourFileName.jpg"), ContentType.IMAGE)
//			 );

			// Interact with the bot and print its response
			System.out.println(bot.chat(msg).getText());

		} // Close resources
	}
}