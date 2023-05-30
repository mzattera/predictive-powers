# predictive-powers

`predictive-powers` is a library to make generative AI (GenAI) easily accessible to Java developers.

Currently the library:

  1. Provides low-level access to OpeanAi API similar to [OpenAI-Java](https://github.com/TheoKanning/openai-java). It adds access to audio API which, at the time of writing (May 2023), is not supported by OpenAI-Java (and [not really working](https://community.openai.com/t/whisper-api-cannot-read-files-correctly/93420), TBH).
  
  2. Adds an abstraction layer for GenAI capabilities, which should allow in the future to plug-in different providers (e.g. Hugging Face) seamlessly (see "[Services](#services)" below).
  
  3. Provides a serializable in-memory vector database. Again, plans are to allow users to plug in any existing vector database in the future.

  4. Offers methods to easily read textual content from web pages and files in different formats (MS Office, PDF, HTML, etc.).
  
## Installation

For the time being, this library comes as a `.jar` file containing all the required dependencies.
The source is a [Maven](https://maven.apache.org/) project inside the `eclipse` folder.

`predictive-powers` requires Java 11 or later.

The code depends, among others, on [Lomboc](https://projectlombok.org/) which is correctly referenced within the `pom.xml` file for this project.
However, to have Lomboc to work in the Eclipse editor, you need to install it inside Eclipse (or any other IDE you are using), as explained on Lomboc website.

To avoid passing the OpenAi API key explicitly, by default the library tries to read it from `OPENAI_API_KEY` system environment variable.
The exact process for setting up this variable depends on the OS you are using.

	
## Usage

### API Clients

API clients are the lowest-level components of this library; they allow you to perform direct API calls to service providers. 
For example, you can access OpeanAi API directly by instantiating an `OpenAiClient` and calling its methods
(similarly to what [OpenAI-Java](https://github.com/TheoKanning/openai-java) does).

*** Update and make more generic, adjust code

Class constructors allow you to pass your OpenAi API key, which will be used in all subsequent calls.
If the API key is `null`, the code will try to read the key from `OPENAI_API_KEY` system environment variable.

After that, you can call OpenAi API directly; this part of code is not heavily documented but matches 1:1 the [OpenAi API definition](https://platform.openai.com/docs/api-reference/introduction).

```java
import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsResponse;

public class OpenAiClientExample {

	public static void main(String[] args) {

		// Get API key from OS environment
		try (OpenAiClient client = new OpenAiClient()) {

			// Complete a sentence
			// see https://platform.openai.com/docs/api-reference/completions
			CompletionsRequest req = CompletionsRequest.builder()
					.model("text-davinci-003")
					.maxTokens(50)
					.prompt("Alan Turing was")
					.build();
			CompletionsResponse resp = client.createCompletion(req);

			// Prints result
			System.out.println(resp.getChoices().get(0).getText());
			
		} // closes client
	}
}
```

will output something like:

```console
 a British mathematician, computer scientist, logician, cryptanalyst, philosopher,
 and theoretical biologist who was highly influential in the development
 of theoretical computer science and artificial intelligence.
```

#### Customization

API clients rely on an underlying `OkHttpClient` which provides features like connection pools, etc.
You can use a customized `OkHttpClient` (e.g. to provide logging) to use in your API client following the belwo steps:

  1. Create a pre-configured version of `OkHttpClient` with `Client.getDefaultHttpClient()`.
     Notice that at this step you will have to provide an API key.
  2. Configure the `OkHttpClient` as desired.
  3. Pass it to your API client constructor.

The below example shows how to configure an `OpenAiClient` to use a proxy.

**** VERIFY CODE

```java
		
		String host = "<Your proxy goes here>";
		int port = 80; // your proxy port goes here

		Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
		OkHttpClient http = ApiClient.getDefaultHttpClient()
				.newBuilder()
				.proxy(proxy)
				.build();
		OpenAiClient cli = new OpenAiClient(http);
		
		//... use client here ...
```

### Endpoints

An endpoint uses a client to provide GenAI capabilities in form of services.

*** ADAPT

; it can be created by passing an optional API key or an existing `OpenAiClient`.

Notice it is good practice to close an endpoint when it is no longer needed; this will close the underlying `OpenAiClient`
terminating idle connections and freeing up resources.

*** Update
  
```java
import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;

[...]

		// Get API key from OS environment
		endpoint = new OpenAiEndpoint();

		// Pass API key explicitly
		endpoint = new OpenAiEndpoint("sk-H0a...Yo1");

		// Build endpoint from an existing API client
		OpenAiClient cli = new OpenAiClient();
		endpoint = new OpenAiEndpoint(cli);
```


### <a name="services"></a>Services

Once the endpoint is created, it can be used to access "services" which are high-level GenAI capabilities. Currently following services are provided:

  * `CompletionService` text completion (including insertions): basically, it executes given text prompt.
  * `ChatService` handles conversations with an agent, taking care of agent personality and conversation history.
  * `EmbeddingService` embeds text and calculates semantic (cosine) similarity between texts; it takes care of automatically splitting long texts when needed.
  * `QuestionAnsweringService` answers questions, using a user-provided context. The context can be a list of embeddings from a [knowledge base](#kb).
  * `QuestionExtractionService` extracts different kinds of questions from a text (e.g. true/false question, multiple choices quizzes, etc.). It automatically handles long texts.
  
The below example shows how to get the `CompletionService` to complete a sentence.

```java
package io.github.mzattera.predictivepowers.examples;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.CompletionService;

public class CompletionExample {

	public static void main(String[] args) {

		try (OpenAiEndpoint endpoint = new OpenAiEndpoint()) {
			
			CompletionService cs = endpoint.getCompletionService();
			System.out.println(cs.complete("Alan Turing was").getText());
			
		} // closes endpoint
	}
}
```

Below we provide some [examples](#examples) about using services; for a detailed description of the functionalities provided, please refer to the library JavaDoc.


#### Service Configuration
  
OpenAi provides a rich set of parameters for each of its API calls. In order to access these parameters, services typically expose a "default request" object.
This object is used when the service calls the OpenAi API. Changing parameters on this object will affect all further calls to the API.

For example, let's assume we want to use `curie` model for text completion:
 
 ```java
import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.CompletionService;

public class DefaultConfigurationExample {

	public static void main(String[] args) {

		try (OpenAiEndpoint endpoint = new OpenAiEndpoint()) {

			CompletionService cs = endpoint.getCompletionService();

			// Set "model" parameter in default request, this will affect all further calls
			cs.getDefaultReq().setModel("text-curie-001");

			// this call now uses text-curie-001 model
			System.out.println(cs.complete("Alan Turing was").getText());
			
		} // closes endpoint
	}
}
```
 
 ### <a name="kb"></a>Knowledge Base
 
 A knowledge base is a vector database storing text embeddings; any number of properties (in the form of a `Map`) can be attached to each embedding. 
 
 The knowledge base provides methods to search text based on embedding similarity or other filtering criteria.
 Each knowledge base can be partitioned into domains, which can be searched separately, to improve performance.
 
 Some examples about how to use a knowledge base can be found [below](#oracle).
  
  
 ### Thread Safety
 
 With the notable exception of the knowledge base and the endpoint, classes in this library are NOT thread safe;
 this is because the library is, at present, supporting a micro-service stateless architecture,
 where AI capabilities are provided at endpoints through REST API.
 
 This greatly simplifies back-end architecture and allows to scale applications automatically and effortlessly when deployed inside a cloud environment.
  
 
## <a name="examples"></a>Examples
 
Below some code examples. These examples, and more, can be found in the [example package](eclipse/predictive-powers/src/main/java/io/github/mzattera/predictivepowers/examples).
 
 
### Chit-chat with GPT
 
One-liner to chat with GPT. Notice how the library allows you to set the bot personality and handles chat history automatically.
 
The below code handles conversation with a very depressed entity similar to the more famous [Marvin](https://en.wikipedia.org/wiki/Marvin_the_Paranoid_Android).
 
 ```java
import java.util.Scanner;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.ChatService;

public class ChatExample {

	public static void main(String[] args) throws Exception {

		try (OpenAiEndpoint endpoint = new OpenAiEndpoint()) {

			// Get chat service and set bot personality
			ChatService bot = endpoint.getChatService();
			bot.setPersonality("You are a very sad and depressed robot. "
					+ "Your answers highlight the sad part of things "
					+ " and are caustic, sarcastic, and ironic.");

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
```

Below is an example of the code output; notice how conversation context is retained automatically through the conversation.
 
![Example of a conversation with GPT-3](./img/Chat.PNG)
 
 
 ### <a name="oracle"></a>All-knowing Oracle
 
 An oracle is a service that can answer questions about a domain.
 
 In the below example, we create an oracle by ingesting a web page into a knowledge base, then we get some questions answered.
 If you type "explain" the oracle will give an explanation about last provided answer.
 
 Notice the service is automatically storing the explanation and the context used to build the answer.
 
```java
package io.github.mzattera.predictivepowers.examples;

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

public class OracleExample {

	public static void main(String[] args) 
			throws MalformedURLException, IOException, SAXException, TikaException 
	{
		try (OpenAiEndpoint endpoint = new OpenAiEndpoint()) {
			
			// Question answering service
			QuestionAnsweringService answerSvc = endpoint.getQuestionAnsweringService();

			try (Scanner console = new Scanner(System.in)) {

				// Get the web page you are interested in
				System.out.print("Web Page Url: ");
				String pageUrl = console.nextLine();
				System.out.println("Reading page " + pageUrl + "...\n");

				// Read the page text, embed it, and store it into a knowledge base
				EmbeddingService embeddingService = endpoint.getEmbeddingService();
				KnowledgeBase knowledgeBase = new KnowledgeBase();
				knowledgeBase.insert(embeddingService.embedURL(pageUrl));

				// Loop to reads questions from user and answer them
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
					// Create context by finding similar text in the web page
					List<Pair<EmbeddedText, Double>> context = 
						knowledgeBase.search(
							embeddingService.embed(question).get(0),
							50, 0
						);

					// Use the context when answering
					answer = answerSvc.answerWithEmbeddings(question, context);

					System.out.println("My Answer: " + answer.getAnswer() + "\n");
				}
			}
		}
	} // closes endpoint
}
```
 
 This will produce the below output:
 
![Example of a conversation with the oracle about the city of Padua](./img/Oracle.PNG)
 
 
### FAQ Creation

The below code downloads a PDF file containing Credit Suisse financial statement for 2022 and creates some FAQ, based on its content.

```java
import java.util.List;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.QnAPair;
import io.github.mzattera.predictivepowers.services.QuestionExtractionService;
import io.github.mzattera.util.ExtractionUtil;
import io.github.mzattera.util.LlmUtil;

public class FaqExample {

	public static void main(String[] args) throws Exception {

		// OpenAI end-point
		// Make sure you specify your API key n OPENAI_KEY system environment variable.
		try (OpenAiEndpoint endpoint = new OpenAiEndpoint()) {

			// Download Credit Suisse financial statement 2022 PDF and extract its text
			// We keep only one piece of 750 tokens
			String statment = LlmUtil.split(
					ExtractionUtil.fromUrl("https://www.credit-suisse.com/media/assets/corporate/docs/about-us/investor-relations/financial-disclosures/financial-reports/csg-ar-2022-en.pdf"),
					750)
					.get(2);

			// Our query generation service
			QuestionExtractionService q = endpoint.getQuestionExtractionService();

			// Get some FAQs and print them
			List<QnAPair> QnA = q.getQuestions(statment);
			for (int i = 0; (i < 3) & (i < QnA.size()); ++i) {
				System.out.println(QnA.get(i).toString());
			}
			System.out.println();

			// fill-the-gap questions
			QnA = q.getFillQuestions(statment);
			for (int i = 0; (i < 3) & (i < QnA.size()); ++i) {
				System.out.println(QnA.get(i).toString());
			}
			System.out.println();

			// true/false questions
			QnA = q.getTFQuestions(statment);
			for (int i = 0; (i < 3) & (i < QnA.size()); ++i) {
				System.out.println(QnA.get(i).toString());
			}
			System.out.println();

			// multiple choice questions
			QnA = q.getMCQuestions(statment);
			for (int i = 0; (i < 3) & (i < QnA.size()); ++i) {
				System.out.println(QnA.get(i).toString());
			}
			System.out.println();
		}
	} // closes endpoint
}
```

This code will produce an output similar to the below:

```console
Question: What was announced by Credit Suisse in October 2022?
Answer:   In October 2022, Credit Suisse announced a strategic plan to create a new Credit Suisse, centered on their core strengths – their leading Wealth Management and Swiss Bank franchises, with strong capabilities in Asset Management and Markets – and returning to their heritage and cultural values.
Question: What are the strategic priorities of Credit Suisse?
Answer:   The strategic priorities of Credit Suisse focus on the restructuring of their Investment Bank, the strengthening and reallocation of their capital, and the accelerated cost transformation.
Question: What is the goal of Credit Suisse's transformation?
Answer:   The goal of Credit Suisse's transformation is to re-establish Credit Suisse as a solid, reliable, and trusted partner with a strong value proposition for all their stakeholders.


Question: The year 2022 was a turning point for Credit Suisse.
Answer:   true
Question: Credit Suisse's strategy focuses on strengthening and reallocating its capital.
Answer:   true
Question: Credit Suisse is primarily a retail bank.
Answer:   false

Question: What was announced by Credit Suisse in October 2022?
 [X] 1. A clear strategy for the future
 [ ] 2. A merger with another bank
 [ ] 3. The resignation of the CEO
 [ ] 4. A decline in profits
 [ ] 5. The closure of all branches
Question: What are the core strengths of Credit Suisse according to the message?
 [X] 1. Leading Wealth Management and Swiss Bank franchises, with strong capabilities in Asset Management and Markets
 [ ] 2. Investment Bank and Corporate Banking
 [ ] 3. Retail Banking and Insurance
 [ ] 4. Real Estate and Construction
 [ ] 5. Energy and Resources
Question: What is Credit Suisse's aim through its strategic, cultural and operational transformation?
 [ ] 1. To become the biggest bank in the world
 [X] 2. To re-establish itself as a solid, reliable and trusted partner with a strong value proposition for all its stakeholders
 [ ] 3. To focus on short-term profits
 [ ] 4. To reduce its workforce
 [ ] 5. To increase its involvement in risky investments
```

