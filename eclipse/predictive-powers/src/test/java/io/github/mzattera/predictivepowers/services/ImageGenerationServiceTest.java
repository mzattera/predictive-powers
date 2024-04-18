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

package io.github.mzattera.predictivepowers.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.mzattera.predictivepowers.AiEndpoint;
import io.github.mzattera.predictivepowers.TestConfiguration;
import io.github.mzattera.predictivepowers.openai.client.DirectOpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.images.ImagesRequest;
import io.github.mzattera.predictivepowers.openai.client.images.ImagesRequest.ImageQuality;
import io.github.mzattera.predictivepowers.openai.client.images.ImagesRequest.ImageStyle;
import io.github.mzattera.predictivepowers.openai.services.OpenAiImageGenerationService;
import io.github.mzattera.util.ImageUtil;
import io.github.mzattera.util.ResourceUtil;

public class ImageGenerationServiceTest {

	private static List<Pair<AiEndpoint, String>> svcs;

	@BeforeAll
	static void init() {
		svcs = TestConfiguration.getImageGenerationServices();
	}

	@AfterAll
	static void tearDown() {
		TestConfiguration.close(svcs.stream().map(p -> p.getLeft()).collect(Collectors.toList()));
	}

	static Stream<Pair<AiEndpoint, String>> services() {
		return svcs.stream();
	}

	@ParameterizedTest
	@MethodSource("services")
	void testCreation(Pair<AiEndpoint, String> p) throws Exception {
		try (ImageGenerationService s = p.getLeft().getImageGenerationService(p.getRight())) {
			List<BufferedImage> r = s.createImage(
					"full body male cyborg shaggy long gray hair shor beard green eyes| shimmering gold metal| lighning| full-length portrait| detailed face| symmetric| steampunk| cyberpunk| cyborg| intricate detailed| to scale| hyperrealistic| cinematic lighting| digital art| concept art| mdjrny-v4 style",
					1, 1024, 1024);
			assertEquals(1, r.size());
			for (BufferedImage img : r) {
				File tmp = File.createTempFile(s.getClass().getName(), ".jpg");
				ImageUtil.toFile(tmp, img);
				System.out.println("Image saved as: " + tmp.getCanonicalPath());
			}
		}
	}

	@ParameterizedTest
	@MethodSource("services")
	void testVariation(Pair<AiEndpoint, String> p) throws Exception {
		try (ImageGenerationService s = p.getLeft().getImageGenerationService(p.getRight())) {
			List<BufferedImage> r = s
					.createImageVariation(ImageUtil.fromFile(ResourceUtil.getResourceFile("DALLE-2.png")), 2, 512, 512);
			assertEquals(2, r.size());

			for (BufferedImage img : r) {
				File tmp = File.createTempFile("variation", ".jpg");
				ImageUtil.toFile(tmp, img);
				System.out.println("Image saved as: " + tmp.getCanonicalPath());
			}
		} catch (UnsupportedOperationException e) {
			assertFalse(p.getLeft() instanceof DirectOpenAiEndpoint);
		}
	}

	@DisplayName("Tests DALL E-3 parameters.")
	@Test
	void testDalle3() throws IOException {
		String prompt = "A portrait of a blonde lady, with green eyes, holding a green apple. On the background a red wall with a window opened on a country landscape with a lake. In the sky an eagle flies. Neoromantic oil portrait style";

		try (DirectOpenAiEndpoint oai = new DirectOpenAiEndpoint();
				OpenAiImageGenerationService imgSvc = oai.getImageGenerationService();) {
			imgSvc.setModel("dall-e-3");

			List<BufferedImage> images = imgSvc.createImage(prompt, 1, 2048, 5120);
			assertEquals(images.size(), 1);
			File tmp = File.createTempFile("imageDalle3Def", ".png");
			ImageUtil.toFile(tmp, images.get(0));
			System.out.println("Image saved as: " + tmp.getCanonicalPath());

			ImagesRequest req = ImagesRequest.builder().model("dall-e-3").quality(ImageQuality.HD)
					.style(ImageStyle.VIVID).build();
			images = imgSvc.createImage(prompt, 1, 2048, 5120, req);
			assertEquals(images.size(), 1);
			tmp = File.createTempFile("imageDalle3Hi", ".png");
			ImageUtil.toFile(tmp, images.get(0));
			System.out.println("Image saved as: " + tmp.getCanonicalPath());

			req = imgSvc.getDefaultReq();
			req.setQuality(ImageQuality.HD);
			req.setStyle(ImageStyle.VIVID);
			images = imgSvc.createImage(prompt, 1, 2048, 5120);
			assertEquals(images.size(), 1);
			tmp = File.createTempFile("imageDalle3HiDef", ".png");
			ImageUtil.toFile(tmp, images.get(0));
			System.out.println("Image saved as: " + tmp.getCanonicalPath());
		}
	}
}