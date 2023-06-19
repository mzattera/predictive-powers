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

package io.github.mzattera.predictivepowers.openai.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.mzattera.predictivepowers.openai.client.models.Model;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.AbstractModelService;
import io.github.mzattera.predictivepowers.services.ChatMessage;
import io.github.mzattera.predictivepowers.services.ModelService;
import io.github.mzattera.predictivepowers.util.tikoken.ChatFormatDescriptor;
import io.github.mzattera.predictivepowers.util.tikoken.Encoding;
import io.github.mzattera.predictivepowers.util.tikoken.GPT3Tokenizer;
import io.github.mzattera.predictivepowers.util.tikoken.TokenCount;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * This class provides {@link ModelService}s for OpenAI.
 * 
 * The class is tread-safe and uses a single data repository for all of its
 * instances.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class OpenAiModelService extends AbstractModelService {

	@Getter
	@Setter
	@Builder
//	@NoArgsConstructor
	@RequiredArgsConstructor
//	@AllArgsConstructor
	@ToString
	public static class OpenAiTokenizer implements Tokenizer {

		@NonNull
		private final GPT3Tokenizer tokenizer;
		private final ChatFormatDescriptor chatFormat;

		/**
		 * 
		 */
		@Override
		public int count(@NonNull String text) {
			return TokenCount.fromString(text, tokenizer);
		}

		@Override
		public int count(@NonNull ChatMessage msg) {
			List<ChatMessage> l = new ArrayList<>();
			l.add(msg);
			return count(l);
		}

		@Override
		public int count(@NonNull List<ChatMessage> msgs) {
			return TokenCount.fromMessages(msgs, tokenizer, chatFormat);
		}
	}

	/**
	 * Maps each model into its context size.
	 */
	private final static Map<String, Integer> CONTEXT_SIZES = new HashMap<>();
	static {
		CONTEXT_SIZES.put("ada", 2049);
		CONTEXT_SIZES.put("babbage", 2049);
		CONTEXT_SIZES.put("code-cushman-001", 2048);
		CONTEXT_SIZES.put("code-cushman-002", 2048);
		CONTEXT_SIZES.put("code-davinci-001", 8001);
		CONTEXT_SIZES.put("code-davinci-002", 8001);
		CONTEXT_SIZES.put("code-search-ada-code-001", 2046);
		CONTEXT_SIZES.put("code-search-ada-text-001", 2046);
		CONTEXT_SIZES.put("code-search-babbage-code-001", 2046);
		CONTEXT_SIZES.put("code-search-babbage-text-001", 2046);
		CONTEXT_SIZES.put("curie", 2049);
		CONTEXT_SIZES.put("davinci", 2049);
		CONTEXT_SIZES.put("gpt-3.5-turbo", 4096);
		CONTEXT_SIZES.put("gpt-3.5-turbo-16k", 16384);
		CONTEXT_SIZES.put("gpt-3.5-turbo-0301", 4096);
		CONTEXT_SIZES.put("gpt-3.5-turbo-0613", 4096);
		CONTEXT_SIZES.put("gpt-3.5-turbo-16k-0613", 16384);
		CONTEXT_SIZES.put("gpt-4", 8192);
		CONTEXT_SIZES.put("gpt-4-32k", 32768);
		CONTEXT_SIZES.put("text-ada-001", 2049);
		CONTEXT_SIZES.put("text-babbage-001", 2049);
		CONTEXT_SIZES.put("text-curie-001", 2049);
		CONTEXT_SIZES.put("text-davinci-002", 4093); // Documentation says 4097 but it is incorrect
		CONTEXT_SIZES.put("text-davinci-003", 4093);
		CONTEXT_SIZES.put("text-embedding-ada-002", 8191);
		CONTEXT_SIZES.put("text-search-ada-doc-001", 2046);
		CONTEXT_SIZES.put("text-search-ada-query-001", 2046);
		CONTEXT_SIZES.put("text-search-babbage-doc-001", 2046);
		CONTEXT_SIZES.put("text-search-babbage-query-001", 2046);
		CONTEXT_SIZES.put("text-search-curie-doc-001", 2046);
		CONTEXT_SIZES.put("text-search-curie-query-001", 2046);
		CONTEXT_SIZES.put("text-search-davinci-doc-001", 2046);
		CONTEXT_SIZES.put("text-search-davinci-query-001", 2046);
		CONTEXT_SIZES.put("text-similarity-ada-001", 2046);
		CONTEXT_SIZES.put("text-similarity-babbage-001", 2046);
		CONTEXT_SIZES.put("text-similarity-curie-001", 2046);
		CONTEXT_SIZES.put("text-similarity-davinci-001", 2046);
	}

	/**
	 * Single instance of the data Map, shared by all instances of this model
	 * service class.
	 */
	private final static Map<String, ModelData> data = new ConcurrentHashMap<>();
	static {
		for (String model : CONTEXT_SIZES.keySet()) {
			Tokenizer tok = null;
			try { // Get a tokenizer, if possible
				GPT3Tokenizer tokenizer = new GPT3Tokenizer(Encoding.forModel(model));
				ChatFormatDescriptor chatFormat = ChatFormatDescriptor.forModel(model);
				tok = new OpenAiTokenizer(tokenizer, chatFormat);
			} catch (IllegalArgumentException e) { // No tokenizer found for the model
			}

			data.put(model, ModelData.builder().contextSize(CONTEXT_SIZES.get(model)).tokenizer(tok).build());
		}
	}

	@NonNull
	@Getter
	protected final OpenAiEndpoint endpoint;

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

	public OpenAiModelService(OpenAiEndpoint endpoint) {
		super(data);
		this.endpoint = endpoint;
	}

	@Override
	public List<String> listModels() {
		List<Model> l = endpoint.getClient().listModels();
		List<String> result = new ArrayList<>(l.size());
		for (Model m : l)
			result.add(m.getId());
		return result;
	}
}
