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

package io.github.mzattera.predictivepowers.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openai.errors.BadRequestException;
import com.openai.models.images.ImageCreateVariationParams.ResponseFormat;
import com.openai.models.images.ImageModel;

import io.github.mzattera.predictivepowers.AiEndpoint;
import io.github.mzattera.predictivepowers.TestConfiguration;
import io.github.mzattera.predictivepowers.openai.client.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiImageGenerationService;
import io.github.mzattera.predictivepowers.services.messages.FilePart;
import io.github.mzattera.predictivepowers.util.ImageUtil;
import io.github.mzattera.predictivepowers.util.ResourceUtil;

public class ImageGenerationServiceTest {

	private final static Logger LOG = LoggerFactory.getLogger(ImageGenerationServiceTest.class);

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
	public void testGettersSetters(Pair<AiEndpoint, String> p) throws Exception {
		try (ImageGenerationService s = p.getLeft().getImageGenerationService(p.getRight())) {

			assertEquals(p.getLeft(), s.getEndpoint());

			String m = s.getModel();
			assertTrue(m != null);
			s.setModel("banana");
			assertEquals("banana", s.getModel());
			s.setModel(m);
			assertEquals(m, s.getModel());
			assertThrows(NullPointerException.class, () -> s.setModel(null));
		}
	}

	@ParameterizedTest
	@MethodSource("services")
	public void testCreation(Pair<AiEndpoint, String> p) throws Exception {
		try (ImageGenerationService s = p.getLeft().getImageGenerationService(p.getRight())) {
			List<FilePart> r = s.createImage("Create a painting of a white puppy", 2, 1024, 1024);
			assertEquals(2, r.size());
			saveFiles(s, "create", r);
		}
	}

	@ParameterizedTest
	@MethodSource("services")
	public void testVariation(Pair<AiEndpoint, String> p) throws Exception {
		try (ImageGenerationService s = p.getLeft().getImageGenerationService(p.getRight())) {

			// In case we use it
			if ("dall-e-3".equals(s.getModel()))
				return;

			List<FilePart> r = s.createImageVariation(ImageUtil.fromFile(ResourceUtil.getResourceFile("DALLE-2.png")),
					2, 512, 512);
			assertEquals(2, r.size());
			saveFiles(s, "variation", r);
		}
	}

	@ParameterizedTest
	@MethodSource("services")
	public void testEdit(Pair<AiEndpoint, String> p) throws Exception {
		try (ImageGenerationService s = p.getLeft().getImageGenerationService(p.getRight())) {

			// In case we use it
			if ("dall-e-3".equals(s.getModel()))
				return;

			// TODO Fails for dall-e-2
			// https://github.com/openai/openai-java/issues/478
			List<FilePart> r = s.createImageEdit(ImageUtil.fromFile(ResourceUtil.getResourceFile("Gfp-wisconsin-madison-the-nature-boardwalk-LOW.png")),
					"Add a puppy on the pathway", null, 2, 512, 512);
			assertEquals(2, r.size());
			saveFiles(s, "variation", r);
		}
	}
 
	// TODO URGENT Rework this as a generic test like chat service
	
	@Test
	@DisplayName("OpenAI Custom Tests - getters & setters")
	public void oaiTest() throws Exception {

		if (!TestConfiguration.TEST_OPENAI_SERVICES)
			return;

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
	// TODO https://github.com/openai/openai-java/issues/478
	public void oaiTest02() throws Exception {

		if (!TestConfiguration.TEST_OPENAI_SERVICES)
			return;

		try (OpenAiEndpoint ep = new OpenAiEndpoint();
				OpenAiImageGenerationService oaies = ep.getImageGenerationService("dall-e-3")) {

			assertThrows(BadRequestException.class,
					() -> oaies.createImage("Create the image of a white cyborg.", 1, 256, 256));
		}
	}

	@Test
	@DisplayName("OpenAI Custom Tests - Squaring an image and returning base 64")
	public void oaiTest03() throws Exception {

		if (!TestConfiguration.TEST_OPENAI_SERVICES)
			return;

		try (OpenAiEndpoint ep = new OpenAiEndpoint();
				OpenAiImageGenerationService oaies = ep.getImageGenerationService("dall-e-2")) {

			oaies.setDefaultVariationRequest(
					oaies.getDefaultVariationRequest().toBuilder().responseFormat(ResponseFormat.B64_JSON).build());
			List<FilePart> r = oaies.createImageVariation(ImageUtil.fromFile(ResourceUtil.getResourceFile("eagle.png")),
					1, 256, 256);
			assertEquals(1, r.size());
			saveFiles(oaies, "variation", r);
		}
	}

	// add test for non null mask (TODO?)

	private void saveFiles(ImageGenerationService service, String prefix, List<FilePart> images) throws IOException {
		for (FilePart img : images) {
			File tmp = File.createTempFile(prefix + "_" + service.getModel() + "_", ".jpg");
			ImageUtil.toFile(ImageUtil.fromFilePart(img), tmp);
			LOG.info("Image saved as: " + tmp.getCanonicalPath());
		}
	}
}