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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.openai.client.images.ImagesRequest.ImageQuality;
import io.github.mzattera.predictivepowers.openai.client.images.ImagesRequest.ImageSize;
import io.github.mzattera.predictivepowers.openai.client.images.ImagesRequest.ImageStyle;
import io.github.mzattera.predictivepowers.openai.client.images.ImagesRequest.ResponseFormat;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiImageGenerationService;
import io.github.mzattera.util.ImageUtil;
import io.github.mzattera.util.ResourceUtil;

class OpenAiImageGenerationServiceTest {

	// TODO add test for createImageEdit()

	@Test
	void test01() throws IOException, URISyntaxException {
		try (OpenAiEndpoint oai = new OpenAiEndpoint()) {
			String prompt = "A portrait of a blonde lady, with green eyes, holding a green apple. On the background a red wall with a window opened on a country landscape with a lake. In the sky an eagle flies. Neoromantic oil portrait style";
			ImagesRequest req = new ImagesRequest();

			req.setModel("dall-e-2");
			req.setPrompt(prompt);
			req.setSize(ImageSize._256x256);

			List<Image> images = oai.getClient().createImage(req);
			assertEquals(images.size(), 1);

			File tmp = File.createTempFile("createImageOAI", ".png");
			ImageUtil.toFile(tmp, ImageUtil.fromUrl(images.get(0).getUrl()));
			System.out.println("Image saved as: " + tmp.getCanonicalPath());
		} // Close endpoint
	}

	@Test
	void test02() throws IOException {
		try (OpenAiEndpoint oai = new OpenAiEndpoint()) {
			ImagesRequest req = new ImagesRequest();

			req.setModel("dall-e-2");
			req.setSize(ImageSize._256x256);
			req.setResponseFormat(ResponseFormat.BASE_64);

			List<Image> images = oai.getClient()
					.createImageVariation(ImageUtil.fromFile(ResourceUtil.getResourceFile("DALLE-2.png")), req);
			assertEquals(images.size(), 1);

			File tmp = File.createTempFile("imageVariationOAI", ".png");
			ImageUtil.toFile(tmp, ImageUtil.fromBase64(images.get(0).getB64Json()));
			System.out.println("Image saved as: " + tmp.getCanonicalPath());
		} // Close endpoint
	}

	/**
	 * Tests DALL E-3 parameters.
	 * 
	 * @throws IOException
	 */
	@Test
	void test03() throws IOException {
		String prompt = "A portrait of a blonde lady, with green eyes, holding a green apple. On the background a red wall with a window opened on a country landscape with a lake. In the sky an eagle flies. Neoromantic oil portrait style";

		try (OpenAiEndpoint oai = new OpenAiEndpoint()) {
			OpenAiImageGenerationService imgSvc = oai.getImageGenerationService();
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
		} // Close endpoint
	}

	// TODO Add test for image variations using DALL-E 3
}
