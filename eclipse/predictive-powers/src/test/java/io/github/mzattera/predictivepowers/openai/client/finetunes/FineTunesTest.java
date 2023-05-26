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
package io.github.mzattera.predictivepowers.openai.client.finetunes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.tika.exception.TikaException;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;
import io.github.mzattera.predictivepowers.openai.client.files.File;
import io.github.mzattera.util.ExtractionUtil;
import io.github.mzattera.util.ResourceUtil;

class FineTunesTest {

	// TODO add checks about events

	@Test
	void test01() throws IOException {
		try (OpenAiEndpoint oai = OpenAiEndpoint.getInstance()) {
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
		} // Close endpoint
	}

	/**
	 * Tests uploading and retrieving a file.
	 * 
	 * @throws IOException
	 * @throws TikaException 
	 * @throws SAXException 
	 */
	@Test
	void test02() throws IOException, SAXException, TikaException {
		try (OpenAiEndpoint oai = OpenAiEndpoint.getInstance()) {
			OpenAiClient c = oai.getClient();

			// Upload file for training
			java.io.File resource = ResourceUtil.getResourceFile("sentiment_training_dataset.jsonl");
			File training = c.uploadFile(resource.getCanonicalPath(), "fine-tune");

			// Retrieve its content
			java.io.File dnLoad = java.io.File.createTempFile("OpenAi", ".jsonl");
			c.retrieveFileContent(training.getId(), dnLoad);

			assertEquals(ExtractionUtil.fromFile(resource), ExtractionUtil.fromFile(dnLoad));
		} // Close endpoint
	}

	/**
	 * Tests listing events
	 * 
	 * @throws IOException
	 * @throws TikaException 
	 * @throws SAXException 
	 */
	@Test
	void test03()  {
		try (OpenAiEndpoint oai = OpenAiEndpoint.getInstance()) {
			OpenAiClient c = oai.getClient();
			
			List<FineTune> fineTunes = c.listFineTunes();
			assertTrue(fineTunes.size()>0);
			
			List<FineTuneEvent> events = c.listFineTuneEvents(fineTunes.get(0).getId());
			assertTrue(events.size()>0);
			assertEquals("fine-tune-event", events.get(0).getObject());
			
			for (FineTuneEvent e : events) {
				System.out.println(e.toString());
			}
		} // Close endpoint
	}
}
