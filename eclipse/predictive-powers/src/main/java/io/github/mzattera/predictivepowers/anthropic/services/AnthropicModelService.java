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

/**
 * 
 */
package io.github.mzattera.predictivepowers.anthropic.services;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.github.mzattera.predictivepowers.anthropic.client.AnthropicClient;
import io.github.mzattera.predictivepowers.anthropic.client.AnthropicEndpoint;
import io.github.mzattera.predictivepowers.anthropic.client.messages.Message;
import io.github.mzattera.predictivepowers.anthropic.client.messages.MessagesRequest;
import io.github.mzattera.predictivepowers.huggingface.services.HuggingFaceModelService.HuggingFaceTokenizer;
import io.github.mzattera.predictivepowers.services.AbstractModelService;
import io.github.mzattera.predictivepowers.services.messages.FilePart;
import io.github.mzattera.predictivepowers.services.messages.MessagePart;
import io.github.mzattera.predictivepowers.services.messages.TextPart;
import io.github.mzattera.predictivepowers.services.messages.ToolCall;
import io.github.mzattera.predictivepowers.services.messages.ToolCallResult;
import io.github.mzattera.predictivepowers.util.ImageUtil;
import io.github.mzattera.predictivepowers.util.ResourceUtil;
import lombok.Getter;
import lombok.NonNull;

/**
 * Model service for Anthropic models.
 */
public class AnthropicModelService extends AbstractModelService {

	public static class AnthropicTokenizer extends HuggingFaceTokenizer {

		// TODO URGENT test it even if results won't be perfect

		public AnthropicTokenizer() throws IOException {
			// TODO must use a newer version as soon as it is made available
			super(ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
					.newInstance(ResourceUtil.getResourceFile("claude-v1-tokenization.json").toPath()));
		}

		public int count(Message message) {
			int result = 0;
			for (MessagePart p : message.getContent()) {
				if (p instanceof TextPart)
					result += count(p.getContent());
				if ((p instanceof ToolCall) || (p instanceof ToolCallResult)) {
					try {
						result += count(
								AnthropicClient.getJsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(p));
					} catch (JsonProcessingException e) {
					}
				} else if (p instanceof FilePart) {
					// From http://docs.anthropic.com/claude/docs/vision
					BufferedImage img;
					try {
						// No need to check whether this is local since images are all base64
						img = ImageUtil.fromBytes(((FilePart) p).getInputStream());
						int w = img.getWidth();
						int h = img.getHeight();
						result += (w * h) / 750;
					} catch (IOException e) {
						// In case we cannot access the file, we approximate (it is approximated anyway)
						result += 1600;
					}
				}
			}

			return result;
		}

		public int count(MessagesRequest req) {
			int result = 0;

			// Instructions
			if (req.getSystem() != null)
				result += count(req.getSystem());

			// Messages
			for (Message msg : req.getMessages())
				result += count(msg);

			// Tools
			if (req.getTools() != null) {
				if (req.getModel().startsWith("claude-3-opus"))
					result += 395;
				else if (req.getModel().startsWith("claude-3-sonnet"))
					result += 159;
				else if (req.getModel().startsWith("claude-3-haiku"))
					result += 264;
//			for (AnthropicTool tool : req.getTools()) {
//					result += count(tool.getId());
//					result += count(tool.getDescription());
//					try {
//						result += count(JsonSchema.getSchema(tool.getParameters()));
//					} catch (JsonProcessingException e) {
//					}
//				}

				try {
					result += count(AnthropicClient.getJsonMapper().writerWithDefaultPrettyPrinter()
							.writeValueAsString(req.getTools()));
				} catch (JsonProcessingException e) {
				}
			}

			return result;
		}
	}

	/**
	 * Maps each Anthropic model into its metadata. Can be static as this is
	 * immutable.
	 */
	final static Map<String, ModelMetaData> MODEL_CONFIG = new HashMap<>();
	static {
		Tokenizer tok = null;
		try {
			tok = new AnthropicTokenizer();
		} catch (IOException e) {
		}

		MODEL_CONFIG.put("claude-3-opus-20240229", //
				new ModelMetaData("claude-3-opus-20240229", tok, 200_000, 4096, true));
		MODEL_CONFIG.put("claude-3-sonnet-20240229", //
				new ModelMetaData("claude-3-sonnet-20240229", tok, 200_000, 4096, true));
		MODEL_CONFIG.put("claude-3-haiku-20240307", //
				new ModelMetaData("claude-3-haiku-20240307", tok, 200_000, 4096, true));
	}

	@Getter
	private final AnthropicEndpoint endpoint;

	/**
	 * @param data
	 */
	public AnthropicModelService(AnthropicEndpoint endpoint) {
		super(MODEL_CONFIG);
		this.endpoint = endpoint;
	}

	@Override
	public List<String> listModels() {
		return new ArrayList<>(MODEL_CONFIG.keySet());
	}

	/**
	 * @return Null, as the service is not bound to any specific model.
	 */
	@Override
	public String getModel() {
		return null;
	}

	/**
	 * Throws {@link UnsupportedOperationException} as the service is not bound to
	 * any specific model.
	 */
	@Override
	public void setModel(@NonNull String model) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ModelMetaData put(@NonNull String model, @NonNull ModelMetaData data) {
		if ((data.getTokenizer() != null) && !(data.getTokenizer() instanceof AnthropicTokenizer))
			throw new IllegalArgumentException("Tokenizer must be a subclass of AnthropicTokenizer");
		return super.put(model, data);
	}

	@Override
	public AnthropicTokenizer getTokenizer(@NonNull String model) throws IllegalArgumentException {
		Tokenizer result = super.getTokenizer(model);
		if (result == null)
			return null;
		return (AnthropicTokenizer) result;
	}

	@Override
	public AnthropicTokenizer getTokenizer(@NonNull String model, Tokenizer def) {
		if ((def != null) && !(def instanceof AnthropicTokenizer))
			throw new IllegalArgumentException("Tokenizer must be a subclass of AnthropicTokenizer");
		return (AnthropicTokenizer) super.getTokenizer(model, def);
	}
}