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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.tika.exception.TikaException;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import io.github.mzattera.predictivepowers.huggingface.endpoint.HuggingFaceEndpoint;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiImageGenerationService;
import io.github.mzattera.util.ImageUtil;
import io.github.mzattera.util.ResourceUtil;

public class ImageGenerationServiceTest {

	// TODO break it in smaller tests...

	@Test
	public void test00() throws IOException, SAXException, TikaException {
		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {
			OpenAiImageGenerationService imgSvc =  ep.getImageGenerationService();
			imgSvc.setModel("dall-e-2");
			test01(imgSvc);
		}
		try (HuggingFaceEndpoint ep = new HuggingFaceEndpoint()) {
			test01(ep.getImageGenerationService());
		}
	}

	private void test01(ImageGenerationService s) throws IOException, SAXException, TikaException {
		List<BufferedImage> r = s.createImage("full body male cyborg shaggy long gray hair shor beard green eyes| shimmering gold metal| lighning| full-length portrait| detailed face| symmetric| steampunk| cyberpunk| cyborg| intricate detailed| to scale| hyperrealistic| cinematic lighting| digital art| concept art| mdjrny-v4 style", 2, 512, 512);
		assertEquals(2, r.size());
		for (BufferedImage img : r) {
			File tmp = File.createTempFile(s.getClass().getName(), ".jpg");
			ImageUtil.toFile(tmp, img);
			System.out.println("Image saved as: " + tmp.getCanonicalPath());
		}
	}

	@Test
	public void test02() throws IOException, SAXException, TikaException {
		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {
			OpenAiImageGenerationService s = ep.getImageGenerationService();
			List<BufferedImage> r = s
					.createImageVariation(ImageUtil.fromFile(ResourceUtil.getResourceFile("DALLE-2.png")), 2, 512, 512);
			assertEquals(2, r.size());

			for (BufferedImage img : r) {
				File tmp = File.createTempFile("variation", ".jpg");
				ImageUtil.toFile(tmp, img);
				System.out.println("Image saved as: " + tmp.getCanonicalPath());
			}
		}
	}
}
