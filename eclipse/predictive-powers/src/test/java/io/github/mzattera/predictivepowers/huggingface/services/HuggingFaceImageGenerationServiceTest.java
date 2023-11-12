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
package io.github.mzattera.predictivepowers.huggingface.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.huggingface.endpoint.HuggingFaceEndpoint;
import io.github.mzattera.util.ImageUtil;

class HuggingFaceImageGenerationServiceTest {

	// TODO add test for createImageEdit()

	@Test
	void test01() throws IOException {
		try (HuggingFaceEndpoint oai = new HuggingFaceEndpoint()) {
			String prompt = "A portrait of a blonde lady, with green eyes, holding a green apple. On the background a red wall with a window opened on a country landscape with a lake. In the sky an eagle flies. Neoromantic oil portrait style";
			List<BufferedImage> images = oai.getImageGenerationService().createImage(prompt, 1, 256, 256);
			assertEquals(images.size(), 1);

			File tmp = File.createTempFile("createImageHF", ".png");
			ImageUtil.toFile(tmp, images.get(0));
			System.out.println("Image saved as: " + tmp.getCanonicalPath());
		} // Close endpoint
	}
}
