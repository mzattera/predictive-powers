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
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;

import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.chat.Function;
import io.github.mzattera.predictivepowers.openai.client.chat.FunctionCall;
import io.github.mzattera.predictivepowers.openai.client.models.Model;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiModelData.SupportedCallType;
import io.github.mzattera.predictivepowers.services.AbstractModelService;
import io.github.mzattera.predictivepowers.services.ChatMessage;
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

	public static class OpenAiModelData extends ModelData {

		public enum SupportedCallType {
			NONE, FUNCTIONS, TOOLS
		}

		/** The type of tools (none, only functions, or tools) a model can call */
		@Getter
		private final SupportedCallType supportedCallType;

		public OpenAiModelData(int contextSize) {
			this(contextSize, SupportedCallType.NONE, null);
		}

		public OpenAiModelData(int contextSize, SupportedCallType supportedCallType) {
			this(contextSize, supportedCallType, null);
		}

		public OpenAiModelData(int contextSize, int maxNewTokens) {
			this(contextSize, SupportedCallType.NONE, maxNewTokens);
		}

		public OpenAiModelData(int contextSize, SupportedCallType supportedCallType, Integer maxNewTokens) {
			super(null, contextSize, maxNewTokens);
			this.supportedCallType = supportedCallType;
		}

		@Override
		public String toString() {
			return "OpenAiModelData [tokenizer=" + getTokenizer() + ", contextSize=" + getContextSize()
					+ ", supportedCallType=" + supportedCallType + "]";
		}
	}

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
			case TOOL: // TODO URGENT add support for tool calls
				role = ChatMessageRole.FUNCTION;
				break;
			default:
				throw new IllegalArgumentException(); // Guard
			}

			xyz.felh.openai.completion.chat.ChatMessage result = new xyz.felh.openai.completion.chat.ChatMessage(role,
					m.getContent(), (m instanceof OpenAiChatMessage ? ((OpenAiChatMessage) m).getName() : null));

			// TODO URGENT add support for tool calls.

			FunctionCall call = (m instanceof OpenAiChatMessage ? ((OpenAiChatMessage) m).getFunctionCall() : null);
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
	 * Maps each model into its parameters. Paramters can be an int, which is then
	 * interpreted as the context size or a Pair<int,SupportedCalls>.
	 */
	final static Map<String, OpenAiModelData> MODEL_CONFIG = new HashMap<>();
	static {
		MODEL_CONFIG.put("babbage-002", new OpenAiModelData(16384));
		MODEL_CONFIG.put("davinci-002", new OpenAiModelData(16384));
		MODEL_CONFIG.put("gpt-3.5-turbo", new OpenAiModelData(4096, SupportedCallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-3.5-turbo-16k", new OpenAiModelData(16384, SupportedCallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-3.5-turbo-instruct", new OpenAiModelData(4096));
		MODEL_CONFIG.put("gpt-3.5-turbo-instruct-0914", new OpenAiModelData(4096));
		MODEL_CONFIG.put("gpt-3.5-turbo-1106", new OpenAiModelData(16385, SupportedCallType.TOOLS, 4096));
		MODEL_CONFIG.put("gpt-3.5-turbo-0613", new OpenAiModelData(4096, SupportedCallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-3.5-turbo-16k-0613", new OpenAiModelData(16384, SupportedCallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-3.5-turbo-0301", new OpenAiModelData(4096));
		MODEL_CONFIG.put("gpt-4", new OpenAiModelData(8192, SupportedCallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-4-0613", new OpenAiModelData(8192, SupportedCallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-4-32k", new OpenAiModelData(32768, SupportedCallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-4-32k-0613", new OpenAiModelData(32768, SupportedCallType.FUNCTIONS));
		MODEL_CONFIG.put("gpt-4-32k-0314", new OpenAiModelData(32768));
		MODEL_CONFIG.put("gpt-4-1106-preview", new OpenAiModelData(128000, SupportedCallType.TOOLS, 4096));
		MODEL_CONFIG.put("gpt-4-vision-preview", new OpenAiModelData(128000, 4096));
		MODEL_CONFIG.put("text-embedding-ada-002", new OpenAiModelData(8192));
		MODEL_CONFIG.put("tts-1", new OpenAiModelData(4096));
		MODEL_CONFIG.put("tts-1-1106", new OpenAiModelData(2046));
		MODEL_CONFIG.put("tts-1-hd", new OpenAiModelData(2046));
		MODEL_CONFIG.put("tts-1-hd-1106", new OpenAiModelData(2046));
	}

	/**
	 * Single instance of the data Map, shared by all instances of this model
	 * service class.
	 * 
	 * To add or remove models, act here.
	 */
	private final static Map<String, ModelData> data = new ConcurrentHashMap<>();
	static {
		for (Entry<String, OpenAiModelData> e : MODEL_CONFIG.entrySet()) {

			// This is a work around since the tokenizer library we use might not have
			// latest gpt-3 or -4 models rolled out every 3 months.
			// TODO possibly remove it when we will move to a newer version of the
			// tokenizer.
			String model = e.getKey();
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

			OpenAiModelData cfg = e.getValue();
			cfg.setTokenizer(new OpenAiTokenizer(modelType));

			data.put(model, cfg);
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
	public int getMaxNewTokens(@NonNull String model) {
		// By default, max number of returned tokens matches context size
		return getMaxNewTokens(model, getContextSize(model));
	}

	@Override
	public List<String> listModels() {
		List<Model> l = endpoint.getClient().listModels();
		List<String> result = new ArrayList<>(l.size());
		for (Model m : l)
			result.add(m.getId());
		return result;
	}

	/**
	 * 
	 * @param model
	 * @return The type of calls (function or tool) that the model supports, if any.
	 */
	public SupportedCallType getSupportedCall(@NonNull String model) {
		ModelData data = get(model);
		if ((data == null) || !(data instanceof OpenAiModelData))
			return SupportedCallType.NONE;
		else
			return ((OpenAiModelData) data).getSupportedCallType();
	}

	/**
	 * 
	 * @param model
	 * @return The type of calls (function or tool) that the model supports, if any.
	 */
	public SupportedCallType getSupportedCall(@NonNull String model, SupportedCallType def) {
		ModelData data = get(model);
		if ((data == null) || !(data instanceof OpenAiModelData))
			return def;
		else
			return ((OpenAiModelData) data).getSupportedCallType();
	}
}
