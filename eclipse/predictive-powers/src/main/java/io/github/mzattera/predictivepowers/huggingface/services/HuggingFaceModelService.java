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

package io.github.mzattera.predictivepowers.huggingface.services;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mzattera.predictivepowers.huggingface.endpoint.HuggingFaceEndpoint;
import io.github.mzattera.predictivepowers.services.AbstractModelService;
import io.github.mzattera.predictivepowers.services.ChatMessage;
import io.github.mzattera.predictivepowers.services.ModelService;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * This class provides {@link ModelService}s for Hugging Face.
 * 
 * The class is tread-safe and uses a single data repository for all of its
 * instances.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class HuggingFaceModelService extends AbstractModelService {

	@Getter
	@Setter
	@Builder
//	@NoArgsConstructor
	@RequiredArgsConstructor
//	@AllArgsConstructor
	@ToString
	public static class HuggingFaceTokenizer implements Tokenizer {

		@NonNull
		private final ai.djl.huggingface.tokenizers.HuggingFaceTokenizer tokenizer;

		/**
		 * 
		 */
		@Override
		public int count(@NonNull String text) {
			return tokenizer.encode(text).getTokens().length;
		}

		@Override
		public int count(@NonNull ChatMessage msg) {
			return count(msg.getContent());
		}

		@Override
		public int count(@NonNull List<ChatMessage> msgs) {
			int result = 0;
			for (ChatMessage m : msgs)
				result += count(m.getContent());
			return result;
		}
	}

	private final static Logger LOG = LoggerFactory.getLogger(HuggingFaceModelService.class);

	/**
	 * Single instance of the data Map, shared by all instances of this model
	 * service class.
	 */
	private final static Map<String, ModelData> data = new ConcurrentHashMap<>();

	@NonNull
	@Getter
	protected final HuggingFaceEndpoint endpoint;

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
		throw new UnsupportedOperationException();
	}

	public HuggingFaceModelService(HuggingFaceEndpoint endpoint) {
		super(data);
		this.endpoint = endpoint;
	}

	@Override
	public ModelData get(@NonNull String model) {
		ModelData result = data.get(model);
		if (result == null)
			return createData(model);
		return result;
	}

	/**
	 * Tries to create ModelData for given model. The newly created data is saved in
	 * the internal Map to be accessible for subsequent calls.
	 * 
	 * @param model
	 * @return The newly created data, or null if it cannot be created.
	 */
	private ModelData createData(@NonNull String model) {

		// Uses DJL services to download proper tokenizer
		ai.djl.huggingface.tokenizers.HuggingFaceTokenizer tokenizer = null;
		try {
			tokenizer = ai.djl.huggingface.tokenizers.HuggingFaceTokenizer.newInstance(model);
		} catch (Exception e) {
			LOG.error("Cannot retrieve Hugging Face Tokenizer for model " + model, e);
			return null;
		}

		ModelData result = ModelData.builder().tokenizer(new HuggingFaceTokenizer(tokenizer)).build();
		put(model, result);
		return result;
	}

	/**
	 * Unsupported, as there are too many.
	 */
	@Override
	public List<String> listModels() {
		throw new UnsupportedOperationException();
	}
}
