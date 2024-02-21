package io.github.mzattera.predictivepowers.openai.services;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

	public OpenAiTokenizer(String model) {
		this.model = model;
		this.encoding = getEncoding(model);
	}

	// The logic for getEncoding() and getEncodingName() comes from
	// https://github.com/openai/tiktoken/blob/main/tiktoken/model.py
	private static final Map<String, String> MODEL_PREFIX_TO_ENCODING = new HashMap<>();
	static {
		// Initialize MODEL_PREFIX_TO_ENCODING
		MODEL_PREFIX_TO_ENCODING.put("gpt-4-", "cl100k_base");
		MODEL_PREFIX_TO_ENCODING.put("gpt-3.5-turbo-", "cl100k_base");
		MODEL_PREFIX_TO_ENCODING.put("gpt-35-turbo-", "cl100k_base");
	}

	private static final Map<String, String> MODEL_TO_ENCODING = new HashMap<>();
	static {

		// Initialize MODEL_TO_ENCODING

		// chat
		MODEL_TO_ENCODING.put("gpt-4", "cl100k_base");
		MODEL_TO_ENCODING.put("gpt-3.5-turbo", "cl100k_base");
		MODEL_TO_ENCODING.put("gpt-35-turbo", "cl100k_base"); // Azure deployment name

		// base
		MODEL_TO_ENCODING.put("davinci-002", "cl100k_base");
		MODEL_TO_ENCODING.put("babbage-002", "cl100k_base");

		// embeddings
		MODEL_TO_ENCODING.put("text-embedding-3-large", "cl100k_base");
		MODEL_TO_ENCODING.put("text-embedding-3-small", "cl100k_base");
		MODEL_TO_ENCODING.put("text-embedding-ada-002", "cl100k_base");

		// DEPRECATED MODELS
		// text (DEPRECATED)
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

		// code (DEPRECATED)
		MODEL_TO_ENCODING.put("code-davinci-002", "p50k_base");
		MODEL_TO_ENCODING.put("code-davinci-001", "p50k_base");
		MODEL_TO_ENCODING.put("code-cushman-002", "p50k_base");
		MODEL_TO_ENCODING.put("code-cushman-001", "p50k_base");
		MODEL_TO_ENCODING.put("davinci-codex", "p50k_base");
		MODEL_TO_ENCODING.put("cushman-codex", "p50k_base");

		// edit (DEPRECATED)
		MODEL_TO_ENCODING.put("text-davinci-edit-001", "p50k_edit");
		MODEL_TO_ENCODING.put("code-davinci-edit-001", "p50k_edit");

		// old embeddings (DEPRECATED)
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

		// open source
		MODEL_TO_ENCODING.put("gpt2", "gpt2");
	}

	private static Encoding getEncoding(String modelName) {
		EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();

		String encName = getEncodingName(modelName);
		if (encName == null)
			return null;

		switch (encName) {
		case "cl100k_base":
			return registry.getEncoding(EncodingType.CL100K_BASE);
		case "p50k_base":
			return registry.getEncoding(EncodingType.P50K_BASE);
		case "p50k_edit":
			return registry.getEncoding(EncodingType.P50K_EDIT);
		case "r50k_base":
			return registry.getEncoding(EncodingType.R50K_BASE);
		default:
			throw new IllegalArgumentException("No encoding for model: " + modelName);
		}
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
	 * 
	 * @param messages
	 * @return Number of tokens needed to encode given list of messages.
	 */
	public int count(List<OpenAiChatMessage> messages) {

		int sum = 0;

		for (OpenAiChatMessage msg : messages) {

			if ("gpt-3.5-turbo-0301".equals(model))
				++sum;

			String role = msg.getRole().toString();
			if ("function".equals(role))
				sum += 2;
			else
				sum += 3;
			sum += encoding.countTokens(role);

			if (msg.getContent() != null)
				sum += encoding.countTokens(msg.getContent());

			if (msg.getName() != null) {
				if (!"gpt-4-vision-preview".equals(model)) {
					sum += encoding.countTokens(msg.getName());
					if ("gpt-3.5-turbo-0301".equals(model))
						--sum;
					else if ("system".equals(role) || "assistant".equals(role))
						++sum;
				}
			}

			if (msg.getFunctionCall() != null) {
				if ("gpt-3.5-turbo-0301".equals(model))
					sum += 2;
				else
					sum += 3;
				JsonNode functionCall = OpenAiClient.getJsonMapper().valueToTree(msg.getFunctionCall());
				sum += encoding.countTokens(functionCall.path("name").asText());
				if (!functionCall.path("arguments").isMissingNode()) {
					sum += encoding.countTokens(functionCall.path("arguments").asText());
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
			// See https://platform.openai.com/docs/guides/vision
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
					BufferedImage img = ImageUtil.fromBytes(file.getInputStream());
					int w = img.getWidth();
					int h = img.getHeight();
					if ((w <= 512) && (h <= 512)) {
						// As we support only detail=auto, we assume model will use low detail
						sum += 65;
					} else {
						double scaleW = 2048d / w;
						double scaleH = 2048d / h;
						w *= Math.min(scaleW, scaleH);
						h *= Math.min(scaleW, scaleH);
						scaleW = 768d / w;
						scaleH = 768d / h;
						w *= Math.max(scaleW, scaleH);
						h *= Math.max(scaleW, scaleH);
						int wt = w / 512 + ((w % 512) > 0 ? 1 : 0);
						int ht = h / 512 + ((h % 512) > 0 ? 1 : 0);

						sum += 170 * wt * ht + 85;
					}
				} catch (Exception e) {
					sum += 170 + 85; // should not happen, but if we cannot read the image put something in
				}
			}
		} // for each message

		return sum;
	}

	/**
	 * 
	 * @param req
	 * @return Number of tokens used to encode given request.
	 */
	public int count(ChatCompletionsRequest req) {

//		System.out.println(OpenAiClient.getJsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(req));

		if (!model.equals(req.getModel()))
			throw new IllegalArgumentException("Model mismatch");

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
