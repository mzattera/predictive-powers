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
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mzattera.predictivepowers.AiEndpoint;
import io.github.mzattera.predictivepowers.EndpointException;
import io.github.mzattera.predictivepowers.TestConfiguration;
import io.github.mzattera.predictivepowers.huggingface.services.HuggingFaceImageGenerationService;
import io.github.mzattera.predictivepowers.services.messages.FilePart;
import io.github.mzattera.predictivepowers.util.FileUtil;
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

	static boolean hasServices() {
		return services().findAny().isPresent();
	}

	@ParameterizedTest
	@MethodSource("services")
	@EnabledIf("hasServices")
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
	@EnabledIf("hasServices")
	public void testCreation(Pair<AiEndpoint, String> p) throws Exception {
		try (ImageGenerationService s = p.getLeft().getImageGenerationService(p.getRight())) {

			if (s instanceof HuggingFaceImageGenerationService) { // n > 1 currently not supported in HF
				assertThrows(EndpointException.class,
						() -> s.createImage("Create a painting of a white puppy", 2, 1024, 1024));

				List<FilePart> r = s.createImage("Create a painting of a white puppy", 1, 1024, 512);
				assertEquals(1, r.size());
				saveFiles(s, "create", r);
			} else {
				List<FilePart> r = s.createImage("Create a painting of a white puppy", 2, 1024, 1024);
				assertEquals(2, r.size());
				saveFiles(s, "create", r);
			}
		}
	}

	@ParameterizedTest
	@MethodSource("services")
	@EnabledIf("hasServices")
	public void testVariation(Pair<AiEndpoint, String> p) throws Exception {
		try (ImageGenerationService s = p.getLeft().getImageGenerationService(p.getRight())) {

			// In case we use it
			if ("dall-e-3".equals(s.getModel()))
				return;

			List<FilePart> r = s.createImageVariation(ImageUtil.fromFile(ResourceUtil.getResourceFile("DALLE-2.png")),
					2, 512, 512);
			assertEquals(2, r.size());
			saveFiles(s, "variation", r);
		} catch (EndpointException e) {
			// Not always available
			assertTrue(e.getCause() instanceof UnsupportedOperationException);
		}
	}

	@ParameterizedTest
	@MethodSource("services")
	@EnabledIf("hasServices")
	public void testEdit(Pair<AiEndpoint, String> p) throws Exception {
		try (ImageGenerationService s = p.getLeft().getImageGenerationService(p.getRight())) {

			// In case we use it
			if ("dall-e-3".equals(s.getModel()))
				return;

			// TODO Fails for dall-e-2
			// https://github.com/openai/openai-java/issues/478
			List<FilePart> r = s.createImageEdit(
					ImageUtil.fromFile(
							ResourceUtil.getResourceFile("Gfp-wisconsin-madison-the-nature-boardwalk-LOW.png")),
					"Add a puppy on the pathway", null, 2, 512, 512);
			assertEquals(2, r.size());
			saveFiles(s, "variation", r);
		} catch (EndpointException e) {
			// Not always available
			assertTrue(e.getCause() instanceof UnsupportedOperationException);
		}
	}

	// add test for non null mask (TODO?)

	private void saveFiles(ImageGenerationService service, String prefix, List<FilePart> images) throws IOException {
		for (FilePart img : images) {
			File tmp = File.createTempFile(prefix + FileUtil.sanitizeFilename("_" + service.getModel() + "_", "_"),
					".jpg");
			ImageUtil.toFile(ImageUtil.fromFilePart(img), tmp);
			LOG.info("Image saved as: " + tmp.getCanonicalPath());
		}
	}
}