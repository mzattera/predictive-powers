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
package io.github.mzattera.predictivepowers.openai.client.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;
import io.github.mzattera.predictivepowers.openai.client.audio.AudioSpeechRequest.Voice;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.util.ResourceUtil;

class AudioTest {

	/**
	 * Transcription from a file.
	 */
	@Test
	void testTranscript() throws IOException {
		try (OpenAiEndpoint endpoint = new OpenAiEndpoint()) {
			AudioRequest req = new AudioRequest();
			req.setModel("whisper-1");
			req.setLanguage("it");

			req.setResponseFormat(null);
			String resp = endpoint.getClient().createTranscription(ResourceUtil.getResourceFile("it-buongiorno.m4a"),
					req);
			assertTrue(resp.toLowerCase().contains("buongiorno"));

			req.setResponseFormat(AudioRequest.ResponseFormat.JSON);
			resp = endpoint.getClient().createTranscription(ResourceUtil.getResourceFile("it-buongiorno.m4a"), req);
			assertTrue(resp.toLowerCase().contains("buongiorno"));
			AudioResponse aResp = OpenAiClient.getJsonMapper().readValue(resp, AudioResponse.class);
			assertTrue(aResp.getText().toLowerCase().contains("buongiorno"));

			req.setResponseFormat(AudioRequest.ResponseFormat.SRT);
			resp = endpoint.getClient().createTranscription(ResourceUtil.getResourceFile("it-buongiorno.m4a"), req);
			assertTrue(resp.toLowerCase().contains("buongiorno"));

			req.setResponseFormat(AudioRequest.ResponseFormat.TEXT);
			resp = endpoint.getClient().createTranscription(ResourceUtil.getResourceFile("it-buongiorno.m4a"), req);
			assertTrue(resp.toLowerCase().contains("buongiorno"));

			req.setResponseFormat(AudioRequest.ResponseFormat.VERBOSE_JSON);
			resp = endpoint.getClient().createTranscription(ResourceUtil.getResourceFile("it-buongiorno.m4a"), req);
			assertTrue(resp.toLowerCase().contains("buongiorno"));
			aResp = OpenAiClient.getJsonMapper().readValue(resp, AudioResponse.class);
			assertTrue(aResp.getText().toLowerCase().contains("buongiorno"));
			assertEquals(1, aResp.getSegments().size());
			assertTrue(aResp.getSegments().get(0).getText().toLowerCase().contains("buongiorno"));

			req.setResponseFormat(AudioRequest.ResponseFormat.VTT);
			resp = endpoint.getClient().createTranscription(ResourceUtil.getResourceFile("it-buongiorno.m4a"), req);
			assertTrue(resp.toLowerCase().contains("buongiorno"));
			assertTrue(resp.startsWith("WEBVTT"));
		} // Close endpoint
	}

	/**
	 * File translation.
	 */
	@Test
	void testTranslation() throws IOException {
		try (OpenAiEndpoint endpoint = new OpenAiEndpoint()) {
			String model = "whisper-1";
			AudioRequest req = new AudioRequest();
			req.setModel(model);

			String resp = endpoint.getClient().createTranslation(ResourceUtil.getResourceFile("it-to_translate.m4a"),
					req);
			assertEquals("Good morning, my name is Massimiliano and I am a software developer.", resp);
		} // Close endpoint
	}

	/**
	 * Text to speech.
	 * 
	 * @throws IOException
	 */
	@Test
	void testTTS() throws IOException {
		try (OpenAiEndpoint endpoint = new OpenAiEndpoint()) {
			String text = "Ma così posso dire polenta senza perdere la elle?";
			AudioSpeechRequest req = AudioSpeechRequest.builder().model("tts-1").input(text).voice(Voice.ONYX).build();

			File tmp = File.createTempFile("STT", ".mp3");
			endpoint.getClient().createSpeech(req, tmp);
			System.out.println("TTS Audio Saved to File: " + tmp.getCanonicalPath());

			req = AudioSpeechRequest.builder().model("tts-1-hd").input(text)
					.responseFormat(AudioSpeechRequest.ResponseFormat.AAC).speed(1.0).voice(Voice.ONYX).build();

			tmp = File.createTempFile("STT", ".aac");
			endpoint.getClient().createSpeech(req, tmp);
			System.out.println("TTS Audio Saved to File: " + tmp.getCanonicalPath());

		} // Close endpoint
	}
}
