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
package io.github.mzattera.predictivepowers.deepseek;

import java.util.ArrayList;
import java.util.List;

import io.github.mzattera.predictivepowers.EndpointException;
import io.github.mzattera.predictivepowers.services.AbstractModelService;
import io.github.mzattera.predictivepowers.services.ModelService;
import io.github.mzattera.predictivepowers.services.ModelService.ModelMetaData.Modality;
import io.github.mzattera.predictivepowers.util.SimpleTokenizer;
import lombok.Getter;
import lombok.NonNull;

/**
 * This class provides {@link ModelService}s for DeepSeek endpoint.
 * 
 * The class is tread-safe and uses a single data repository for all of its
 * instances.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class DeepSeekModelService extends AbstractModelService {

	/** Tokenizer to use when no other tokenizer is found */
	public static final Tokenizer FALLBACK_TOKENIZER = new SimpleTokenizer(0.3);
	
	@Getter
	protected final @NonNull DeepSeekEndpoint endpoint;

	private final static List<ModelMetaData> MODELS = new ArrayList<>();
	static {
		MODELS.add(ModelMetaData.builder().model("deepseek-chat") //
				.inputMode(Modality.TEXT) //
				.outputMode(Modality.TEXT) //
				.contextSize(128 * 1024) //
				.maxNewTokens(8 * 1024) //
				.tokenizer(FALLBACK_TOKENIZER).build());
		MODELS.add(ModelMetaData.builder().model("deepseek-reasoner") //
				.inputMode(Modality.TEXT) //
				.outputMode(Modality.TEXT) //
				.contextSize(128 * 1024) //
				.maxNewTokens(64 * 1024) //
				.tokenizer(FALLBACK_TOKENIZER).build());
	}

	protected DeepSeekModelService(DeepSeekEndpoint endpoint) {
		super(MODELS);
		this.endpoint = endpoint;
	}

	@Override
	public List<String> listModels() throws EndpointException {
		return List.copyOf(data.keySet());
	}
}
