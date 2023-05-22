# predictive-powers

`predictive-powers` is a library to make generative AI (GenAI) easily accessible to Java developers.

Currently the library:

  1. Provides low-level access to OpeanAi API similar to [OpenAI-Java](https://github.com/TheoKanning/openai-java). It adds access to audio API which, at the time of writing (May 2023), is not supported by OpenAI-Java (and [not really working](https://community.openai.com/t/whisper-api-cannot-read-files-correctly/93420), TBH).
  
  2. Adds an abstraction layer for GenAI capabilities, which should allow in the future to plug-in different providers (e.g. Hugging Face) seamlessly.
  
  3. Provides an in-memory vector database and methods to easily process and embed files in different formats (MS Office, PDF, HTML, etc.). Again, plans are to make this library database agnostic.
  
## Installation

For the time being, this library comes as a [Maven](https://maven.apache.org/) project inside the `eclipse` folder. Following versions will be better packaged.

predictive-powers required Java 11 or later.

The code depends, among others, on [Lomboc](https://projectlombok.org/) which is correctly referenced within the `pom.xml` file for this project.
However, to have Lomboc to work in the Eclipse editor, you need to install it inside Eclipse (or any other IDE you are using), as explained on Lomboc website.

To avoid passing the OpenAi API key explicitly, the library tries to read it from 'OPENAI_API_KEY' system environment variable; setting up this variable depends on the OS you are using.

	
## Usage

### Direct OpenAi API calls

You can access OpeanAi API by instantiating an `OpenAiClient`. Class constructors allow you to pass your OpenAi API key, which will be used in all subsequent calls. If you use the no-arguments constructor, the code will try to read the key from 'OPENAI_API_KEY' system environment variable.

After that, you can call OpenAi API directly; this part of code is not heavily documented but matches 1:1 [OpenAi documentation](https://platform.openai.com/docs/api-reference/introduction).

```java
import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsResponse;

public class Test {

	public static void main(String[] args) {

		// Get API key from OS environment
		OpenAiClient cli = new OpenAiClient();

		// Complete a sentence.
		CompletionsRequest req = CompletionsRequest.builder()
			.model("text-davinci-003")
			.maxTokens(50)
			.prompt("Alan Turing was").build();
		CompletionsResponse resp = cli.createCompletion(req);

		System.out.println(resp.getChoices().get(0).getText());

	}

}
```

will output something like:

```console
 a British mathematician, computer scientist, logician, cryptanalyst, philosopher,
 and theoretical biologist who was highly influential in the development
 of theoretical computer science and artificial intelligence.
```

### Endpoint

An endpoint provides GenAI capabilities in form of services; it can be created similarly to an `OpenAiClient`, by passing an API key or reading it from the OS environment.
  
```java
import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;

[...]

	// Get API key from OS environment
	OpenAiEndpoint ep = OpenAiEndpoint.getInstance();

	// Pass API key explicitly
	ep = OpenAiEndpoint.getInstance();

	// Build it from an API client
	OpenAiClient cli = new OpenAiClient();
	ep = OpenAiEndpoint.getInstance(cli);
```

### Services

Once the endpoint is created, it can be used to access "services" which are high-level GenAI capabilities. Currently following services are provided:

  * `CompletionService` text completion (including insertions): basically, it executes given text prompt.
  * `ChatService` handles conversations with an agent, taking care of agent personality and conversation history.
  * `EmbeddingService` embeds text and calculate semantic similarity; it takes care of automatically splitting text when needed, or if desired.
  * `QuestionAnsweringService` answers questions, using a user-provided context. The context can be a knowledge base (see below).
  * `QuestionExtractionService` extracts different kinds of questions from a text (e.g. true/false question, multiple choices quizzes, etc.). It automatically handles long texts.
  
The below example shows how to get the `CompletionService` to complete a sentence.

```java
import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.CompletionService;

public class TestEndpoint {

	public static void main(String[] args) {

		OpenAiEndpoint ep = OpenAiEndpoint.getInstance();
		CompletionService cs = ep.getCompletionService();

		System.out.println(cs.complete("Alan Turing was").getText());
	}
}
```

Below we provide some examples of using services; for a detailed description of the functionalities provided, please refer to the library JavaDoc.

#### Service Configuration
  
OpenAi provides a rich set of parameters for each of its API calls; in order to access these parameters services exposes a "default request" object.
This object is used when the service calls OpenAi API. Changing parameters on this object, will affect all further calls to the API.

 For example, let's assume we want to use `courie` model for text completion; we can change the above text as shown here:
 
 ```java
 import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.CompletionService;

public class TestEndpoint {

	public static void main(String[] args) {

		OpenAiEndpoint ep = OpenAiEndpoint.getInstance();
		CompletionService cs = ep.getCompletionService();
		
		// Set model parameter in default request, this will affect all further calls		
		cs.getDefaultReq().setModel(" text-curie-001");

		// This call now uses text-curie-001 model
		System.out.println(cs.complete("Alan Turing was").getText());
	}
}
 ```
 
 ### Knowledge Base
 
 A knowledge base is a vector database storing text embedding; a number of arbitrary data (in the form of a map) can be attached to each embedding. 
 
 The knowledge base provides methods to search text, based on embedding similarity or other filt3ring criteria. Each knowledge base can be partitioned into domains, which can be searched separately, to improve performance.
 
 
 ### Thread Safety
 
 With the notable exception of the knowledge base, classes in this library are NOT thread safe; this is because the library is, at present, supporting a micro-service stateless architecture,
 where AI capabilities are provided at endpoints through REST API.
 
 This greatly simplifies server architecture and allows to scale applications automatically and effortlessly when deployed inside a cloud environment.
 
 ## Examples
 
 ### Chit-chat with GPT
 
 One-liner to chat with GPT. Notice how the library allows you to set the bot personality and handles chat history automatically.
 
 The below code handles conversation with a very depressed entity similar to the more famous [Marvin](https://en.wikipedia.org/wiki/Marvin_the_Paranoid_Android).
 
 ```java
import java.util.Scanner;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.ChatService;

public class Chat {

	public static void main(String[] args) throws Exception {

		// OpenAI end-point
		// Make sure you specify your API key n OPENAI_KEY system environment variable.
		OpenAiEndpoint endpoint = OpenAiEndpoint.getInstance();

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
	}
}
```

Below is an example of the code output; notice how conversation context is retained automatically through the conversation.
 
![Example of a conversation with GPT-3](./Chat.PNG)
 
 
 ### Knowing-all Oracle
 
 An oracle is a service that can answer questions about a domain.
 
 In the below example, we create an oracle by ingesting a web page into a knowledge base, then we get some questions answered.
 If you type "explain" the oracle will give an explanation about last provided answer.
 
```java
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.knowledge.KnowledgeBase;
import io.github.mzattera.predictivepowers.services.EmbeddedText;
import io.github.mzattera.predictivepowers.services.EmbeddingService;
import io.github.mzattera.predictivepowers.services.QnAPair;
import io.github.mzattera.predictivepowers.services.QuestionAnsweringService;

public class Oracle {

	public static void main(String[] args) throws MalformedURLException, IOException, SAXException, TikaException {

		// OpenAI end-point
		// Make sure you specify your API key in OPENAI_KEY system environment variable.
		OpenAiEndpoint endpoint = OpenAiEndpoint.getInstance();
		QuestionAnsweringService questionAnswer = endpoint.getQuestionAnsweringService();

		try (Scanner console = new Scanner(System.in)) {

			// Get the web page you are interested in
			System.out.print("Web Page Url: ");
			String pageUrl = console.nextLine();
			System.out.println("Reading page " + pageUrl + "...\n");

			// Read the page text, embed it and store it into an in-memory knowledge base
			EmbeddingService embeddingService = endpoint.getEmbeddingService();
			KnowledgeBase knowledgeBase = new KnowledgeBase();
			knowledgeBase.insert(embeddingService.embedUrl(pageUrl));

			// Reads questions from user and answers them
			QnAPair answer = null;
			while (true) {

				// Get user question
				System.out.print("Your Question: ");
				String question = console.nextLine();

				// Does user want an explanation?
				if (question.toLowerCase().equals("explain")) {
					if (answer == null)
						continue;
					System.out.println();
					System.out.println(answer.getExplanation());
					System.out.println();
					continue;
				}

				// If not, answer the question
				// Find similar text in the web page, to use as context
				List<Pair<EmbeddedText, Double>> context = knowledgeBase.search(embeddingService.embed(question).get(0),
						50, 0);

				// Use the context when answering
				answer = questionAnswer.answerWithEmbeddings(question, context);

				System.out.println("My Answer: " + answer.getAnswer() + "\n");
			}
		}
	}
}
 ```
 
 This will produce the below output:
 
![Example of a conversation with the oracle about the city of Padua](./Oracle.PNG)
 
### FAQ Creation

The below code downloads a PDF file containing Credit Suisse financial statement for 2022 and creates some FAQ, based on its content.

```java
import java.util.List;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.QnAPair;
import io.github.mzattera.predictivepowers.services.QuestionExtractionService;
import io.github.mzattera.util.ExtractionUtil;
import io.github.mzattera.util.LlmUtil;

public class FAQ {

	public static void main(String[] args) throws Exception {

		// OpenAI end-point
		// Make sure you specify your API key n OPENAI_KEY system environment variable.
		OpenAiEndpoint endpoint = OpenAiEndpoint.getInstance();

		// Download Credit Suisse financial statement 2022 PDF and extract its text.
		// We keep only one piece of 750 tokens, as extracting questions from a long
		// text might result in a timeout.
		String statment = LlmUtil.split(ExtractionUtil.fromUrl(
				"https://www.credit-suisse.com/media/assets/corporate/docs/about-us/investor-relations/financial-disclosures/financial-reports/csg-ar-2022-en.pdf"),
				750).get(2);

		QuestionExtractionService q = endpoint.getQuestionExtractionService();

		// Get some FAQs and print them
		List<QnAPair> QnA = q.getQuestions(statment);
		for (int i = 0; (i < 3) & (i < QnA.size()); ++i) {
			System.out.println(QnA.get(i).toString());
		}
		System.out.println();

		// Demo fill-in questions
		QnA = q.getFillQuestions(statment);
		for (int i = 0; (i < 3) & (i < QnA.size()); ++i) {
			System.out.println(QnA.get(i).toString());
		}
		System.out.println();

		// Demo true/false questions
		QnA = q.getTFQuestions(statment);
		for (int i = 0; (i < 3) & (i < QnA.size()); ++i) {
			System.out.println(QnA.get(i).toString());
		}
		System.out.println();

		// Demo multiple choice questions
		QnA = q.getMCQuestions(statment);
		for (int i = 0; (i < 3) & (i < QnA.size()); ++i) {
			System.out.println(QnA.get(i).toString());
		}
		System.out.println();
	}
}
```

This code will produce an output similar to the below:

```console
Question: What was announced by Credit Suisse in October 2022?
Answer:   In October 2022, Credit Suisse announced a strategic plan to create a new Credit Suisse, centered on their core strengths and returning to their heritage and cultural values.
Question: What are the core strengths of Credit Suisse?
Answer:   The core strengths of Credit Suisse are their leading Wealth Management and Swiss Bank franchises, with strong capabilities in Asset Management and Markets.
Question: What is the aim of Credit Suisse's strategic, cultural, and operational transformation?
Answer:   The aim of Credit Suisse's strategic, cultural, and operational transformation is to re-establish Credit Suisse as a solid, reliable, and trusted partner with a strong value proposition for all their stakeholders.

Question: Credit Suisse announced a _______ plan to create a new Credit Suisse.
Answer:   strategic
Question: Credit Suisse is centered on its core strengths such as leading Wealth Management and Swiss Bank franchises, with strong capabilities in Asset Management and _______.
Answer:   Markets

Question: Credit Suisse announced a new strategy in 2022.
Answer:   true
Question: The new strategy focuses on creating a more complex bank.
Answer:   false
Question: The strategic priorities include the restructuring of the Investment Bank and the accelerated cost transformation.
Answer:   true

Question: What was announced by Credit Suisse in October 2022?
 [X] 1. A plan to create a new bank
 [ ] 2. The resignation of the CEO
 [ ] 3. A merger with another bank
 [ ] 4. The acquisition of a new subsidiary
 [ ] 5. None of the above
Question: What are Credit Suisse's core strengths according to the message from the Chairman and CEO?
 [ ] 1. Retail banking and insurance
 [ ] 2. Investment banking and trading
 [X] 3. Wealth management and Swiss bank franchises
 [ ] 4. Real estate and private equity
 [ ] 5. None of the above
Question: What is the purpose of Credit Suisse's strategic, cultural, and operational transformation?
 [ ] 1. To become a more complex and diversified bank
 [ ] 2. To reduce the number of stakeholders
 [X] 3. To re-establish Credit Suisse as a solid, reliable, and trusted partner
 [ ] 4. To merge with another bank
 [ ] 5. None of the above
```

