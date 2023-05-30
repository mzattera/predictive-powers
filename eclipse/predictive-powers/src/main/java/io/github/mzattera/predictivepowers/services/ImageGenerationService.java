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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

/**
 * This service provides methods to create images, from text or other images.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public interface ImageGenerationService extends Service {

	/**
	 * Creates images from a textual prompt.
	 * 
	 * @param prompt The textual prompt.
	 * @param n      Number of images to create.
	 * @param width  Image width (actual implementations might return different
	 *               size, typically smaller).
	 * @param height Image height (actual implementations might return different
	 *               size, typically smaller).
	 * 
	 * @return Generated image(s).
	 * @throws IOException If an error occurs while downloading the images.
	 */
	List<BufferedImage> createImage(String prompt, int n, int width, int height) throws IOException;

	/**
	 * Creates variations from given image, used as a prompt.
	 * 
	 * @param prompt The image f.
	 * @param n      Number of images to create.
	 * @param width  Image width (actual implementations might return different
	 *               size, typically smaller).
	 * @param height Image height (actual implementations might return different
	 *               size, typically smaller).
	 * 
	 * @return Generated image(s).
	 * @throws IOException If an error occurs while downloading the images.
	 */
	List<BufferedImage> createImageVariation(BufferedImage prompt, int n, int width, int height) throws IOException;
}
