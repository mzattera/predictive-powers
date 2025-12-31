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

package io.github.mzattera.predictivepowers.services;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.mzattera.predictivepowers.EndpointException;
import io.github.mzattera.predictivepowers.services.ModelService.ModelMetaData.Modality;
import lombok.NonNull;

/**
 * Abstract {@link ModelService} that can be sub-classed to create other
 * services faster (hopefully).
 * 
 * This class is thread-safe.
 * 
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public abstract class AbstractModelService implements ModelService {

	@NonNull
	protected final Map<String, ModelMetaData> data = new ConcurrentHashMap<>();

	protected AbstractModelService() {
	}

	/**
	 * Builds a new instance by copying data from given map.
	 */
	protected AbstractModelService(Map<String, ? extends ModelMetaData> data) {
		this.data.putAll(data);
	}

	/**
	 * Builds a new instance by copying data from given map.
	 */
	public AbstractModelService(List<? extends ModelMetaData> models) {
		for (ModelMetaData model:models) 
			this.data.put(model.getModel(), model);
	}

	/**
	 * @return null, as there is no model associated with this service.
	 */
	@Override
	public String getModel() {
		return null;
	}

	/**
	 * Unsupported, as there is no model associated with this service.
	 */
	@Override
	public void setModel(@NonNull String model) {
		throw new EndpointException(new UnsupportedOperationException());
	}

	@Override
	public ModelMetaData get(@NonNull String model) {
		return data.get(model);
	}

	@Override
	public ModelMetaData put(@NonNull String model, @NonNull ModelMetaData data) {
		return this.data.put(model, data);
	}

	@Override
	public ModelMetaData remove(@NonNull String model) {
		return data.remove(model);
	}

	@Override
	public Tokenizer getTokenizer(@NonNull String model) throws IllegalArgumentException {
		ModelMetaData data = get(model);
		if ((data == null) || (data.getTokenizer() == null))
			throw new IllegalArgumentException(
					"No tokenizer found for model " + model + ". Consider registering model data");
		return data.getTokenizer();
	}

	@Override
	public Tokenizer getTokenizer(@NonNull String model, Tokenizer def) {
		ModelMetaData data = get(model);
		if ((data == null) || (data.getTokenizer() == null))
			return def;
		return data.getTokenizer();
	}

	@Override
	public int getContextSize(@NonNull String model) throws IllegalArgumentException {
		ModelMetaData data = get(model);
		if ((data == null) || (data.getContextSize() == null))
			throw new IllegalArgumentException(
					"No context size defined for model " + model + ". Consider registering model data");
		return data.getContextSize();
	}

	@Override
	public int getContextSize(@NonNull String model, int def) {
		ModelMetaData data = get(model);
		if ((data == null) || (data.getContextSize() == null))
			return def;
		return data.getContextSize();
	}

	/**
	 * 
	 * @param model
	 * @return Maximum number of new tokens a model can generate; some models have
	 *         this limitation in addition to max context size.
	 */
	@Override
	public int getMaxNewTokens(@NonNull String model) {
		ModelMetaData data = get(model);
		if ((data == null) || (data.getMaxNewTokens() == null))
			throw new IllegalArgumentException(
					"No maximum response length defined for model " + model + ". Consider registering model data");
		return data.getMaxNewTokens();
	}

	/**
	 * 
	 * @param model
	 * @return Maximum number of new tokens a model can generate; some models have
	 *         this limitation in addition to max context size.
	 */
	@Override
	public int getMaxNewTokens(@NonNull String model, int def) {
		ModelMetaData data = get(model);
		if ((data == null) || (data.getMaxNewTokens() == null))
			return def;
		return data.getMaxNewTokens();
	}

	/**
	 * 
	 * @param model
	 * @return Input modes supported by the model.
	 */
	@Override
	public List<Modality> getInputModes(@NonNull String model) {
		return get(model).getInputModes();
	}

	/**
	 * 
	 * @param model
	 * @return Output modes supported by the model.
	 */
	@Override
	public List<Modality> getOutputModes(@NonNull String model) {
		return get(model).getOutputModes();
	}

	@Override
	public void close() {
	}
}
