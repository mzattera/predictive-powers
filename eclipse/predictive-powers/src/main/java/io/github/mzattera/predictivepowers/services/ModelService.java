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

import com.fasterxml.jackson.core.JsonProcessingException;

import io.github.mzattera.predictivepowers.EndpointException;
import io.github.mzattera.predictivepowers.services.ModelService.ModelMetaData.Modality;
import io.github.mzattera.predictivepowers.services.messages.JsonSchema;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

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
		
		/**
		 * 
		 * @param obj
		 * @return Number of tokens to encode JSON serialization of given object.
		 * @throws JsonProcessingException If an error happens on serialization.
		 */
		int countAsJson(@NonNull Object obj) throws JsonProcessingException;
	}

	/**
	 * Abstract implementation of a Tokenizer.
	 * 
	 * @author Massimiliano "Maxi" Zattera
	 *
	 */
	public abstract class AbstractTokenizer implements Tokenizer {
		
		@Override
		public int countAsJson(@NonNull Object obj) throws JsonProcessingException {
			return count(JsonSchema.JSON_MAPPER.writer().writeValueAsString(obj));
		}			
	}

	/**
	 * Data Associated to a model.
	 * 
	 * Notice some of the fields might be missing or make no sense for a model.
	 * 
	 * This class is unmodifiable, to ensure model services can be made thread safe.
	 * 
	 * @author Massimiliano "Maxi" Zattera.
	 *
	 */
	@Getter
	@SuperBuilder(toBuilder = true) // Allows creating a builder from an existing instance
	@ToString
	public class ModelMetaData {

		/**
		 * Input/Output modalities a model supports.
		 */
		public enum Modality {
			TEXT, IMAGE, AUDIO, EMBEDDINGS, VIDEO
		}

		/**
		 * The model this metadata refers to.
		 */
		private @NonNull String model;

		/**
		 * A tokenizer for this (text) model (if any).
		 */
		private Tokenizer tokenizer;

		/**
		 * Context size for a model (namely for GPT models).
		 * 
		 * Null if unlimited.
		 */
		private Integer contextSize;

		/**
		 * Some models (namely GPT-4), even if with a huge context size, return only a
		 * limited amount of tokens, this must be considered or it will cause errors.
		 * 
		 * Null if unlimited.
		 */
		private Integer maxNewTokens;

		/** The input modes this model supports */
		@Singular("inputMode")
		private final @NonNull List<Modality> inputModes;

		/** The output modes this model supports */
		@Singular("outputMode")
		private final @NonNull List<Modality> outputModes;

		public boolean supportsImageInput() {
			return inputModes.contains(Modality.IMAGE);
		}

		public boolean supportsAudioInput() {
			return inputModes.contains(Modality.AUDIO);
		}

		public boolean supportsImageOutput() {
			return outputModes.contains(Modality.IMAGE);
		}

		public boolean supportsAudioOutput() {
			return outputModes.contains(Modality.AUDIO);
		}
	}

	/**
	 * @return List IDs for all available models at this endpoint.
	 */
	List<String> listModels() throws EndpointException;

	/**
	 * 
	 * @param model
	 * @return {@link ModelMetaData} for given model, or null if it cannot be found.
	 */
	ModelMetaData get(@NonNull String model) throws EndpointException;

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
	List<Modality> getInputModes(@NonNull String model);

	/**
	 * 
	 * @param model
	 * @return Output modes supported by the model.
	 */
	List<Modality> getOutputModes(@NonNull String model);
}