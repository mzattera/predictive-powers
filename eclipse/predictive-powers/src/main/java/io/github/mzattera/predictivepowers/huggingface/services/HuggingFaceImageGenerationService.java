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

/**
 * Image generation service over Hugging Face.
 * 
 * @author Massimiliano "Maxi" Zattera
 */
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.github.mzattera.predictivepowers.huggingface.client.multimodal.TextToImageRequest;
import io.github.mzattera.predictivepowers.huggingface.endpoint.HuggingFaceEndpoint;
import io.github.mzattera.predictivepowers.services.ImageGenerationService;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class HuggingFaceImageGenerationService implements ImageGenerationService {

	// This was the first one I found that works fine
	public static final String DEFAULT_MODEL = "prompthero/openjourney-v4";

	public HuggingFaceImageGenerationService(HuggingFaceEndpoint ep) {
		this.endpoint = ep;
		defaultReq.getOptions().setWaitForModel(true); // TODO remove? Improve?
		defaultReq.getOptions().setUseCache(false);
	}

	@NonNull
	@Getter
	private final HuggingFaceEndpoint endpoint;

	@NonNull
	@Getter
	@Setter
	private String model = DEFAULT_MODEL;

	protected final TextToImageRequest defaultReq = new TextToImageRequest();

	@Override
	public List<BufferedImage> createImage(String prompt, int n, int width, int height) throws IOException {

		defaultReq.setInputs(prompt);
		List<BufferedImage> result = new ArrayList<>(n);
		for (int i = 0; i < n; ++i) {
			result.add(endpoint.getClient().textToImage(model, defaultReq));
		}

		return result;
	}

	@Override
	public List<BufferedImage> createImageVariation(BufferedImage prompt, int n, int width, int height)
			throws IOException {
		throw new UnsupportedOperationException();
	}

}
