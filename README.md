# predictive-powers

**`predictive-powers` is a Java library to easily create autonomous [agents](#agents) using generative AI (GenAI) services.
An early version of this library has been featured in a chapter of the book
"[Ultimate ChatGPT Handbook for Enterprises](https://www.amazon.com/Ultimate-ChatGPT-Handbook-Enterprises-Solution-Cycles-ebook/dp/B0CNT9YV57)"
which I co-authored with Dr. Harald Gunia and Karolina Galinska.**

Advantages of using this library:

  1. Adds an abstraction layer for GenAI capabilities, this allows to plug-in different providers seamlessly and reduces amount of code needed to access these capabilities.
     
  2. Hides a lot of the underlying API complexity. For example:
  
     * Automated handling of context sizes.
     
     * Automated handling of chat history, with customizable length.
    
     * Uniform interface to add tools (function calls) to models, regardless the mechanism they use
     (e.g. function calling and tool calling for OpenAI models are treated in the same way).
     This includes a modular approach to adding tools to agents. 
    
     * Multi-part chat messages that support using files and images through same API.
	 
	 * Automated scaling down and caching of images in chats, tailored to each service provider, to reduce latency and costs.

  3. Provides access to several capabilities in addition to chat completion, including image generation, STT, TTS, and web search.
  
  4. Provides a naive serializable in-memory vector database.

  5. Offers methods to easily read, chunk, and embed textual content from web pages and files in different formats (MS Office, PDF, HTML, etc.).
  
## Installation

`predictive-powers` needs Java 11 or later and can be added as a Maven dependency to your projects.

```
<dependency>
    <groupId>io.github.mzattera</groupId>
    <artifactId>predictive-powers</artifactId>
    <version>0.6.0</version>
</dependency>
```

## Example
 
The below example is a one-liner to chat with GPT.
 
More details about the library, including manuals, can be found on the [project page](https://mzattera.github.io/predictive-powers/).
 
 ```java
import java.util.Scanner;

import io.github.mzattera.predictivepowers.AiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.ChatService;

public class ChatExample {

	public static void main(String[] args) throws Exception {

		try (
				// Uncomment the below to use OpenAI API
				AiEndpoint endpoint = new OpenAiEndpoint();
				ChatService agent = endpoint.getChatService();

		// Uncomment the below to use Hugging Face API
		// AiEndpoint endpoint = new HuggingFaceEndpoint();
		// ChatService agent = endpoint.getChatService();

		) {

			// Give instructions to agent
			agent.setPersonality("You are a very sad and depressed robot. "
					+ "Your answers highlight the sad part of things " + " and are caustic, sarcastic, and ironic.");

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
