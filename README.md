# predictive-powers

`predictive-powers` is library to make generative AI (easily?) accessible to Java developers.

Currently the library:

  1. Provides low-level access to OpeanAi API similar to [OpenAI-Java](https://github.com/TheoKanning/openai-java). It adds access to audio API which, at the time of writing (May 2023) is unsupported by OpenAI-Java (and [not really working](https://community.openai.com/t/whisper-api-cannot-read-files-correctly/93420) TBH).
  
  2. Adds an abstraction layer to GenAi capabilities, which should allow in the future to plug-in different providers (e.g. Hugging Face) seamlessly.
  
  3. Provides a in-memory vector database and methods to easily process and embed files in different formats (MS Office, PDF, HTML, etc., courtesy of [Apache Tika](https://tika.apache.org/)). Again, plans are to make this DB agnostic and add support for the most commmon vector databases.
  
## Installation

  * This library depends, among others, on Jackson [data-binding](https://github.com/FasterXML/jackson-databind) and [annotations](https://github.com/FasterXML/jackson-annotations),
  and on [Lomboc](https://projectlombok.org/) libraries.
  
    These are correctly referenced within the `pom.xml` file for this project. However, to have Lomboc to work, you need to *install it in Eclipse* or any other IDE you are using, as explained on Lomboc website.
	
## Usage

## Direct OpenAi API calls

You can access OpeanAi API by instantiating an `OpenAiClient`. The constructor allows you to pass your OpenAi API key, which will be used in all subsequent calls. If you use the no-arguments constructor, the code will try to read the key from 'OPENAI_API_KEY' system environment variable.

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
		CompletionsRequest req = CompletionsRequest.builder().model("text-davinci-003").maxTokens(50)
				.prompt("Alan Turing was").build();
		CompletionsResponse resp = cli.createCompletion(req);

		System.out.println(resp.getChoices().get(0).getText());

	}

}
```

will output:

```console
 a British mathematician, computer scientist, logician, cryptanalyst, philosopher, and theoretical biologist who was highly influential in the development of theoretical computer science and artificial intelligence. He is widely considered to be the father of computer science and artificial intelligence, and
```

## Endpoint

An endpoint provides GenAi capabilities. 
  
