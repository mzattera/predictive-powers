/*
 * Copyright 2024-2025 Massimiliano "Maxi" Zattera
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

package io.github.mzattera.predictivepowers.openai;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.openai.core.JsonValue;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionCreateParams.Function;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;
import com.openai.models.chat.completions.ChatCompletionTool;

import io.github.mzattera.predictivepowers.services.ModelService.Tokenizer;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

/**
 * Tokenizer for OpenAI models.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@ToString
public class OpenAiTokenizer implements Tokenizer {

	@Getter
	@NonNull
	private final String model;

	@Getter
	@NonNull
	private final Encoding encoding;

	private OpenAiTokenizer(@NonNull String model) {
		this(model, getEncoding(model));
	}

	OpenAiTokenizer(@NonNull String model, Encoding encoding) {
		this.model = model;
		this.encoding = encoding;
	}

	public static OpenAiTokenizer getTokenizer(@NonNull String model) {
		try {
			return new OpenAiTokenizer(model);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static Encoding getEncoding(String modelName) {

		// Tries "regular" way of getting encoding for model
		EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
		Optional<Encoding> enc = registry.getEncodingForModel(modelName);
		if (enc.isPresent())
			return enc.get();

		// If it fails, tries tiktoken mapping
		String encName = getEncodingName(modelName);
		if (encName == null)
			throw new IllegalArgumentException("No encoding found for model: " + modelName);

		switch (encName) {
		case "cl100k_base":
			return registry.getEncoding(EncodingType.CL100K_BASE);
		case "p50k_base":
			return registry.getEncoding(EncodingType.P50K_BASE);
		case "p50k_edit":
			return registry.getEncoding(EncodingType.P50K_EDIT);
		case "r50k_base":
			return registry.getEncoding(EncodingType.R50K_BASE);
		case "o200k_base":
			return registry.getEncoding(EncodingType.O200K_BASE);
		default:
			throw new IllegalArgumentException("No encoding found for model: " + modelName);
		}
	}

	// The fall back logic for getEncoding() and getEncodingName() comes from
	// https://github.com/openai/tiktoken/blob/main/tiktoken/model.py
	private static final Map<String, String> MODEL_PREFIX_TO_ENCODING = new HashMap<>();
	static {
		MODEL_PREFIX_TO_ENCODING.put("gpt-5-", "o200k_base");
		MODEL_PREFIX_TO_ENCODING.put("gpt-5.1-", "o200k_base");
		MODEL_PREFIX_TO_ENCODING.put("gpt-5.2-", "o200k_base");

		MODEL_PREFIX_TO_ENCODING.put("gpt-4-", "cl100k_base");
		MODEL_PREFIX_TO_ENCODING.put("gpt-4.1-", "o200k_base");
		MODEL_PREFIX_TO_ENCODING.put("gpt-4.5-", "o200k_base");
		MODEL_PREFIX_TO_ENCODING.put("gpt-4o-", "o200k_base");
		MODEL_PREFIX_TO_ENCODING.put("chatgpt-4o-", "o200k_base");

		MODEL_PREFIX_TO_ENCODING.put("gpt-3.5-turbo-", "cl100k_base");
		MODEL_PREFIX_TO_ENCODING.put("gpt-35-turbo-", "cl100k_base");

		MODEL_PREFIX_TO_ENCODING.put("o1-", "o200k_base");
		MODEL_PREFIX_TO_ENCODING.put("o3-", "o200k_base");
		MODEL_PREFIX_TO_ENCODING.put("o4-mini-", "o200k_base");
	}

	private static final Map<String, String> MODEL_TO_ENCODING = new HashMap<>();
	static {
		MODEL_TO_ENCODING.put("gpt-5", "o200k_base");
		MODEL_TO_ENCODING.put("gpt-5.1", "o200k_base");
		MODEL_TO_ENCODING.put("gpt-5.2", "o200k_base");
		
		MODEL_TO_ENCODING.put("gpt-4", "cl100k_base");
		MODEL_TO_ENCODING.put("gpt-4o", "o200k_base");
		MODEL_TO_ENCODING.put("gpt-3.5-turbo", "cl100k_base");
		MODEL_TO_ENCODING.put("gpt-3.5", "cl100k_base");
		MODEL_TO_ENCODING.put("gpt-35-turbo", "cl100k_base");

		MODEL_TO_ENCODING.put("davinci-002", "cl100k_base");
		MODEL_TO_ENCODING.put("babbage-002", "cl100k_base");

		MODEL_TO_ENCODING.put("text-embedding-3-large", "cl100k_base");
		MODEL_TO_ENCODING.put("text-embedding-3-small", "cl100k_base");
		MODEL_TO_ENCODING.put("text-embedding-ada-002", "cl100k_base");

		MODEL_TO_ENCODING.put("o1", "o200k_base");
		MODEL_TO_ENCODING.put("o3", "o200k_base");
		MODEL_TO_ENCODING.put("o4-mini", "o200k_base");

		MODEL_TO_ENCODING.put("gpt-5", "o200k_base");
		MODEL_TO_ENCODING.put("gpt-4.1", "o200k_base");

		MODEL_TO_ENCODING.put("text-davinci-003", "p50k_base");
		MODEL_TO_ENCODING.put("text-davinci-002", "p50k_base");
		MODEL_TO_ENCODING.put("text-davinci-001", "r50k_base");
		MODEL_TO_ENCODING.put("text-curie-001", "r50k_base");
		MODEL_TO_ENCODING.put("text-babbage-001", "r50k_base");
		MODEL_TO_ENCODING.put("text-ada-001", "r50k_base");
		MODEL_TO_ENCODING.put("davinci", "r50k_base");
		MODEL_TO_ENCODING.put("curie", "r50k_base");
		MODEL_TO_ENCODING.put("babbage", "r50k_base");
		MODEL_TO_ENCODING.put("ada", "r50k_base");

		MODEL_TO_ENCODING.put("code-davinci-002", "p50k_base");
		MODEL_TO_ENCODING.put("code-davinci-001", "p50k_base");
		MODEL_TO_ENCODING.put("code-cushman-002", "p50k_base");
		MODEL_TO_ENCODING.put("code-cushman-001", "p50k_base");
		MODEL_TO_ENCODING.put("davinci-codex", "p50k_base");
		MODEL_TO_ENCODING.put("cushman-codex", "p50k_base");

		MODEL_TO_ENCODING.put("text-davinci-edit-001", "p50k_edit");
		MODEL_TO_ENCODING.put("code-davinci-edit-001", "p50k_edit");

		MODEL_TO_ENCODING.put("text-similarity-davinci-001", "r50k_base");
		MODEL_TO_ENCODING.put("text-similarity-curie-001", "r50k_base");
		MODEL_TO_ENCODING.put("text-similarity-babbage-001", "r50k_base");
		MODEL_TO_ENCODING.put("text-similarity-ada-001", "r50k_base");
		MODEL_TO_ENCODING.put("text-search-davinci-doc-001", "r50k_base");
		MODEL_TO_ENCODING.put("text-search-curie-doc-001", "r50k_base");
		MODEL_TO_ENCODING.put("text-search-babbage-doc-001", "r50k_base");
		MODEL_TO_ENCODING.put("text-search-ada-doc-001", "r50k_base");
		MODEL_TO_ENCODING.put("code-search-babbage-code-001", "r50k_base");
		MODEL_TO_ENCODING.put("code-search-ada-code-001", "r50k_base");

		// Added
		MODEL_TO_ENCODING.put("o4-mini", "o200k_base");
	}

	private static String getEncodingName(String modelName) {
		String encodingName = MODEL_TO_ENCODING.get(modelName);
		if (encodingName != null)
			return encodingName;

		for (Map.Entry<String, String> entry : MODEL_PREFIX_TO_ENCODING.entrySet()) {
			if (modelName.startsWith(entry.getKey())) {
				return entry.getValue();
			}
		}

		return null;
	}

	@Override
	public int count(@NonNull String text) {
		return encoding.countTokens(text);
	}

	/**
	 * Perform exact token calculation.
	 * 
	 * Notice that, for performance reason, this is approximate if any message in
	 * the list contains a URL to an image.
	 * 
	 * @param messages
	 * @return Number of tokens needed to encode given list of messages.
	 */
	@SuppressWarnings("deprecation")
	public int count(List<ChatCompletionMessageParam> messages) {

		int sum = 0;
		
//		if ("cl100k_base".equals(encoding.getName()))
//			sum+=4;

		// For some models, only first message for a role counts
		boolean firstMessage = true;

		for (ChatCompletionMessageParam msg : messages) {
				
			if ("gpt-3.5-turbo-0301".equals(model))
				++sum;

			String role = null;
			String content = null;
			String name = null;
			if (msg.isAssistant()) {
				role = "assistant";
				content = msg.asAssistant().content().filter(c -> c.isText()).isPresent()
						? msg.asAssistant().content().get().asText()
						: null;

				// Message is a function call
				if (msg.asAssistant().functionCall().isPresent()) {
					if ("gpt-3.5-turbo".equals(model))
						sum += 5;
//					else if ("gpt-3.5-turbo-0301".equals(model))
//						sum += 2;
					else
						sum += 3;
					sum += encoding.countTokens(msg.asAssistant().functionCall().get().name());
					sum += encoding.countTokens(msg.asAssistant().functionCall().get().arguments());
					// Too much hassle fro an old model
//						if ("gpt-3.5-turbo".equals(model))
//							sum -= (msg.getFunctionCall().getArguments().size() * 4);
				}

				// Message is a ftool call
				if (msg.asAssistant().toolCalls().isPresent()) {
					List<ChatCompletionMessageToolCall> calls = msg.asAssistant().toolCalls().get();
					if (calls.size() > 1)
						sum += 21;
					else
						sum += 3;

					// This is true if all calls in the message have no parameters
					boolean allCallsWithNoParams = true;

					for (ChatCompletionMessageToolCall toolCall : calls) {
						sum += 2;
						// Call ID is NOT counted against total tokens

						String type = (String) toolCall._type().asString().get();
						sum += encoding.countTokens(type);

						if ("function".equals(type)) {

							com.openai.models.chat.completions.ChatCompletionMessageToolCall.Function functionCall = toolCall
									.function();
							sum += encoding.countTokens(functionCall.name());
							sum += encoding.countTokens(functionCall.arguments());

							// TODO URGENT See how it was done before
							if (functionCall.arguments().length() == 0)
								++sum;
							else
								allCallsWithNoParams = false;

//							for (Entry<String, Object> e : functionCall.getArguments().entrySet()) {
//								sum += 2;
//								String fName = e.getKey();
//								sum += encoding.countTokens(fName);
//								sum += encoding.countTokens(e.getValue().toString());
//							}

						} else
							throw new IllegalArgumentException("Unsupported tool type: " + type);
					} // for each tool call

					if (allCallsWithNoParams) {
						if (calls.size() > 1)
							++sum;
						else
							--sum;
					}
				} // if we have tool calls

			} else if (msg.isDeveloper()) {
				role = "developer";
				content = msg.asDeveloper().content().isText() ? msg.asDeveloper().content().asText() : null;
				name = msg.asDeveloper().name().orElse(null);
			} else if (msg.isFunction()) {
				role = "function";
				content = msg.asFunction().content().orElse(null);
				name = msg.asFunction().name();
			} else if (msg.isSystem()) {
				role = "system";
				content = msg.asSystem().content().isText() ? msg.asSystem().content().asText() : null;
				name = msg.asSystem().name().orElse(null);
			} else if (msg.isTool()) {
				role = "tool";
				content = msg.asTool().content().isText() ? msg.asTool().content().asText() : null;
			} else if (msg.isUser()) {
				role = "user";
				content = msg.asUser().content().isText() ? msg.asUser().content().asText() : null;
				name = msg.asUser().name().orElse(null);
			} else
				throw new IllegalArgumentException("Unrecognized message role: " + role);

			if ("function".equals(role))
				sum += 2;
			else
				sum += 3;
			sum += encoding.countTokens(role);

			// TODO urgent, what when we mix text an images? We should analyze parts
			// separately, probably
			if (content != null)
				sum += encoding.countTokens(content);

			if (name != null) { // Name provided //////////////////////////////////

				sum += encoding.countTokens(name) + 3;
				if (model.startsWith("o1-mini")) {
					if (firstMessage)
						sum -= 6;
					sum += 11;
				} else if (model.startsWith("o1-preview")) {
					if (firstMessage)
						sum -= 7;
					sum += 11;
				} else if (model.startsWith("o1")) {
					if (firstMessage)
						--sum;
				} else if (model.startsWith("o3-mini")) {
					if (firstMessage)
						--sum;
				} else if (model.startsWith("o3")) {
					if (firstMessage)
						--sum;
					if ("assistant".equals(role))
						sum += 2;
				} else if (model.startsWith("o4-mini")) {
					if (firstMessage)
						--sum;
					if ("assistant".equals(role))
						sum += 2;
				}
			} else { // No name provided ///////////////////////////////////
				if (model.startsWith("o1-mini")) {
					if (firstMessage)
						sum += 2;
					else
						sum += 8;
				} else if (model.startsWith("o1-preview")) {
					if (firstMessage)
						sum += 1;
					else
						sum += 8;
				} else if (model.startsWith("o1")) {
					if (firstMessage)
						--sum;
				} else if (model.startsWith("o3-mini")) {
					if (firstMessage)
						--sum;
				} else if (model.startsWith("o3")) {
					if (firstMessage)
						--sum;
					if ("assistant".equals(role))
						sum += 2;
				} else if (model.startsWith("o4-mini")) {
					if (firstMessage)
						--sum;
					if ("assistant".equals(role))
						sum += 2;
				}
			} // here we were handling name in message

			// TODO Add token calculation for audio

			// If we use image model, calculate image tokens.
			// See https://platform.openai.com/docs/guides/images
//			boolean firstImage = true;
//			for (MessagePart part : msg.getContentParts()) {
//				if (!(part instanceof FilePart))
//					continue;
//				FilePart file = (FilePart) part;
//				if (file.getContentType() != ContentType.IMAGE)
//					continue;
//				if (firstImage) {
//					sum += 8;
//					firstImage = false;
//				}
//				try {
//					if (!file.isLocalFile()) {
//						// For performance reasons, we do not inspect the image so we just estimate
//						// tokens
//						sum += 170 * 2 + 85; // should not happen, but if we cannot read the image put something in
//					} else {
//						// Inspect image for exact token calculation
//						BufferedImage img = ImageUtil.fromBytes(file.getInputStream());
//						int w = img.getWidth();
//						int h = img.getHeight();
//						if ((w > 2048) || (h > 2048)) {
//							double scale = 2048d / Math.max(w, h);
//							w *= scale;
//							h *= scale;
//						}
//						if ((w > 768) || (h > 768)) {
//							double scale = 768d / Math.min(w, h);
//							w *= scale;
//							h *= scale;
//						}
//						int wt = w / 512 + ((w % 512) > 0 ? 1 : 0);
//						int ht = h / 512 + ((h % 512) > 0 ? 1 : 0);
//
//						sum += 170 * wt * ht + 85;
//					}
//				} catch (Exception e) {
//					sum += 170 * 2 + 85; // should not happen, but if we cannot read the image put something in
//				}
//			}
//
//			firstMessage = false;
		} // for each message

		if (model.startsWith("o1-mini") && (sum > 0))
			--sum;
		return sum;

	}

	/**
	 * Perform exact token calculation.
	 * 
	 * Notice that, for performance reason, this is approximate if any message in
	 * the request contains a URL to an image.
	 * 
	 * @param req
	 * @return Number of tokens used to encode given request.
	 */
	@SuppressWarnings("deprecation")
	public int count(ChatCompletionCreateParams req) {
		int sum = count(req.messages());
		if (req.functions().isPresent())
			sum += countFunctions(req.functions().get());
		if (req.tools().isPresent())
			sum += countTools(req.tools().get());
		sum += 3;
		return sum;
	}

	/**
	 * 
	 * @param functions
	 * @return Number of tokens needed to encode given list of Functions (=function
	 *         descriptions).
	 */
	@SuppressWarnings("deprecation")
	private int countFunctions(List<Function> functions) {

		if (functions == null)
			return 0;

		int sum = "gpt-3.5-turbo".equals(model) ? 8 : 4;

		for (Function function : functions) {
			sum += encoding.countTokens(function.name());

			if (function.description().isPresent()) {
				++sum;
				sum += encoding.countTokens(function.description().get());
			}

			if (function.parameters().isPresent()) {
				sum += 3;
				Map<String, JsonValue> properties = function.parameters().get()._additionalProperties();

				if (properties != null) {
					Iterator<String> propertiesKeys = properties.keySet().iterator();

					while (propertiesKeys.hasNext()) { // For each property, which is a function parameter
						boolean hasDescription = false;
						boolean isEnumOrInt = false;

						String propertiesKey = propertiesKeys.next();
						sum += encoding.countTokens(propertiesKey);
//						JsonValue v = properties.get(propertiesKey);

						// TODO URGENT Fix one day, unless OpenAI SDK provides these by default
//						Iterator<String> fields = v.fieldNames();
//						while (fields.hasNext()) {
//							String field = fields.next();
//							if ("type".equals(field)) {
//								sum += 2;
//								String type = v.path("type").asText();
//								sum += encoding.countTokens(type);
//								if ("integer".equals(type))
//									isEnumOrInt = true;
//							} else if ("description".equals(field)) {
//								++sum;
//								sum += encoding.countTokens(v.path("description").asText());
//								hasDescription = true;
//							} else if ("enum".equals(field)) {
//								sum -= 3;
//								Iterator<JsonNode> enumValues = v.path("enum").elements();
//								while (enumValues.hasNext()) {
//									JsonNode enumValue = enumValues.next();
//									sum += 3;
//									sum += encoding.countTokens(enumValue.asText());
//								}
//								isEnumOrInt = true;
//							}
//						} // for each field of the property

						if (hasDescription && isEnumOrInt)
							++sum;
					} // For each property
				}
			} // If function has parameters

			sum += 6;
		} // for each function

		sum += 12;
		return sum;
	}

	/**
	 * 
	 * @param tools
	 * @return Number of tokens needed to encode given list of Tools (=tools
	 *         descriptions).
	 */
	private int countTools(List<ChatCompletionTool> tools) {

		// TODO URGENT
		return 0;
//		if (tools == null)
//			return 0;
//
//		JsonNode toolsArray = OpenAiClient.getJsonMapper().valueToTree(tools);
//		int sum = 0;
//
//		for (JsonNode tool : toolsArray) {
//
//			if (!"function".equals(tool.path("type").asText()))
//				throw new IllegalArgumentException("Unsoported tool type: " + tool.path("type").asText());
//
//			JsonNode function = tool.path("function");
//			sum += encoding.countTokens(function.path("name").asText());
//
//			JsonNode description = function.path("description");
//			if (!description.isMissingNode()) {
//				++sum;
//				sum += encoding.countTokens(function.path("description").asText());
//			}
//
//			JsonNode parameters = function.path("parameters");
//			if (!function.path("parameters").isMissingNode()) {
//				JsonNode properties = parameters.path("properties");
//
//				if (!properties.isMissingNode()) {
//
//					if (properties.size() > 0)
//						sum += 3;
//
//					Iterator<String> propertiesKeys = properties.fieldNames();
//					while (propertiesKeys.hasNext()) {
//						boolean hasDescription = false;
//						boolean isDouble = false;
//
//						String propertiesKey = propertiesKeys.next();
//						sum += encoding.countTokens(propertiesKey);
//						JsonNode v = properties.path(propertiesKey);
//
//						Iterator<String> fields = v.fieldNames();
//						while (fields.hasNext()) {
//							String field = fields.next();
//							if ("type".equals(field)) {
//								sum += 2;
//								String type = v.path("type").asText();
//								sum += encoding.countTokens(type);
//								if ("number".equals(type))
//									isDouble = true;
//
//							} else if ("description".equals(field)) {
//								sum += 2;
//								sum += encoding.countTokens(v.path("description").asText());
//								hasDescription = true;
//							} else if ("enum".equals(field)) {
//								sum -= 3;
//								Iterator<JsonNode> enumValues = v.path("enum").elements();
//								while (enumValues.hasNext()) {
//									JsonNode enumValue = enumValues.next();
//									sum += 3;
//									sum += encoding.countTokens(enumValue.asText());
//								}
//							}
//						} // for each field
//
//						if (hasDescription && isDouble)
//							sum -= 1;
//					} // for each property
//				} // if there are properties
//			} // if there are parameters
//
//			sum += 11;
//		} // for each tool
//
//		sum += 16;
//		return sum;
	}
}
