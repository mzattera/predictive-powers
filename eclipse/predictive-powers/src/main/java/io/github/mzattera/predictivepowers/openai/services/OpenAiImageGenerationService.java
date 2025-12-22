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

package io.github.mzattera.predictivepowers.openai.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.openai.core.JsonMissing;
import com.openai.core.MultipartField;
import com.openai.errors.OpenAIInvalidDataException;
import com.openai.models.images.ImageCreateVariationParams;
import com.openai.models.images.ImageEditParams;
import com.openai.models.images.ImageEditParams.Builder;
import com.openai.models.images.ImageGenerateParams;
import com.openai.models.images.ImageModel;
import com.openai.models.images.ImagesResponse;

import io.github.mzattera.predictivepowers.EndpointException;
import io.github.mzattera.predictivepowers.openai.util.OpenAiUtil;
import io.github.mzattera.predictivepowers.services.AbstractImageGenerationService;
import io.github.mzattera.predictivepowers.services.messages.Base64FilePart;
import io.github.mzattera.predictivepowers.services.messages.FilePart;
import io.github.mzattera.predictivepowers.util.ImageUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * OpenAI implementation of image generation service.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class OpenAiImageGenerationService extends AbstractImageGenerationService {

	public static final String DEFAULT_MODEL = "dall-e-3";

	@Getter
	@NonNull
	protected final OpenAiEndpoint endpoint;

	@Override
	public void setModel(@NonNull String model) {
		super.setModel(model);
		defaultGenerateRequest = defaultGenerateRequest.toBuilder().model(model).build();
		defaultVariationRequest = defaultVariationRequest.toBuilder().model(model).build();
		defaultEditRequest = defaultEditRequest.toBuilder().model(model).build();
	}

	@Getter
	@Setter
	@NonNull
	@SuppressWarnings("unchecked")
	private ImageGenerateParams defaultGenerateRequest = ImageGenerateParams.builder().model(DEFAULT_MODEL)
			.prompt(JsonMissing.of()).build();

	@Getter
	@Setter
	@NonNull
	@SuppressWarnings("unchecked")
	private ImageCreateVariationParams defaultVariationRequest = ImageCreateVariationParams.builder()
			.model(DEFAULT_MODEL).image(MultipartField.of(JsonMissing.of())).build();

	@Getter
	@Setter
	@NonNull
	@SuppressWarnings("unchecked")
	private ImageEditParams defaultEditRequest = ImageEditParams.builder().model(DEFAULT_MODEL)
			.image(MultipartField.of(JsonMissing.of())).prompt(MultipartField.of(JsonMissing.of())).build();

	public OpenAiImageGenerationService(@NonNull OpenAiEndpoint endpoint) {
		this(endpoint, DEFAULT_MODEL);
	}

	public OpenAiImageGenerationService(@NonNull OpenAiEndpoint endpoint, @NonNull String model) {
		this.endpoint = endpoint;
		setModel(model); // Notice: do not use super(model) as it won-t fix default requests
	}

	@Override
	public List<FilePart> createImage(@NonNull String prompt, int n, int width, int height) throws EndpointException {

		try {
			ImageModel model = defaultGenerateRequest.model().isEmpty() ? ImageModel.DALL_E_2
					: defaultGenerateRequest.model().get();

			ImageGenerateParams.Size size;
			switch (model.value()) {
			case DALL_E_2:
				if (Math.max(width, height) <= 256)
					size = ImageGenerateParams.Size._256X256;
				else if (Math.max(width, height) <= 512)
					size = ImageGenerateParams.Size._512X512;
				else
					size = ImageGenerateParams.Size._1024X1024;
				break;
			case DALL_E_3:
				if (width == height)
					size = ImageGenerateParams.Size._1024X1024;
				else if (width > height)
					size = ImageGenerateParams.Size._1792X1024;
				else
					size = ImageGenerateParams.Size._1024X1792;
				break;
			default: // Catches gpt-image-1 and future cases too (hopefully)
				if (width == height)
					size = ImageGenerateParams.Size._1024X1024;
				else if (width > height)
					size = ImageGenerateParams.Size._1536X1024;
				else
					size = ImageGenerateParams.Size._1024X1536;
				break;
			}

			try {
				com.openai.models.images.ImageGenerateParams.Builder b = defaultGenerateRequest.toBuilder() //
						.prompt(prompt).size(size);
				if (ImageModel.DALL_E_3.equals(model)) {
					b.n(1);
					List<FilePart> result = new ArrayList<>(n);
					for (int i = 0; i < n; ++i) {
						ImagesResponse resp = endpoint.getClient().images().generate(b.build());
						result.addAll(OpenAiUtil.readImages(resp.data().get()));
					}
					return result;
				} else {
					b.n(n);
					ImagesResponse resp = endpoint.getClient().images().generate(b.build());
					return OpenAiUtil.readImages(resp.data().get());
				}
			} catch (OpenAIInvalidDataException e) {
				// TODO https://github.com/openai/openai-java/issues/478
				// "Create the image of a white cyborg." throws this exception
				// Exception in thread "main" com.openai.errors.OpenAIInvalidDataException:
				// `message` is null
				// at com.openai.core.JsonField.getRequired$openai_java_core(Values.kt:175)
				if (e.getMessage().contains("`message` is null"))
					throw io.github.mzattera.predictivepowers.BadRequestException.badRequestBuilder() //
							.cause(e) //
							.message(e.getMessage()).build();
				throw e;
			}
		} catch (Exception e) {
			throw OpenAiUtil.toEndpointException(e);
		}
	}

	@Override
	public List<FilePart> createImageVariation(@NonNull FilePart prompt, int n, int width, int height)
			throws EndpointException {

		try {
			// For DALL-E-2 make image squared by padding it
			ImageModel model = defaultVariationRequest.model().get();
			if (ImageModel.DALL_E_2.equals(model)) {
				try (InputStream s = prompt.getInputStream()){
				prompt = new Base64FilePart(ImageUtil.padImageToSquare(s), "prompt.png",
						"image/png");}
			}

			ImageCreateVariationParams.Size size;
			if (Math.max(width, height) <= 256)
				size = ImageCreateVariationParams.Size._256X256;
			else if (Math.max(width, height) <= 512)
				size = ImageCreateVariationParams.Size._512X512;
			else
				size = ImageCreateVariationParams.Size._1024X1024;

			ImageCreateVariationParams req = defaultVariationRequest.toBuilder() //
					.image(toMultipartStream(prompt)) //
					.n(n).size(size).build();
			ImagesResponse resp = endpoint.getClient().images().createVariation(req);
			return OpenAiUtil.readImages(resp.data().get());
		} catch (Exception e) {
			throw OpenAiUtil.toEndpointException(e);
		}
	}

	@Override
	public List<FilePart> createImageEdit(@NonNull FilePart image, @NonNull String prompt, FilePart mask, int n,
			int width, int height) throws EndpointException {

		try {
			ImageEditParams.Size size;
			switch (defaultEditRequest.model().get().value()) {
			case DALL_E_2:
				if (Math.max(width, height) <= 256)
					size = ImageEditParams.Size._256X256;
				else if (Math.max(width, height) <= 512)
					size = ImageEditParams.Size._512X512;
				else
					size = ImageEditParams.Size._1024X1024;
				break;
			default: // Catches gpt-image-1 and future cases too (hopefully)
				if (width == height)
					size = ImageEditParams.Size._1024X1024;
				else if (width > height)
					size = ImageEditParams.Size._1536X1024;
				else
					size = ImageEditParams.Size._1024X1536;
				break;
			}

			Builder b = defaultEditRequest.toBuilder().image(toMultipartImage(image)).prompt(prompt);
			if (mask != null)
				b.mask(toMultipartStream(mask));
			b.n(n).size(size);

			ImagesResponse resp = endpoint.getClient().images().edit(b.build());
			return OpenAiUtil.readImages(resp.data().get());
		} catch (Exception e) {
			throw OpenAiUtil.toEndpointException(e);
		}
	}

	// TODO Uncomment this and all others once we know how to use
	// imageOfInputStreams() it might work already TBH

//	public List<FilePart> createImageEdit(@NonNull List<BufferedImage> images, @NonNull String prompt,
//			BufferedImage mask, int n, int width, int height) throws IOException {
//
//		return createImageEdit(toPart(images), prompt, toPart(mask), n, width, height);
//	}

//	public List<FilePart> createImageEdit(@NonNull List<FilePart> images, @NonNull String prompt, FilePart mask, int n,
//			int width, int height) {
//
//		Builder b = defaultEditRequest.toBuilder();
//		if (images.size() == 1) {
//			b.image(toMultipartImage(images.get(0)));
//		} else {
//			List<MultipartField<InputStream>> streams = new ArrayList<>(images.size());
//			for (FilePart image : images)
//				streams.add(toMultipartStream(image));
//			b.imageOfInputStreams(streams);
//		}
//
//		ImageEditParams.Size size;
//		switch (OptionalUtil.get(defaultEditRequest.model()).value()) {
//		case DALL_E_2:
//			if (Math.max(width, height) <= 256)
//				size = ImageEditParams.Size._256X256;
//			else if (Math.max(width, height) <= 512)
//				size = ImageEditParams.Size._512X512;
//			else
//				size = ImageEditParams.Size._1024X1024;
//			break;
//		default: // Catches gpt-image-1 and future cases too (hopefully)
//			if (width == height)
//				size = ImageEditParams.Size._1024X1024;
//			else if (width > height)
//				size = ImageEditParams.Size._1536X1024;
//			else
//				size = ImageEditParams.Size._1024X1536;
//			break;
//		}
//		b.size(size);
//
//		ImagesResponse resp = endpoint.getClient().images().edit(b.build());
//		return readImages(OptionalUtil.get(resp.data()));
//	}

	private MultipartField<InputStream> toMultipartStream(FilePart prompt) throws IOException {
		return MultipartField.<InputStream>builder() //
				.filename(prompt.getName()) //
				.contentType(prompt.getMimeType()) //
				.value(prompt.getInputStream()).build();
	}

	private MultipartField<com.openai.models.images.ImageEditParams.Image> toMultipartImage(FilePart imagePart)
			throws IOException {

		return MultipartField.<ImageEditParams.Image>builder() //
				.filename(imagePart.getName()) //
				.contentType(imagePart.getMimeType()) //
				.value(ImageEditParams.Image.ofInputStream(imagePart.getInputStream())).build();
	}
}
