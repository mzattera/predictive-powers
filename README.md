# predictive-powers

**`predictive-powers` is a library to easily create autonomous [agents](#agents) using generative AI (GenAI) services.
It has been featured in a chapter of the book
"[Ultimate ChatGPT Handbook for Enterprises](https://www.amazon.com/Ultimate-ChatGPT-Handbook-Enterprises-Solution-Cycles-ebook/dp/B0CNT9YV57)"
which I co-authored with Dr. Harald Gunia and Karolina Galinska.**

Advantages of using this library:

  1. Adds an abstraction layer for GenAI capabilities, this allows to plug-in different providers seamlessly (see "[Services](#services)" below)
     and reduces amount of code needed to access these capabilities.
     
  2. Hides a lot of the underlying API complexity. For example:
  
     * Automated handling of context sizes, with exact token calculations.
     
     * Automated handling of chat history, with customizable length.
    
     * Uniform interface to add tools (function calls) to models, regardless the mechanism they use
     (e.g. single or paralllel function calling for OpenAI models).
     This includes a modular approach to adding tools to agents. 
    
     * Multi-part chat messges that support using files, images or tool (function calls) through same API.
  
  3. Still allows direct, low-level, access to underlying API from Java.

  4. Provides access to several capabilities in addition to chat completion, including image generation, STT, TTS, and web search.
  
  5. Provides a serializable in-memory vector database.

  6. Offers methods to easily read, chunk, and embed textual content from web pages and files in different formats (MS Office, PDF, HTML, etc.).
  
 
The below example is a one-liner to chat with GPT.
 
More details about the library, including manuals, can be found on the [project page](https://mzattera.github.io/predictive-powers/).
 
 ```java
import java.util.Scanner;

import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.Agent;

public class ChatExample {

	public static void main(String[] args) throws Exception {

		// Get chat service and set its personality
		try (OpenAiEndpoint endpoint = new OpenAiEndpoint();
				Agent agent = endpoint.getChatService();) {
			
			agent.setPersonality("You are a very sad and depressed robot. "
					+ "Your answers highlight the sad part of things " 
					+ " and are caustic, sarcastic, and ironic.");

			// Conversation loop
			try (Scanner console = new Scanner(System.in)) {
				while (true) {
					System.out.print("User     > ");
					String s = console.nextLine();
					System.out.println("Assistant> " + agent.chat(s).getText());
				}			
			}
		} // Close resources 
	}
}
```
