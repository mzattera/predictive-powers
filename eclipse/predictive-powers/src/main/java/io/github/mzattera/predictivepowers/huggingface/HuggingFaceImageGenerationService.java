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

package io.github.mzattera.predictivepowers.huggingface;

import java.util.ArrayList;
import java.util.List;

import io.github.mzattera.hfinferenceapi.client.model.ImageGenerationRequest;
import io.github.mzattera.hfinferenceapi.client.model.ImageGenerationRequestParameters;
import io.github.mzattera.hfinferenceapi.client.model.ImageGenerationResponseDataInner;
import io.github.mzattera.predictivepowers.EndpointException;
import io.github.mzattera.predictivepowers.services.AbstractImageGenerationService;
import io.github.mzattera.predictivepowers.services.messages.Base64FilePart;
import io.github.mzattera.predictivepowers.services.messages.FilePart;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * OpenAI implementation of image generation service.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class HuggingFaceImageGenerationService extends AbstractImageGenerationService {

	public static final String DEFAULT_MODEL = "stabilityai/stable-diffusion-xl-base-1.0:nscale";

	@Getter
	@NonNull
	protected final HuggingFaceEndpoint endpoint;

	@Getter
	@Setter
	@NonNull
	private ImageGenerationRequest defaultRequest;

	protected HuggingFaceImageGenerationService(@NonNull HuggingFaceEndpoint endpoint) {
		this(endpoint, DEFAULT_MODEL);
	}

	protected HuggingFaceImageGenerationService(@NonNull HuggingFaceEndpoint endpoint, @NonNull String model) {
		super.setModel(model);
		this.endpoint = endpoint;
		this.defaultRequest = new ImageGenerationRequest().model(model).n(1).parameters(new ImageGenerationRequestParameters());
	}

	@Override
	public void setModel(@NonNull String model) {
		super.setModel(model);
		defaultRequest.setModel(model);
	}

	@Override
	public List<FilePart> createImage(@NonNull String prompt, int n, int width, int height) throws EndpointException {

		String model = defaultRequest.getModel();

		try {
			List<FilePart> result = new ArrayList<>();
			String[] parts = HuggingFaceUtil.parseModel(model);
			defaultRequest.setModel(parts[0]);
			defaultRequest.setPrompt(prompt);
			defaultRequest.setN(n);

			ImageGenerationRequestParameters params = defaultRequest.getParameters();
			if (params == null) { // paranoid
				params = new ImageGenerationRequestParameters();
				defaultRequest.setParameters(params);
			}
			params.setWidth(width);
			params.setHeight(height);		

			List<ImageGenerationResponseDataInner> response = endpoint.getClient().textToImage(parts[1], defaultRequest)
					.getData();

			for (int i = 0; i < response.size(); ++i) {
				ImageGenerationResponseDataInner data = response.get(i);
				result.add(new Base64FilePart(data.getB64Json(), "Image_" + i));
			}

			return result;

		} catch (Exception e) {
			throw HuggingFaceUtil.toEndpointException(e);
		} finally {
			// Restore model value
			defaultRequest.setModel(model);
		}
	}

	@Override
	public List<FilePart> createImageVariation(@NonNull FilePart prompt, int n, int width, int height)
			throws EndpointException {
		throw new EndpointException(new UnsupportedOperationException());
	}

	@Override
	public List<FilePart> createImageEdit(@NonNull FilePart image, @NonNull String prompt, FilePart mask, int n,
			int width, int height) throws EndpointException {
		throw new EndpointException(new UnsupportedOperationException());
	}
}
