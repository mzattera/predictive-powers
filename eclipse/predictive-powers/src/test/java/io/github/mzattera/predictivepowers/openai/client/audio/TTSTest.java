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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.mzattera.predictivepowers.TestConfiguration;
import io.github.mzattera.predictivepowers.openai.client.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.audio.AudioSpeechRequest.Voice;

class TTSTest {

	private static List<ImmutablePair<OpenAiEndpoint, String>> svcs;

	@BeforeAll
	static void init() {
		svcs = TestConfiguration.getTTSServices().stream() //
				.filter(p -> p.getLeft() instanceof OpenAiEndpoint) //
				.map(p -> new ImmutablePair<OpenAiEndpoint, String>((OpenAiEndpoint) p.getLeft(), p.getRight())) //
				.toList();
	}

	@AfterAll
	static void tearDown() {
		TestConfiguration.close(svcs.stream().map(p -> p.getLeft()).toList());
	}

	static Stream<ImmutablePair<OpenAiEndpoint, String>> services() {
		return svcs.stream();
	}

	@DisplayName("Text to speech.")
	@ParameterizedTest
	@MethodSource("services")
	void testTTS(Pair<OpenAiEndpoint, String> p) throws IOException {
		OpenAiEndpoint endpoint = p.getLeft();
		String model = p.getRight();

		String text = "Ma così posso dire polenta senza perdere la elle?";
		AudioSpeechRequest req = AudioSpeechRequest.builder() //
				.model(model) //
				.input(text) //
				.voice(Voice.ONYX).build();

		File tmp = File.createTempFile("STT", ".mp3");
		endpoint.getClient().createSpeech(req, tmp);
		System.out.println("TTS Audio Saved to File: " + tmp.getCanonicalPath());

		req = AudioSpeechRequest.builder() //
				.model(model) //
				.input(text) //
				.responseFormat(AudioSpeechRequest.ResponseFormat.AAC) //
				.speed(1.0) //
				.voice(Voice.ONYX).build();

		tmp = File.createTempFile("STT", ".aac");
		endpoint.getClient().createSpeech(req, tmp);
		System.out.println("TTS Audio Saved to File: " + tmp.getCanonicalPath());

	}
}
