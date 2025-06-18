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

/**
 * 
 */
package io.github.mzattera.predictivepowers.services;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import io.github.mzattera.predictivepowers.services.messages.FilePart;
import io.github.mzattera.predictivepowers.util.ImageUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Abstract implementation of {@link ImageGenerationService} that can be
 * subclasses to easily (hopefully) create new services.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractImageGenerationService implements ImageGenerationService {

	@Getter
	@Setter
	@NonNull
	private String model;

	@Override
	public List<FilePart> createImageVariation(@NonNull BufferedImage prompt, int n, int width, int height)
			throws IOException {

		return createImageVariation(ImageUtil.toFilePart(prompt), n, width, height);
	}

	@Override
	public List<FilePart> createImageEdit(@NonNull BufferedImage image, @NonNull String prompt, BufferedImage mask,
			int n, int width, int height) throws IOException {

		return createImageEdit(ImageUtil.toFilePart(image), prompt, ImageUtil.toFilePart(mask), n, width, height);
	}

	@Override
	public void close() throws Exception {
	}
}
