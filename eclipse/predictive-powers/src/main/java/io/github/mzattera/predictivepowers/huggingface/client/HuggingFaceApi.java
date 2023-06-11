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

import java.util.List;

import io.github.mzattera.predictivepowers.huggingface.client.multimodal.TextToImageRequest;
import io.github.mzattera.predictivepowers.huggingface.client.nlp.TextClassificationRequest;
import io.github.mzattera.predictivepowers.huggingface.client.nlp.TextClassificationResponse;
import io.github.mzattera.predictivepowers.huggingface.client.nlp.TextGenerationRequest;
import io.github.mzattera.predictivepowers.huggingface.client.nlp.TextGenerationResponse;
import io.github.mzattera.predictivepowers.huggingface.nlp.QuestionAnsweringRequest;
import io.github.mzattera.predictivepowers.huggingface.nlp.QuestionAnsweringResponse;
import io.reactivex.Single;
import lombok.NonNull;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Url;

/**
 * Retrofit definition for Hugging Face Inference API.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public interface HuggingFaceApi {

	/// NLP //////////////////////////////////////////////////

	@POST
	Single<List<List<TextClassificationResponse>>> textClassification(@Url @NonNull String model,
			@Body TextClassificationRequest req);

	@POST
	Single<List<List<TextGenerationResponse>>> textGeneration(@Url @NonNull String model,
			@Body TextGenerationRequest req);

	@POST
	Single<List<List<Double>>> featureExtraction(@Url @NonNull String model, @Body HuggingFaceRequest req);

	@POST
	Single<QuestionAnsweringResponse> questionAnswering(@Url @NonNull String model, @Body QuestionAnsweringRequest req);

	/// MULTIMODAL //////////////////////////////////////////

	@POST
	Single<ResponseBody> textToImage(@Url @NonNull String model, @Body TextToImageRequest req);
}
