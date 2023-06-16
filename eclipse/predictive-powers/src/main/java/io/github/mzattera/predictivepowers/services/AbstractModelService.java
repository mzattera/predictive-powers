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

package io.github.mzattera.predictivepowers.services;

import java.util.Map;

import io.github.mzattera.predictivepowers.services.ModelService.ModelData;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Abstract {@link ModelService} that can be sub-classed to create other
 * services faster (hopefully).
 * 
 * The constructor requires a Map to map models into their data, this allows
 * more flexibility for subclasses to implement a singleton repository of data,
 * using a thread safe Map, etc.
 * 
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@RequiredArgsConstructor
public abstract class AbstractModelService implements ModelService {

	@NonNull protected final Map<String, ModelData> data;

	/**
	 * 
	 * @param model
	 * @return {@link ModelData} for given model, or null if it cannot be found.
	 */
	public ModelData get(@NonNull String model) {
		return data.get(model);
	}

	@Override
	public ModelData put(@NonNull String model, @NonNull ModelData data) {
		return this.data.put(model, data);
	}

	@Override
	public ModelData remove(@NonNull String model) {
		return data.remove(model);
	}

	@Override
	public Tokenizer getTokenizer(@NonNull String model) throws IllegalArgumentException {
		ModelData data = get(model);
		if ((data == null) || (data.getTokenizer() == null))
			throw new IllegalArgumentException(
					"No tokenizer found for model " + model + ". Consider registering model data");
		return data.getTokenizer();
	}

	@Override
	public Tokenizer getTokenizer(@NonNull String model, Tokenizer def) {
		ModelData data = get(model);
		if ((data == null) || (data.getTokenizer() == null))
			return def;
		return data.getTokenizer();
	}

	@Override
	public int getContextSize(@NonNull String model) throws IllegalArgumentException {
		ModelData data = get(model);
		if ((data == null) || (data.getContextSize() == null))
			throw new IllegalArgumentException(
					"No context size defined for model " + model + ". Consider registering model data");
		return data.getContextSize();
	}

	@Override
	public int getContextSize(@NonNull String model, int def) {
		ModelData data = get(model);
		if ((data == null) || (data.getContextSize() == null))
			return def;
		return data.getContextSize();
	}
}
