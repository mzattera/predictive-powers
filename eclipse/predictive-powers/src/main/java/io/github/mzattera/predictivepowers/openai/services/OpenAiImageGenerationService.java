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

	public static final String DEFAULT_MODEL = "text-embedding-ada-002";

	@NonNull
	@Getter
	private final OpenAiEndpoint endpoint;

	@Override
	public String getModel() {
		return "DALL-E 2";
	}

	@Override
	public void setModel(@NonNull String model) {
		throw new UnsupportedOperationException("At this time only DALL-E model is available");
	}

	@Override
	public List<BufferedImage> createImage(String prompt, int n, int width, int height) throws IOException {

		ImagesRequest req = new ImagesRequest();
		req.setPrompt(prompt);
		req.setSize(getMinSize(width, height));
		req.setResponseFormat(ResponseFormat.URL);
		req.setN(n);

		List<Image> images = endpoint.getClient().createImage(req);
		List<BufferedImage> result = new ArrayList<>(images.size());
		for (Image img : images)
			result.add(ImageUtil.fromUrl(img.getUrl()));

		return result;
	}

	@Override
	public List<BufferedImage> createImageVariation(BufferedImage prompt, int n, int width, int height)
			throws IOException {

		ImagesRequest req = new ImagesRequest();
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
		return ImageSize._1024x1024;
	}
}
