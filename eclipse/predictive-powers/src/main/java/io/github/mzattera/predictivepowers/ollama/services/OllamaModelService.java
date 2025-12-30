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

package io.github.mzattera.predictivepowers.ollama.services;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mzattera.predictivepowers.services.AbstractModelService;
import io.github.mzattera.predictivepowers.services.ModelService;
import io.github.mzattera.predictivepowers.util.SimpleTokenizer;
import lombok.Getter;
import lombok.NonNull;

/**
 * This class provides {@link ModelService}s for Hugging Face.
 * 
 * The class is tread-safe.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class OllamaModelService extends AbstractModelService {

	private final static Logger LOG = LoggerFactory.getLogger(OllamaModelService.class);

	/** Tokeniser to use when no other tokeniser is found */
	public static final Tokenizer FALLBACK_TOKENIZER = new SimpleTokenizer(2.5);

	@NonNull
	@Getter
	protected final OllamaEndpoint endpoint;

	protected OllamaModelService(OllamaEndpoint endpoint) {
		this.endpoint = endpoint;
	}

	@Override
	public ModelMetaData get(@NonNull String model) {
		ModelMetaData result = data.get(model);
		if (result == null) {
			result = load(model);
			put(model, result);
			return result;
		}
		return result;
	}

	/**
	 * Tries to build model meta data by using DLJ library to get tokeniser and
	 * configuration files. It then tries some heuristic to parse the files.
	 * 
	 * @param model
	 * @return Model meta data filled as much as possible
	 */
	private static ModelMetaData load(String model) {
		// TODO URGENT implement
		return ModelMetaData.builder().model(model).build();
	}

	@Override
	public List<String> listModels() {
		try {
			return endpoint.getClient().tags().getModels().stream().map(m -> m.getName()).collect(Collectors.toList());
		} catch (Exception e) {
			throw OllamaUtil.toEndpointException(e);
		}
	}
}
