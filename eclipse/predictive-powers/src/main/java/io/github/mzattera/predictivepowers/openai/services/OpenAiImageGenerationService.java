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

package io.github.mzattera.predictivepowers.openai.services;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.github.mzattera.predictivepowers.openai.client.images.Image;
import io.github.mzattera.predictivepowers.openai.client.images.ImagesRequest;
import io.github.mzattera.predictivepowers.openai.client.images.ImagesRequest.ImageSize;
import io.github.mzattera.predictivepowers.openai.client.images.ImagesRequest.ResponseFormat;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.ImageGenerationService;
import io.github.mzattera.util.ImageUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * OpenAI implementation of image generation service.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@RequiredArgsConstructor
public class OpenAiImageGenerationService implements ImageGenerationService {

	public static final String DEFAULT_MODEL = "dall-e-3";

	public OpenAiImageGenerationService(OpenAiEndpoint ep) {
		this(ep, ImagesRequest.builder().model(DEFAULT_MODEL).build());
	}

	@NonNull
	@Getter
	protected final OpenAiEndpoint endpoint;

	/**
	 * This request, with its parameters, is used as default setting for each call.
	 * 
	 * You can change any parameter to change these defaults (e.g. the model used)
	 * and the change will apply to all subsequent calls.
	 */
	@Getter
	@NonNull
	private final ImagesRequest defaultReq;

	@Override
	public String getModel() {
		return defaultReq.getModel();
	}

	@Override
	public void setModel(@NonNull String model) {
		defaultReq.setModel(model);
	}

	@Override
	public List<BufferedImage> createImage(String prompt, int n, int width, int height) throws IOException {
		return createImage(prompt, n, width, height, defaultReq);
	}

	/** Create images using parameters in the provided request. */
	public List<BufferedImage> createImage(String prompt, int n, int width, int height, ImagesRequest req)
			throws IOException {

		req.setPrompt(prompt);
		req.setN(n);
		req.setSize(getMinSize(width, height));
		req.setResponseFormat(ResponseFormat.URL);

		List<Image> images = endpoint.getClient().createImage(req);
		List<BufferedImage> result = new ArrayList<>(images.size());
		for (Image img : images)
			result.add(ImageUtil.fromUrl(img.getUrl()));

		return result;
	}

	@Override
	public List<BufferedImage> createImageVariation(BufferedImage prompt, int n, int width, int height)
			throws IOException {
		return createImageVariation(prompt, n, width, height, defaultReq);
	}

	public List<BufferedImage> createImageVariation(BufferedImage prompt, int n, int width, int height,
			ImagesRequest req) throws IOException {
		req.setSize(getMinSize(width, height));
		req.setResponseFormat(ResponseFormat.URL);
		req.setN(n);

		List<Image> images = endpoint.getClient().createImageVariation(prompt, req);
		List<BufferedImage> result = new ArrayList<>(images.size());
		for (Image img : images)
			result.add(ImageUtil.fromUrl(img.getUrl()));

		return result;
	}

	private static ImageSize getMinSize(int width, int height) {
		if (Math.max(width, height) <= 256) {
			return ImageSize._256x256;
		}
		if (Math.max(width, height) <= 512) {
			return ImageSize._512x512;
		}
		if ((Math.max(width, height) <= 1024) || (width == height)) {
			return ImageSize._1024x1024;
		}
		if (width > height)
			return ImageSize._1792x1024;
		else
			return ImageSize._1024x1792;
	}
}
