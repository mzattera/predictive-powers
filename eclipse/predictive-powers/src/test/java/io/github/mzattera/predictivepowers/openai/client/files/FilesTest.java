/*
 * Copyright 2023-2024 Massimiliano "Maxi" Zattera
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
package io.github.mzattera.predictivepowers.openai.client.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.openai.client.DeleteResponse;
import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;
import io.github.mzattera.predictivepowers.openai.client.DirectOpenAiEndpoint;
import io.github.mzattera.util.ResourceUtil;

class FilesTest {

	private final static String FILE = "sentiment_training_dataset.jsonl";

	@Test
	void test01() throws IOException {
		try (DirectOpenAiEndpoint oai = new DirectOpenAiEndpoint()) {
			OpenAiClient cli = oai.getClient();

			// See how many files we have
			List<File> files = cli.listFiles();
			int existing = files.size();

			// Upload one
			File uploaded = cli.uploadFile(ResourceUtil.getResourceFile(FILE), "fine-tune");
			assertEquals(uploaded.getFilename(), FILE);
			assertEquals(uploaded.getObject(), "file");
			assertEquals(uploaded.getPurpose(), "fine-tune");

			// Shall be there
			files = cli.listFiles();
			assertEquals(files.size(), existing + 1);

			// Compare content
			File f = cli.retrieveFile(uploaded.getId());
			assertEquals(f.getId(), uploaded.getId());
			byte[] b = ResourceUtil.getResourceStream(FILE).readAllBytes();
			assertEquals(uploaded.getBytes(), b.length);
			assertTrue(compare(b, cli.retrieveFileContent(uploaded.getId())));

			// Delete file
			while (true) {
				try {
					DeleteResponse resp = cli.deleteFile(uploaded.getId());
					assertTrue(resp.isDeleted());
					break;
				} catch (Exception e) {
					System.out.println("Waiting for the file to be processed...");
					try {
						TimeUnit.SECONDS.sleep(10);
					} catch (InterruptedException e1) {
					}
				}
			}
		} // Close endpoint
	}

	private boolean compare(byte[] b1, byte[] b2) {
		if (b1.length != b2.length)
			return false;

		for (int i = 0; i < b1.length; ++i)
			if (b1[i] != b2[i])
				return false;

		return true;
	}
}
