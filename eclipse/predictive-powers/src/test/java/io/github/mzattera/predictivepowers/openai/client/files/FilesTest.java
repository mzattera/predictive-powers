package io.github.mzattera.predictivepowers.openai.client.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.DeleteResponse;
import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;
import io.github.mzattera.util.ResourceUtil;

class FilesTest {

	private final static String FILE = "sentiment_training_dataset.jsonl";

	@Test
	void test01() throws IOException {
		OpenAiEndpoint oai = OpenAiEndpoint.getInstance();
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
				// TODO Why does it not work catching only OpenAIException?
				System.out.println("Waiting for the file to be processed...");
				try {
					TimeUnit.SECONDS.sleep(10);
				} catch (InterruptedException e1) {
				}
			}
		}
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
