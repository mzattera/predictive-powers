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

package io.github.mzattera.predictivepowers.huggingface.services;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.github.mzattera.predictivepowers.huggingface.client.HuggingFaceEndpoint;
import io.github.mzattera.predictivepowers.huggingface.client.Options;
import io.github.mzattera.predictivepowers.huggingface.client.SingleHuggingFaceRequest;
import io.github.mzattera.predictivepowers.services.ImageGenerationService;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Image generation service over Hugging Face.
 * 
 * @author Massimiliano "Maxi" Zattera
 */
@RequiredArgsConstructor
public class HuggingFaceImageGenerationService implements ImageGenerationService {

	// This was the first one I found that works fine
	public static final String DEFAULT_MODEL = "prompthero/openjourney-v4";

	public HuggingFaceImageGenerationService(HuggingFaceEndpoint ep) {
		 // TODO remove? Improve?
		this(ep, new SingleHuggingFaceRequest("", Options.builder().useCache(false).waitForModel(true).build()));
	}

	@NonNull
	@Getter
	protected final HuggingFaceEndpoint endpoint;

	@NonNull
	@Getter
	@Setter
	private String model = DEFAULT_MODEL;

	/**
	 * This request, with its parameters, is used as default setting for each call.
	 * 
	 * You can change any parameter to change these defaults (e.g. the model used)
	 * and the change will apply to all subsequent calls.
	 */
	@Getter
	@NonNull
	private final SingleHuggingFaceRequest defaultReq;

	@Override
	public List<BufferedImage> createImage(String prompt, int n, int width, int height) throws IOException {
		return createImage(prompt, n, width, height, defaultReq);
	}

	public List<BufferedImage> createImage(String prompt, int n, int width, int height, SingleHuggingFaceRequest req)
			throws IOException {

		req.setInputs(prompt);
		List<BufferedImage> result = new ArrayList<>(n);
		for (int i = 0; i < n; ++i) {
			result.add(endpoint.getClient().textToImage(model, defaultReq));
		}

		return result;
	}

	@Override
	public List<BufferedImage> createImageVariation(BufferedImage prompt, int n, int width, int height)
			throws IOException {
		return createImageVariation(prompt, n, width, height, defaultReq);
	}

	public List<BufferedImage> createImageVariation(BufferedImage prompt, int n, int width, int height,
			SingleHuggingFaceRequest req) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void close() {
	}
}
