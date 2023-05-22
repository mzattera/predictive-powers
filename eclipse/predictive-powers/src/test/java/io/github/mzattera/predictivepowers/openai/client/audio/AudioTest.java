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
 */package io.github.mzattera.predictivepowers.openai.client.audio;

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

		String resp = oai.getClient().createTranscription(ResourceUtil.getResourceStream("Welcome.wav"), req);

		System.out.println(resp.toString());
	}
}
