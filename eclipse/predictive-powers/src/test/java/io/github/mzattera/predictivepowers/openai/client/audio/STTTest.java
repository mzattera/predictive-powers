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
package io.github.mzattera.predictivepowers.openai.client.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.mzattera.predictivepowers.TestConfiguration;
import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;
import io.github.mzattera.predictivepowers.openai.client.OpenAiEndpoint;
import io.github.mzattera.util.ResourceUtil;

class STTTest {

	private static List<ImmutablePair<OpenAiEndpoint, String>> svcs;

	@BeforeAll
	static void init() {
		svcs = TestConfiguration.getSTTServices().stream() //
				.filter(p -> p.getLeft() instanceof OpenAiEndpoint) //
				.map(p -> new ImmutablePair<OpenAiEndpoint, String>((OpenAiEndpoint) p.getLeft(), p.getRight())) //
				.collect(Collectors.toList());
	}

	@AfterAll
	static void tearDown() {
		TestConfiguration.close(svcs.stream().map(p -> p.getLeft()).collect(Collectors.toList()));
	}

	static Stream<ImmutablePair<OpenAiEndpoint, String>> services() {
		return svcs.stream();
	}

	@DisplayName("Transcription from a file.")
	@ParameterizedTest
	@MethodSource("services")
	void testTranscript(Pair<OpenAiEndpoint, String> p) throws IOException {
		OpenAiEndpoint endpoint = p.getLeft();
		AudioRequest req = new AudioRequest();
		String model = p.getRight();
		req.setModel(model);

		req.setResponseFormat(null);
		String resp = endpoint.getClient().createTranscription(ResourceUtil.getResourceFile("it-buongiorno.m4a"), req);
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
	}

	@DisplayName("File translation.")
	@ParameterizedTest
	@MethodSource("services")
	void testTranslation(Pair<OpenAiEndpoint, String> p) throws IOException {
		OpenAiEndpoint endpoint = p.getLeft();
		AudioRequest req = new AudioRequest();
		String model = p.getRight();
		req.setModel(model);

		String resp = endpoint.getClient().createTranslation(ResourceUtil.getResourceFile("it-to_translate.m4a"), req);
		assertEquals("Good morning, my name is Massimiliano and I am a software developer.", resp);
	}
}
