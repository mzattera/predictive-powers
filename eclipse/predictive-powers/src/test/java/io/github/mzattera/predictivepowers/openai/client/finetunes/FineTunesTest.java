package io.github.mzattera.predictivepowers.openai.client.finetunes;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsResponse;
import io.github.mzattera.predictivepowers.openai.client.files.File;
import io.github.mzattera.util.ResourceUtil;

class FineTunesTest {

	@Test
	void test01() throws IOException {
		OpenAiEndpoint oai = OpenAiEndpoint.getInstance();
		OpenAiClient c = oai.getClient();

		// Upload file for training
		File training = c.uploadFile(ResourceUtil.getResourceFile("sentiment_training_dataset.jsonl"), "fine-tune");

		// Start tuning the model
		FineTunesRequest req = FineTunesRequest.builder().trainingFile(training.getId()).model("ada").build();
		FineTune tuned = c.createFineTune(req);
		String status = tuned.getStatus();
		System.out.println("Status=" + status);

		// Wait it is ready
		while (!status.equals("succeeded")) {

			try {
				TimeUnit.SECONDS.sleep(10);
			} catch (InterruptedException e1) {
			}

			tuned = c.retrieveFineTune(tuned.getId());
			status = tuned.getStatus();
			System.out.println("Status=" + status);
		}

		// Give it a try
		CompletionsRequest cReq = CompletionsRequest.builder().model(tuned.getFineTunedModel()).maxTokens(100)
				.prompt("I really like this movie!").logprobs(2).build();
		CompletionsResponse resp = c.createCompletion(cReq);

		System.out.println(resp.toString());

	}
}
