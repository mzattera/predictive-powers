/*
 * Copyright 2024 Massimiliano "Maxi" Zattera
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

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;

import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.chat.Function;
import io.github.mzattera.predictivepowers.openai.client.chat.FunctionCall;
import io.github.mzattera.predictivepowers.openai.client.chat.OpenAiTool;
import io.github.mzattera.predictivepowers.openai.client.chat.OpenAiToolCall;
import io.github.mzattera.predictivepowers.services.ModelService.Tokenizer;
import io.github.mzattera.predictivepowers.services.messages.FilePart;
import io.github.mzattera.predictivepowers.services.messages.FilePart.ContentType;
import io.github.mzattera.predictivepowers.services.messages.MessagePart;
import io.github.mzattera.util.ImageUtil;
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
		MODEL_PREFIX_TO_ENCODING.put("gpt-4-", "cl100k_base");
		MODEL_PREFIX_TO_ENCODING.put("gpt-4o-", "o200k_base");
		MODEL_PREFIX_TO_ENCODING.put("chatgpt-4o-", "o200k_base");
		MODEL_PREFIX_TO_ENCODING.put("gpt-3.5-turbo-", "cl100k_base");
		MODEL_PREFIX_TO_ENCODING.put("gpt-35-turbo-", "cl100k_base");
		MODEL_PREFIX_TO_ENCODING.put("o1-", "o200k_base");
		MODEL_PREFIX_TO_ENCODING.put("o3-", "o200k_base");
	}

	private static final Map<String, String> MODEL_TO_ENCODING = new HashMap<>();
	static {
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

		MODEL_TO_ENCODING.put("o1", "o200k_base");
		MODEL_TO_ENCODING.put("o3", "o200k_base");

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
	public int count(List<OpenAiChatMessage> messages) {

		int sum = 0;

		// For some models, only first message for a role counts
		boolean firstMessage = true;

		for (OpenAiChatMessage msg : messages) {

			if ("gpt-3.5-turbo-0301".equals(model))
				++sum;

			String role = msg.getRole().toString();
			if ("function".equals(role))
				sum += 2;
			else
				sum += 3;
			sum += encoding.countTokens(role);

			// TODO urgent, what when we mix text an images? We should analyze parts
			// separately, probably
			try {
				sum += encoding.countTokens(msg.getContent());
			} catch (IllegalArgumentException e) {
				// Message is not a simple text message
			}

			if (msg.getName() != null) { // Name provided //////////////////////////////////

				sum += encoding.countTokens(msg.getName())+1;
				if (model.startsWith("o1-mini")) {
					if (firstMessage)
						sum-=6;
					sum += 11;
				} else if (model.startsWith("o1-preview")) {
					if (firstMessage)
						sum-=7;
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
						sum+=2;
				} else if (model.startsWith("o4-mini")) {
					if (firstMessage)
						--sum;
					if ("assistant".equals(role))
						sum+=2;
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

			if (msg.getFunctionCall() != null) {
				if ("gpt-3.5-turbo".equals(model))
					sum += 5;
				else if ("gpt-3.5-turbo-0301".equals(model))
					sum += 2;
				else
					sum += 3;
				JsonNode functionCall = OpenAiClient.getJsonMapper().valueToTree(msg.getFunctionCall());
				sum += encoding.countTokens(functionCall.path("name").asText());
				if (!functionCall.path("arguments").isMissingNode()) {
					sum += encoding.countTokens(functionCall.path("arguments").asText());
					if ("gpt-3.5-turbo".equals(model))
						sum -= (msg.getFunctionCall().getArguments().size() * 4);
				}
			}

			if (msg.getToolCalls() != null) {
				if (msg.getToolCalls().size() > 1)
					sum += 21;
				else
					sum += 3;

				// This is true if all calls in the message have no parameters
				boolean allCallsWithNoParams = true;

				for (OpenAiToolCall toolCall : msg.getToolCalls()) {
					sum += 2;
					// Call ID is NOT counted against total tokens

					String type = toolCall.getType().toString();
					sum += encoding.countTokens(type);

					if ("function".equals(type)) {

						FunctionCall functionCall = toolCall.getFunction();
						sum += encoding.countTokens(functionCall.getName());

						if (functionCall.getArguments().size() == 0)
							++sum;
						else
							allCallsWithNoParams = false;

						for (Entry<String, Object> e : functionCall.getArguments().entrySet()) {
							sum += 2;
							String fName = e.getKey();
							sum += encoding.countTokens(fName);
							sum += encoding.countTokens(e.getValue().toString());
						}

					} else
						throw new IllegalArgumentException("Unsupported tool type: " + type);
				} // for each tool call

				if (allCallsWithNoParams) {
					if (msg.getToolCalls().size() > 1)
						++sum;
					else
						--sum;
				}
			} // if we have tool calls

			// If we use image model, calculate image tokens.
			// See https://platform.openai.com/docs/guides/images
			boolean firstImage = true;
			for (MessagePart part : msg.getContentParts()) {
				if (!(part instanceof FilePart))
					continue;
				FilePart file = (FilePart) part;
				if (file.getContentType() != ContentType.IMAGE)
					continue;
				if (firstImage) {
					sum += 8;
					firstImage = false;
				}
				try {
					if (!file.isLocalFile()) {
						// For performance reasons, we do not inspect the image so we just estimate
						// tokens
						sum += 170 * 2 + 85; // should not happen, but if we cannot read the image put something in
					} else {
						// Inspect image for exact token calculation
						BufferedImage img = ImageUtil.fromBytes(file.getInputStream());
						int w = img.getWidth();
						int h = img.getHeight();
						if ((w > 2048) || (h > 2048)) {
							double scale = 2048d / Math.max(w, h);
							w *= scale;
							h *= scale;
						}
						if ((w > 768) || (h > 768)) {
							double scale = 768d / Math.min(w, h);
							w *= scale;
							h *= scale;
						}
						int wt = w / 512 + ((w % 512) > 0 ? 1 : 0);
						int ht = h / 512 + ((h % 512) > 0 ? 1 : 0);

						sum += 170 * wt * ht + 85;
					}
				} catch (Exception e) {
					sum += 170 * 2 + 85; // should not happen, but if we cannot read the image put something in
				}
			}

			firstMessage = false;
		} // for each message

		if (model.startsWith("o1-mini") && (sum>0))
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
	public int count(ChatCompletionsRequest req) {
		int sum = count(req.getMessages());
		sum += countFunctions(req.getFunctions());
		sum += countTools(req.getTools());
		sum += 3;
		return sum;
	}

	/**
	 * 
	 * @param functions
	 * @return Number of tokens needed to encode given list of Functions (=function
	 *         descriptions).
	 */
	private int countFunctions(List<Function> functions) {

		if (functions == null)
			return 0;

		JsonNode functionsArray = OpenAiClient.getJsonMapper().valueToTree(functions);
		int sum = "gpt-3.5-turbo".equals(model) ? 8 : 4;

		for (JsonNode function : functionsArray) {
			sum += encoding.countTokens(function.path("name").asText());

			JsonNode description = function.path("description");
			if (!description.isMissingNode()) {
				++sum;
				sum += encoding.countTokens(function.path("description").asText());
			}

			JsonNode parameters = function.path("parameters");
			if (!function.path("parameters").isMissingNode()) {
				sum += 3;
				JsonNode properties = parameters.path("properties");

				if (!properties.isMissingNode()) {
					Iterator<String> propertiesKeys = properties.fieldNames();

					while (propertiesKeys.hasNext()) { // For each property, which is a function parameter
						boolean hasDescription = false;
						boolean isEnumOrInt = false;

						String propertiesKey = propertiesKeys.next();
						sum += encoding.countTokens(propertiesKey);
						JsonNode v = properties.path(propertiesKey);

						Iterator<String> fields = v.fieldNames();
						while (fields.hasNext()) {
							String field = fields.next();
							if ("type".equals(field)) {
								sum += 2;
								String type = v.path("type").asText();
								sum += encoding.countTokens(type);
								if ("integer".equals(type))
									isEnumOrInt = true;
							} else if ("description".equals(field)) {
								++sum;
								sum += encoding.countTokens(v.path("description").asText());
								hasDescription = true;
							} else if ("enum".equals(field)) {
								sum -= 3;
								Iterator<JsonNode> enumValues = v.path("enum").elements();
								while (enumValues.hasNext()) {
									JsonNode enumValue = enumValues.next();
									sum += 3;
									sum += encoding.countTokens(enumValue.asText());
								}
								isEnumOrInt = true;
							}
						} // for each field of the property

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
	private int countTools(List<OpenAiTool> tools) {

		if (tools == null)
			return 0;

		JsonNode toolsArray = OpenAiClient.getJsonMapper().valueToTree(tools);
		int sum = 0;

		for (JsonNode tool : toolsArray) {

			if (!"function".equals(tool.path("type").asText()))
				throw new IllegalArgumentException("Unsoported tool type: " + tool.path("type").asText());

			JsonNode function = tool.path("function");
			sum += encoding.countTokens(function.path("name").asText());

			JsonNode description = function.path("description");
			if (!description.isMissingNode()) {
				++sum;
				sum += encoding.countTokens(function.path("description").asText());
			}

			JsonNode parameters = function.path("parameters");
			if (!function.path("parameters").isMissingNode()) {
				JsonNode properties = parameters.path("properties");

				if (!properties.isMissingNode()) {

					if (properties.size() > 0)
						sum += 3;

					Iterator<String> propertiesKeys = properties.fieldNames();
					while (propertiesKeys.hasNext()) {
						boolean hasDescription = false;
						boolean isDouble = false;

						String propertiesKey = propertiesKeys.next();
						sum += encoding.countTokens(propertiesKey);
						JsonNode v = properties.path(propertiesKey);

						Iterator<String> fields = v.fieldNames();
						while (fields.hasNext()) {
							String field = fields.next();
							if ("type".equals(field)) {
								sum += 2;
								String type = v.path("type").asText();
								sum += encoding.countTokens(type);
								if ("number".equals(type))
									isDouble = true;

							} else if ("description".equals(field)) {
								sum += 2;
								sum += encoding.countTokens(v.path("description").asText());
								hasDescription = true;
							} else if ("enum".equals(field)) {
								sum -= 3;
								Iterator<JsonNode> enumValues = v.path("enum").elements();
								while (enumValues.hasNext()) {
									JsonNode enumValue = enumValues.next();
									sum += 3;
									sum += encoding.countTokens(enumValue.asText());
								}
							}
						} // for each field

						if (hasDescription && isDouble)
							sum -= 1;
					} // for each property
				} // if there are properties
			} // if there are parameters

			sum += 11;
		} // for each tool

		sum += 16;
		return sum;
	}
}
