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

package io.github.mzattera.predictivepowers.examples;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import io.github.mzattera.predictivepowers.AiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.ImageGenerationService;
import io.github.mzattera.predictivepowers.services.messages.FilePart;
import io.github.mzattera.predictivepowers.util.ImageUtil;

public class ImageGenerationExample {

	private final static String PROMPT = "full body male cyborg shaggy long gray hair short beard green eyes|"
			+ " shimmering gold metal| lighning| full-length portrait| detailed face|"
			+ " symmetric| steampunk| cyberpunk| cyborg| intricate detailed| to scale|"
			+ " hyperrealistic| cinematic lighting| digital art| concept art|" + " mdjrny-v4 style";

	public static void main(String[] args) throws Exception {

		try (
				// Uncomment this to use DALL-E
				AiEndpoint endpoint = new OpenAiEndpoint();
				ImageGenerationService svc = endpoint.getImageGenerationService("dall-e-2");

		// Uncomment this to use Openjourney
//				AiEndpoint endpoint = new HuggingFaceEndpoint();
//				ImageGenerationService svc = endpoint.getImageGenerationService();
		) {
			// Generates image
			FilePart img = svc.createImage(PROMPT, 1, 1024, 1024).get(0);

			// Saves it in a temporary file
			save(img);
		}
	}

	private static void save(FilePart img) throws IOException {
		File tmp = File.createTempFile("GenAI", ".jpg");
		try (InputStream s = img.getInputStream()) {
			ImageUtil.toFile(ImageUtil.fromBytes(s), tmp);
		}
		System.out.println("Image saved as: " + tmp.getCanonicalPath());
	}
}
