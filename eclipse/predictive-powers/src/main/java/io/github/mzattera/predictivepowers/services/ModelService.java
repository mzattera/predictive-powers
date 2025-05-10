/*
 * Copyright 2023-2024 Massimiliano "Maxi" Zattera
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.mzattera.predictivepowers.services.ModelService.ModelMetaData.Mode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

/**
 * Instances of this interface provide services to list available models end
 * expose their relevant parameters (e.g. context size, tokenizers, etc.).
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public interface ModelService extends AiService {

	/**
	 * As for many models parameters are expressed in terms of tokens, it is
	 * necessary to "tokenize" text; the way this happens is model-specific as each
	 * family of models has its own tokenizer.
	 * 
	 * @author Massimiliano "Maxi" Zattera
	 *
	 */
	public interface Tokenizer {

		/**
		 * 
		 * @param text
		 * @return Number of tokens in given text.
		 */
		int count(@NonNull String text);
	}

	/**
	 * Data Associated to a model.
	 * 
	 * Notice some of the fields might be missing or make no sense for a model.
	 * 
	 * @author Massimiliano "Maxi" Zattera.
	 *
	 */
	@Getter
	@ToString
	public static class ModelMetaData {

		/**
		 * Input modalities a model supports.
		 */
		public enum Mode {
			TEXT, IMAGE, AUDIO, EMBEDDINGS
		}

		/**
		 * The model this metadata refers to.
		 */
		protected @NonNull String model;

		/**
		 * A tokenizer for this (text) model (if any).
		 */
		protected Tokenizer tokenizer;

		/**
		 * Context size for a model (namely for GPT models).
		 * 
		 * Null if unlimited.
		 */
		protected Integer contextSize;

		/**
		 * Some models (namely GPT-4), even if with a huge context size, return only a
		 * limited amount of tokens, this must be considered or it will cause errors.
		 * 
		 * Null if unlimited.
		 */
		protected Integer maxNewTokens;

		/** The input modes this model supports */
		protected final List<Mode> inputModes = new ArrayList<>();

		/** The output modes this model supports */
		protected final List<Mode> outputModes = new ArrayList<>();

		public boolean supportsImageInput() {
			return inputModes.contains(Mode.IMAGE);
		}

		public boolean supportsAudioInput() {
			return inputModes.contains(Mode.AUDIO);
		}

		public boolean supportsImageOutput() {
			return outputModes.contains(Mode.IMAGE);
		}

		public boolean supportsAudioOutput() {
			return outputModes.contains(Mode.AUDIO);
		}

		/**
		 * Creates metadata for a model that supports only text input and output.
		 */
		public ModelMetaData(@NonNull String model) {
			this(model, null, null, null, new Mode[] { Mode.TEXT }, new Mode[] { Mode.TEXT });
		}

		/**
		 * Creates metadata for a model that supports only text input and output.
		 */
		public ModelMetaData(@NonNull String model, Tokenizer tokenizer) {
			this(model, tokenizer, null, null, new Mode[] { Mode.TEXT }, new Mode[] { Mode.TEXT });
		}

		/**
		 * Creates metadata for a model that supports only text input and output.
		 */
		public ModelMetaData(@NonNull String model, Tokenizer tokenizer, Integer contextSize, Integer maxNewTokens) {
			this(model, tokenizer, Integer.valueOf(contextSize), Integer.valueOf(maxNewTokens),
					new Mode[] { Mode.TEXT }, new Mode[] { Mode.TEXT });
		}

		/**
		 * 
		 * @param model
		 * @param tokenizer
		 * @param contextSize
		 * @param maxNewTokens
		 * @param supportsImages True if this model supports images as input, otherwise
		 *                       it is supposed the model has only text as input and
		 *                       output.
		 */
		public ModelMetaData(@NonNull String model, Tokenizer tokenizer, Integer contextSize, Integer maxNewTokens,
				boolean supportsImages) {
			this(model, tokenizer, contextSize, maxNewTokens, new Mode[] { Mode.TEXT },
					(supportsImages ? new Mode[] { Mode.TEXT, Mode.IMAGE } : new Mode[] { Mode.TEXT }));
		}

		public ModelMetaData(@NonNull String model, Tokenizer tokenizer, Integer contextSize, Integer maxNewTokens,
				Mode[] inputModes, Mode[] outputModes) {
			this.model = model;
			this.tokenizer = tokenizer;
			this.contextSize = contextSize;
			this.maxNewTokens = maxNewTokens;
			Collections.addAll(this.inputModes, inputModes);
			Collections.addAll(this.outputModes, outputModes);
		}

		public ModelMetaData(@NonNull String model, Tokenizer tokenizer, Integer contextSize, Integer maxNewTokens,
				List<Mode> inputModes, List<Mode> outputModes) {
			this.model = model;
			this.tokenizer = tokenizer;
			this.contextSize = contextSize;
			this.maxNewTokens = maxNewTokens;
			this.inputModes.addAll(inputModes);
			this.outputModes.addAll(outputModes);
		}
	}

	/**
	 * @return List IDs for all available models at this endpoint.
	 */
	List<String> listModels();

	/**
	 * 
	 * @param model
	 * @return {@link ModelMetaData} for given model, or null if it cannot be found.
	 */
	ModelMetaData get(@NonNull String model);

	/**
	 * Sets the {@link ModelMetaData} for given model.
	 * 
	 * @return the previous data for given model, or null if there was no mapping
	 *         for key.
	 */
	ModelMetaData put(@NonNull String model, @NonNull ModelMetaData data);

	/**
	 * Deletes the {@link ModelMetaData} for given model.
	 * 
	 * @return Previously set ModelData, if any.
	 */
	ModelMetaData remove(@NonNull String model);

	/**
	 * 
	 * @param model
	 * @return {@link Tokenizer} to use with given model, based on available model
	 *         data.
	 * @throws IllegalArgumentException If a tokenizer is not defined.
	 */
	Tokenizer getTokenizer(@NonNull String model) throws IllegalArgumentException;

	/**
	 * 
	 * @param model
	 * @param def   Tokenizer to return by default
	 * @return {@link Tokenizer} to use with given model, based on available model
	 *         data. If no tokenizer is define for the model, it returns default
	 *         one.
	 */
	Tokenizer getTokenizer(@NonNull String model, Tokenizer def);

	/**
	 * 
	 * @param model
	 * @return Context size for given model.
	 * @throws IllegalArgumentException if no context size was defined for given
	 *                                  model.
	 */
	int getContextSize(@NonNull String model) throws IllegalArgumentException;

	/**
	 * 
	 * @param model
	 * @param def   Default context size.
	 * @return Context size for given model, or the default one if no context size
	 *         was defined for given model.
	 */
	int getContextSize(@NonNull String model, int def);

	/**
	 * 
	 * @param model
	 * @return Maximum number of new tokens a model can generate; some models have
	 *         this limitation in addition to max context size.
	 */
	int getMaxNewTokens(@NonNull String model);

	/**
	 * 
	 * @param model
	 * @return Maximum number of new tokens a model can generate; some models have
	 *         this limitation in addition to max context size.
	 */
	int getMaxNewTokens(@NonNull String model, int def);

	/**
	 * 
	 * @param model
	 * @return Input modes supported by the model.
	 */
	List<Mode> getInputModes(@NonNull String model);

	/**
	 * 
	 * @param model
	 * @return Output modes supported by the model.
	 */
	List<Mode> getOutputModes(@NonNull String model);
}