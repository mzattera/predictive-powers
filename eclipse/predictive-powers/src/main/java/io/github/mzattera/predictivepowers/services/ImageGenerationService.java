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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import io.github.mzattera.predictivepowers.EndpointException;
import io.github.mzattera.predictivepowers.services.messages.FilePart;
import lombok.NonNull;

/**
 * This service provides methods to create images, from text or other images.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public interface ImageGenerationService extends AiService {

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
	 * @return Generated image(s) as a list of {@link FilePart}; this allows more
	 *         efficient handling of images when they are returned by URLs.
	 */
	List<FilePart> createImage(@NonNull String prompt, int n, int width, int height) throws EndpointException;

	/**
	 * Creates variations from given image, used as a prompt.
	 * 
	 * @param prompt The image to use for creating variations.
	 * @param n      Number of variations to create.
	 * @param width  Image width (actual implementations might return different
	 *               size, typically smaller).
	 * @param height Image height (actual implementations might return different
	 *               size, typically smaller).
	 * 
	 * @return Generated image(s) as a list of {@link FilePart}; this allows more
	 * @throws IOException If an error occurs while downloading the images.
	 */
	List<FilePart> createImageVariation(@NonNull BufferedImage prompt, int n, int width, int height)
			throws EndpointException;

	/**
	 * Creates variations from given image, used as a prompt.
	 * 
	 * @param prompt The image to use for creating variations.
	 * @param n      Number of variations to create.
	 * @param width  Image width (actual implementations might return different
	 *               size, typically smaller).
	 * @param height Image height (actual implementations might return different
	 *               size, typically smaller).
	 * 
	 * @return Generated image(s) as a list of {@link FilePart}; this allows more
	 *         efficient handling of images when they are returned by URLs.
	 */
	List<FilePart> createImageVariation(@NonNull FilePart prompt, int n, int width, int height)
			throws EndpointException;

	/**
	 * Edits given image by following instructions in the prompt.
	 * 
	 * @param image  The image to edit.
	 * @param prompt Instructions about how to edit the image.
	 * @param mask   An optional mask to apply to image, which transparent areas
	 *               indicate where image should be edited
	 * @param n      Number of edits to create.
	 * @param width  Image width (actual implementations might return different
	 *               size, typically smaller).
	 * @param height Image height (actual implementations might return different
	 *               size, typically smaller).
	 * 
	 * @return Generated image(s) as a list of {@link FilePart}; this allows more
	 *         efficient handling of images when they are returned by URLs.
	 * @throws IOException If an error occurs while downloading the images.
	 */
	List<FilePart> createImageEdit(@NonNull BufferedImage image, @NonNull String prompt, BufferedImage mask, int n,
			int width, int height) throws EndpointException;

	/**
	 * Edits given image by following instructions in the prompt.
	 * 
	 * @param image  The image to edit.
	 * @param prompt Instructions about how to edit the image.
	 * @param mask   An optional mask to apply to image, which transparent areas
	 *               indicate where image should be edited
	 * @param n      Number of edits to create.
	 * @param width  Image width (actual implementations might return different
	 *               size, typically smaller).
	 * @param height Image height (actual implementations might return different
	 *               size, typically smaller).
	 * 
	 * @return Generated image(s) as a list of {@link FilePart}; this allows more
	 *         efficient handling of images when they are returned by URLs.
	 * @throws IOException If an error occurs while downloading the images.
	 */
	List<FilePart> createImageEdit(@NonNull FilePart image, @NonNull String prompt, FilePart mask, int n, int width,
			int height) throws EndpointException;
}
