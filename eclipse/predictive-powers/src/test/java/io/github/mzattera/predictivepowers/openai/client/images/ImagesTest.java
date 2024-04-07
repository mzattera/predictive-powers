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
package io.github.mzattera.predictivepowers.openai.client.images;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.mzattera.predictivepowers.TestConfiguration;
import io.github.mzattera.predictivepowers.openai.client.AzureOpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.images.ImagesRequest.ImageSize;
import io.github.mzattera.predictivepowers.openai.client.images.ImagesRequest.ResponseFormat;
import io.github.mzattera.util.ImageUtil;
import io.github.mzattera.util.ResourceUtil;

class ImagesTest {

	// TODO add test for createImageEdit()

	// TODO Add test for image variations using DALL-E 3

	private static List<ImmutablePair<OpenAiEndpoint, String>> svcs;

	@BeforeAll
	static void init() {
		svcs = TestConfiguration.getImageGenerationServices().stream() //
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

	@ParameterizedTest
	@MethodSource("services")
	void testCreation(Pair<OpenAiEndpoint, String> p) throws IOException, URISyntaxException {
		String prompt = "A portrait of a blonde lady, with green eyes, holding a green apple. On the background a red wall with a window opened on a country landscape with a lake. In the sky an eagle flies. Neoromantic oil portrait style";
		ImagesRequest req = new ImagesRequest();

		req.setModel(p.getRight());
		req.setPrompt(prompt);
		req.setSize(ImageSize._1024x1024);

		List<Image> images = p.getLeft().getClient().createImage(req);
		assertEquals(1, images.size());

		File tmp = File.createTempFile("createImage_" + p.getRight().getClass().getName() + "_" + p.getLeft(), ".png");
		ImageUtil.toFile(tmp, ImageUtil.fromUrl(images.get(0).getUrl()));
		System.out.println("Image saved as: " + tmp.getCanonicalPath());
	}

	@ParameterizedTest
	@MethodSource("services")
	void testVariation(Pair<OpenAiEndpoint, String> p) throws IOException {

		ImagesRequest req = new ImagesRequest();

		req.setModel(p.getRight());
		req.setSize(ImageSize._1024x1024);
		req.setResponseFormat(ResponseFormat.BASE_64);

		List<Image> images = null;
		try {
			images = p.getLeft().getClient()
					.createImageVariation(ImageUtil.fromFile(ResourceUtil.getResourceFile("DALLE-2.png")), req);
			assertEquals(1, images.size());
		} catch (UnsupportedOperationException e) {
			assertTrue(p.getLeft() instanceof AzureOpenAiEndpoint);
		}

		if (images != null) {
			File tmp = File.createTempFile("createImage_" + p.getRight().getClass().getName() + "_" + p.getLeft(),
					".png");
			ImageUtil.toFile(tmp, ImageUtil.fromBase64(images.get(0).getB64Json()));
			System.out.println("Image saved as: " + tmp.getCanonicalPath());
		}
	}
}
