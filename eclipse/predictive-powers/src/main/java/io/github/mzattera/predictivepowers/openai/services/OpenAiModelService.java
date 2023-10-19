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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;

import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.chat.Function;
import io.github.mzattera.predictivepowers.openai.client.models.Model;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.AbstractModelService;
import io.github.mzattera.predictivepowers.services.ChatMessage;
import io.github.mzattera.predictivepowers.services.ChatMessage.FunctionCall;
import io.github.mzattera.predictivepowers.services.ModelService;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import xyz.felh.openai.completion.chat.ChatMessageRole;
import xyz.felh.openai.jtokkit.utils.TikTokenUtils;

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

	private final static Logger LOG = LoggerFactory.getLogger(OpenAiModelService.class);

	/**
	 * Tokenizer for OpenAI models.
	 * 
	 * @author Massimiliano "Maxi" Zattera
	 *
	 */
	@Getter
	@Setter
	@Builder
//	@NoArgsConstructor
	@RequiredArgsConstructor
//	@AllArgsConstructor
	@ToString
	public static class OpenAiTokenizer implements Tokenizer {

		@Getter
		@NonNull
		private final String model;

		@Override
		public int count(@NonNull String text) {
			return TikTokenUtils.tokens(model, text);
		}

		@Override
		public int count(@NonNull ChatMessage msg) {
			List<ChatMessage> l = new ArrayList<>();
			l.add(msg);
			return count(l);
		}

		@Override
		public int count(@NonNull List<ChatMessage> msgs) {
			List<xyz.felh.openai.completion.chat.ChatMessage> l = new ArrayList<>(msgs.size());
			for (ChatMessage m : msgs)
				l.add(translate(m));
			return TikTokenUtils.tokens(model, l);
		}

		/**
		 * Counts tokens required to encode given request.
		 */
		public int count(@NonNull ChatCompletionsRequest req) {
			return count(req.getMessages()) + countFunctions(req.getFunctions());
		}

		/**
		 * Counts tokens required to encode given list of functions.
		 */
		public int countFunctions(List<Function> functions) {
			if ((functions == null) || (functions.size() == 0))
				return 0;

			List<xyz.felh.openai.completion.chat.func.Function> l = new ArrayList<>(functions.size());
			for (Function f : functions)
				l.add(translate(f));
			return TikTokenUtils.tokens(model, null, l);
		}

		private xyz.felh.openai.completion.chat.ChatMessage translate(ChatMessage m) {

			ChatMessageRole role;
			switch (m.getRole()) {
			case USER:
				role = ChatMessageRole.USER;
				break;
			case BOT:
				role = ChatMessageRole.ASSISTANT;
				break;
			case SYSTEM:
				role = ChatMessageRole.SYSTEM;
				break;
			case FUNCTION:
				role = ChatMessageRole.FUNCTION;
				break;
			default:
				throw new IllegalArgumentException(); // Guard
			}

			xyz.felh.openai.completion.chat.ChatMessage result = new xyz.felh.openai.completion.chat.ChatMessage(role,
					m.getContent(), m.getName());

			FunctionCall call = m.getFunctionCall();
			if (call != null) {
				String arguments = null;
				try {
					arguments = OpenAiClient.getJsonMapper().writerWithDefaultPrettyPrinter()
							.writeValueAsString(call.getArguments());
				} catch (JsonProcessingException e) {
					LOG.error("Error while parsing function call arguments", e);
				}
				result.setFunctionCall(
						new xyz.felh.openai.completion.chat.func.FunctionCall(call.getName(), arguments));
			}

			return result;
		}

		private xyz.felh.openai.completion.chat.func.Function translate(Function f) {
			JSONObject json = JSONObject
					.parseObject(Function.getSchemaGenerator().generateJsonSchema(f.getParameters()).toString());
			return new xyz.felh.openai.completion.chat.func.Function(f.getName(), f.getDescription(), json);
		}
	}

	/**
	 * Maps each model into its context size.
	 */
	final static Map<String, Integer> CONTEXT_SIZES = new HashMap<>();
	static {
		CONTEXT_SIZES.put("ada", 2049);
		CONTEXT_SIZES.put("babbage", 2049);
		CONTEXT_SIZES.put("babbage-002", 16384);
		CONTEXT_SIZES.put("code-search-ada-code-001", 2046);
		CONTEXT_SIZES.put("code-search-ada-text-001", 2046);
		CONTEXT_SIZES.put("code-search-babbage-code-001", 2046);
		CONTEXT_SIZES.put("code-search-babbage-text-001", 2046);
		CONTEXT_SIZES.put("curie", 2049);
		CONTEXT_SIZES.put("davinci", 2049);
		CONTEXT_SIZES.put("davinci-002", 16384);
		CONTEXT_SIZES.put("gpt-3.5-turbo", 4096);
		CONTEXT_SIZES.put("gpt-3.5-turbo-16k", 16384);
		CONTEXT_SIZES.put("gpt-3.5-turbo-instruct", 4096);
		CONTEXT_SIZES.put("gpt-3.5-turbo-instruct-0914", 4096);
		CONTEXT_SIZES.put("gpt-3.5-turbo-0613", 4096);
		CONTEXT_SIZES.put("gpt-3.5-turbo-16k-0613", 16384);
		CONTEXT_SIZES.put("gpt-3.5-turbo-0301", 4096);
		CONTEXT_SIZES.put("gpt-4", 8192);
		CONTEXT_SIZES.put("gpt-4-0613", 8192);
		CONTEXT_SIZES.put("gpt-4-32k", 32768);
		CONTEXT_SIZES.put("gpt-4-32k-0613", 32768);
		CONTEXT_SIZES.put("gpt-4-0314", 8192);
		CONTEXT_SIZES.put("gpt-4-32k-0314", 32768);
		CONTEXT_SIZES.put("text-ada-001", 2049);
		CONTEXT_SIZES.put("text-babbage-001", 2049);
		CONTEXT_SIZES.put("text-curie-001", 2049);
		CONTEXT_SIZES.put("text-davinci-001", 2049); 
		CONTEXT_SIZES.put("text-davinci-002", 4093); // Documentation says 4097 but it is incorrect
		CONTEXT_SIZES.put("text-davinci-003", 4093);
		CONTEXT_SIZES.put("text-embedding-ada-002", 8192);
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
	 * 
	 * To add or remove models, act here.
	 */
	private final static Map<String, ModelData> data = new ConcurrentHashMap<>();
	static {
		for (String model : CONTEXT_SIZES.keySet()) {

			// This is a work around since the tokenizer library we use might not have
			// latest gpt-3 or -4 models rolled out every 3 months.
			String modelType = model;
			if (modelType.startsWith("gpt-3.5-turbo-16k")) {
				modelType = "gpt-3.5-turbo-16k";
			} else {
				if (modelType.startsWith("gpt-3.5-turbo")) {
					modelType = "gpt-3.5-turbo";
				} else {
					if (modelType.startsWith("gpt-4-32k")) {
						modelType = "gpt-4-32k";
					} else {
						if (modelType.startsWith("gpt-4")) {
							modelType = "gpt-4";
						}
					}
				}
			}

			Tokenizer tok = new OpenAiTokenizer(modelType);
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
	public OpenAiTokenizer getTokenizer(@NonNull String model) throws IllegalArgumentException {
		return (OpenAiTokenizer) super.getTokenizer(model);
	}

	@Override
	public OpenAiTokenizer getTokenizer(@NonNull String model, Tokenizer def) {
		return (OpenAiTokenizer) super.getTokenizer(model, def);
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
