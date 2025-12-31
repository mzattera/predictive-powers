/*
 * Copyright 2023-2025 Massimiliano "Maxi" Zattera
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

package io.github.mzattera.predictivepowers.openai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openai.models.images.ImageCreateVariationParams.ResponseFormat;
import com.openai.models.images.ImageModel;

import io.github.mzattera.predictivepowers.BadRequestException;
import io.github.mzattera.predictivepowers.TestConfiguration;
import io.github.mzattera.predictivepowers.services.ImageGenerationService;
import io.github.mzattera.predictivepowers.services.messages.FilePart;
import io.github.mzattera.predictivepowers.util.FileUtil;
import io.github.mzattera.predictivepowers.util.ImageUtil;
import io.github.mzattera.predictivepowers.util.ResourceUtil;

public class OpenAiImageGenerationServiceTest {

	private final static Logger LOG = LoggerFactory.getLogger(OpenAiImageGenerationServiceTest.class);

	static boolean isEnabled() {
		return TestConfiguration.TEST_OPENAI_SERVICES;
	}

	@Test
	@DisplayName("OpenAI Custom Tests - getters & setters")
	@EnabledIf("isEnabled")
	@Disabled
	public void oaiTest() throws Exception {

		try (OpenAiEndpoint ep = new OpenAiEndpoint();
				OpenAiImageGenerationService oaies = ep.getImageGenerationService()) {

			oaies.setModel("banana");
			assertEquals("banana", oaies.getDefaultEditRequest().model().get()._value().asString().get());
			assertEquals("banana", oaies.getDefaultGenerateRequest().model().get()._value().asString().get());
			assertEquals("banana", oaies.getDefaultVariationRequest().model().get()._value().asString().get());

			oaies.setDefaultEditRequest(
					oaies.getDefaultEditRequest().toBuilder().model(ImageModel.GPT_IMAGE_1).build());
			assertEquals(ImageModel.GPT_IMAGE_1, oaies.getDefaultEditRequest().model().get());
			oaies.setDefaultGenerateRequest(
					oaies.getDefaultGenerateRequest().toBuilder().model(ImageModel.GPT_IMAGE_1).build());
			assertEquals(ImageModel.GPT_IMAGE_1, oaies.getDefaultGenerateRequest().model().get());
			oaies.setDefaultVariationRequest(
					oaies.getDefaultVariationRequest().toBuilder().model(ImageModel.GPT_IMAGE_1).build());
			assertEquals(ImageModel.GPT_IMAGE_1, oaies.getDefaultVariationRequest().model().get());
		}
	}

	@Test
	@DisplayName("OpenAI Custom Tests - Known bug")
	@EnabledIf("isEnabled")
	// TODO https://github.com/openai/openai-java/issues/478
	public void oaiTest02() throws Exception {

		System.out.println("Active Threads: " + Thread.activeCount());

		try (OpenAiEndpoint ep = new OpenAiEndpoint();
				OpenAiImageGenerationService oaies = ep.getImageGenerationService("dall-e-3")) {

			System.out.println("Entered!");

			assertThrows(BadRequestException.class,
					() -> oaies.createImage("Create the image of a white cyborg.", 1, 256, 256));
		}
	}

	@Test
	@DisplayName("OpenAI Custom Tests - Squaring an image and returning base 64")
	@EnabledIf("isEnabled")
	public void oaiTest03() throws Exception {

		System.out.println("Active Threads: " + Thread.activeCount());

		try (OpenAiEndpoint ep = new OpenAiEndpoint();
				OpenAiImageGenerationService oaies = ep.getImageGenerationService("dall-e-2")) {

			System.out.println("Entered!");

			oaies.setDefaultVariationRequest(
					oaies.getDefaultVariationRequest().toBuilder().responseFormat(ResponseFormat.B64_JSON).build());
			List<FilePart> r = oaies.createImageVariation(ImageUtil.fromFile(ResourceUtil.getResourceFile("eagle.png")),
					1, 256, 256);
			assertEquals(1, r.size());
			saveFiles(oaies, "variation", r);
		}
	}

	private void saveFiles(ImageGenerationService service, String prefix, List<FilePart> images) throws IOException {
		for (FilePart img : images) {
			File tmp = File.createTempFile(prefix + FileUtil.sanitizeFilename("_" + service.getModel() + "_", "_"),
					".jpg");
			ImageUtil.toFile(ImageUtil.fromFilePart(img), tmp);
			LOG.info("Image saved as: " + tmp.getCanonicalPath());
		}
	}
}