/**
 * 
 */
package io.github.mzattera.predictivepowers.anthropic.services;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.mzattera.predictivepowers.anthropic.client.AnthropicEndpoint;
import io.github.mzattera.predictivepowers.anthropic.client.messages.Message;
import io.github.mzattera.predictivepowers.anthropic.client.messages.Message.Role;
import io.github.mzattera.predictivepowers.anthropic.client.messages.MessagesRequest;
import io.github.mzattera.predictivepowers.anthropic.client.messages.MessagesResponse;
import io.github.mzattera.predictivepowers.huggingface.services.HuggingFaceModelService.HuggingFaceTokenizer;
import io.github.mzattera.predictivepowers.services.AbstractModelService;
import io.github.mzattera.predictivepowers.services.messages.Base64FilePart;
import io.github.mzattera.predictivepowers.services.messages.FilePart;
import io.github.mzattera.predictivepowers.services.messages.MessagePart;
import io.github.mzattera.predictivepowers.services.messages.TextPart;
import io.github.mzattera.util.ImageUtil;
import io.github.mzattera.util.ResourceUtil;
import lombok.Getter;
import lombok.NonNull;

/**
 * Model service ro Antrhop/c models.
 */
public class AnthropicModelService extends AbstractModelService {

	public static class AnthropicTokenizer extends HuggingFaceTokenizer {

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
				else if (p instanceof FilePart) {
					// An image 1092 * 1092 is about 1600 tokens
					BufferedImage img;
					try {
						img = ImageUtil.fromBytes(((FilePart) p).getInputStream());
						int w = img.getWidth();
						int h = img.getHeight();
						result += 1600 * w * h / (1092 * 1092);
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
			for (Message msg : req.getMessages())
				result += count(msg);

			return result + (req.getSystem() == null ? 0 : count(req.getSystem()));
		}
	}

	/**
	 * Maps each Anthrop/c model into its metadata. Can be static as this is
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
				new ModelMetaData("claude-3-opus-20240229", tok, 200_000, 4096));
		MODEL_CONFIG.put("claude-3-sonnet-20240229", //
				new ModelMetaData("claude-3-sonnet-20240229", tok, 200_000, 4096));
		MODEL_CONFIG.put("claude-3-haiku-20240307", //
				new ModelMetaData("claude-3-haiku-20240307", tok, 200_000, 4096));
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

	/**
	 * Throws {@link UnsupportedOperationException} as you cannot train custom
	 * models in Anthrop/c.
	 */
	@Override
	public ModelMetaData put(@NonNull String model, @NonNull ModelMetaData data) {
		throw new UnsupportedOperationException("Custom models not supported in ANTHROP/C");
	}

	@Override
	public AnthropicTokenizer getTokenizer(@NonNull String model) throws IllegalArgumentException {
		return (AnthropicTokenizer) super.getTokenizer(model);
	}

	@Override
	public Tokenizer getTokenizer(@NonNull String model, Tokenizer def) {
		if (!(def instanceof AnthropicTokenizer))
			throw new IllegalArgumentException("Tokenizer must be a subclass of AnthropicTokenizer");
		return (AnthropicTokenizer) super.getTokenizer(model, def);
	}
}