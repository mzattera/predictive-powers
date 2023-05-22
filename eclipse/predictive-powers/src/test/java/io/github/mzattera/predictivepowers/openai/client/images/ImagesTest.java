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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.images.ImagesRequest.ImageSize;
import io.github.mzattera.predictivepowers.openai.client.images.ImagesRequest.ResponseFormat;
import io.github.mzattera.util.ImageUtil;
import io.github.mzattera.util.ResourceUtil;

class ImagesTest {

	// TODO add test for image edits

	@Test
	void test01() throws IOException {
		OpenAiEndpoint oai = OpenAiEndpoint.getInstance();
		String prompt = "A portrait of a blonde lady, with green eyes, holding a green apple. On the background a red wall with a window opened on a country landscape with a lake. In the sky an eagle flies. Neoromantic oil portrait style";
		ImagesRequest req = new ImagesRequest();

		req.setPrompt(prompt);
		req.setSize(ImageSize._256x256);

		List<Image> images = oai.getClient().createImage(req);
		assertEquals(images.size(), 1);

		File tmp = File.createTempFile("createImage", ".png");
		ImageUtil.toFile(tmp, ImageUtil.fromUrl(images.get(0).getUrl()));
		System.out.println("Image saved as: " + tmp.getCanonicalPath());
	}

	@Test
	void test02() throws IOException {
		OpenAiEndpoint oai = OpenAiEndpoint.getInstance();
		ImagesRequest req = new ImagesRequest();

		req.setSize(ImageSize._256x256);
		req.setResponseFormat(ResponseFormat.BASE_64);

		List<Image> images = oai.getClient()
				.createImageVariation(ImageUtil.fromFile(ResourceUtil.getResourceFile("DALLE-2.png")), req);
		assertEquals(images.size(), 1);

		File tmp = File.createTempFile("imageVariation", ".png");
		ImageUtil.toFile(tmp, ImageUtil.fromBase64(images.get(0).getB64Json()));
		System.out.println("Image saved as: " + tmp.getCanonicalPath());

	}
}
