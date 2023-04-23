package io.github.mzattera.predictivepowers.openai.client.audio;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.audio.AudioRequest.ResponseFormat;
import io.github.mzattera.util.ResourceUtil;

class AudioTest {

	@Test
	void test01() throws IOException {
		OpenAiEndpoint oai = OpenAiEndpoint.getInstance();
		String model = "whisper-1";
		AudioRequest req = new AudioRequest();

		req.setModel(model);
		req.setResponseFormat(ResponseFormat.TEXT);
		req.setLanguage("en");

		System.out.println(req.toString());

		String resp = oai.getClient().createTranscription(ResourceUtil.getResourceStream("Welcome.wav"), req);

		System.out.println(resp.toString());
	}

	@Test
	void test02() throws IOException {
		OpenAiEndpoint oai = OpenAiEndpoint.getInstance();
		String model = "whisper-1";
		AudioRequest req = new AudioRequest();

		req.setModel(model);
		req.setResponseFormat(ResponseFormat.TEXT);
		req.setLanguage("en");

		System.out.println(req.toString());

		String resp = oai.getClient().createTranscription(ResourceUtil.getResourceStream("Welcome.wav"), req);

		System.out.println(resp.toString());
	}
}
