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
package io.github.mzattera.predictivepowers.huggingface.client;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.huggingface.client.multimodal.TextToImageRequest;
import io.github.mzattera.predictivepowers.huggingface.client.nlp.TextClassificationRequest;
import io.github.mzattera.predictivepowers.huggingface.client.nlp.TextClassificationResponse;
import io.github.mzattera.predictivepowers.huggingface.client.nlp.TextGenerationRequest;
import io.github.mzattera.predictivepowers.huggingface.client.nlp.TextGenerationResponse;
import io.github.mzattera.util.ImageUtil;

public class HuggingFaceClientTest {

//	@Test
	public void test01() {

		try (HuggingFaceClient cli = new HuggingFaceClient()) {

			TextClassificationRequest req = new TextClassificationRequest();
			req.getInputs().add("Life sucks!");
			req.getInputs().add("I like my new kangaroo.");

			// Classify some sentences.
			List<List<TextClassificationResponse>> resp = cli
					.textClassification("distilbert-base-uncased-finetuned-sst-2-english", req);
			for (List<TextClassificationResponse> lsa : resp) {
				System.out.println("---");
				for (TextClassificationResponse ls : lsa) {
					System.out.println("\t" + ls.toString());
				}
			}
		}
	}

	@Test
	public void test03() {

		try (HuggingFaceClient cli = new HuggingFaceClient()) {

			TextGenerationRequest req = new TextGenerationRequest();
			req.getInputs().add("How high is Mt. Everest?");
//			req.getInputs().add("I like my new kangaroo.");
			req.getParameters().setMaxNewTokens(15);
			req.getParameters().setReturnFullText(false);

			// Prompt completion
			List<List<TextGenerationResponse>> resp = cli.textGeneration("gpt2", req);
			for (List<TextGenerationResponse> lsa : resp) {
				System.out.println("---");
				for (TextGenerationResponse ls : lsa) {
					System.out.println("\t" + ls.toString());
				}
			}
		}
	}

//	@Test
	public void test02() throws IOException {

		try (HuggingFaceClient cli = new HuggingFaceClient()) {

			TextToImageRequest req = new TextToImageRequest();
			req.setInputs(
					"mdjrny-v4 Detailed and realistic photography of a woman next to a pole, soft natural lighting, hyper realistic, 85mm lens, magical photography, dramatic lighting, photo realism, ultra-detailed, Cinestill 800T");

			// Classify some sentences.
			BufferedImage resp = cli.textToImage("prompthero/openjourney-v4", req);
			File temp = File.createTempFile("OpenJourney_", ".jpg");
			ImageUtil.toFile(temp, resp);
			System.out.println("Image saved to: " + temp.getCanonicalPath());
		}
	}
}
