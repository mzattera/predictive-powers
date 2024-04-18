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
import io.github.mzattera.predictivepowers.services.AbstractTool;
import io.github.mzattera.predictivepowers.services.Capability;
import io.github.mzattera.predictivepowers.services.Toolset;
import io.github.mzattera.predictivepowers.services.messages.FilePart;
import io.github.mzattera.predictivepowers.services.messages.MessagePart;
import io.github.mzattera.predictivepowers.services.messages.TextPart;
import io.github.mzattera.predictivepowers.services.messages.ToolCall;
import io.github.mzattera.predictivepowers.services.messages.ToolCallResult;
import io.github.mzattera.util.ImageUtil;
import io.github.mzattera.util.ResourceUtil;
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
						result += count(AnthropicClient.getJsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(p));
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
					result += count(AnthropicClient.getJsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(req.getTools()));
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

	//////////////////////////////////////////////////////////////////////////////////////////////

	public static class GetCurrentWeatherTool extends AbstractTool {

		// This is a schema describing the function parameters
		private static class GetCurrentWeatherParameters {
		}

		public GetCurrentWeatherTool(@NonNull String name) {
			super(name, // Function name
					"A", // Function description
					GetCurrentWeatherParameters.class);
		}

		@Override
		public ToolCallResult invoke(@NonNull ToolCall call) throws Exception {
			return new ToolCallResult(call, "30 Celsius");
		}
	}

	// Capability providing the functions to the agent
	private final static Capability DEFAULT_CAPABILITY = new Toolset();
	static {
		DEFAULT_CAPABILITY.putTool("A", () -> (new GetCurrentWeatherTool("A")));
		DEFAULT_CAPABILITY.putTool("B", () -> (new GetCurrentWeatherTool("B")));
		DEFAULT_CAPABILITY.putTool("C", () -> (new GetCurrentWeatherTool("C")));
	}

	public static void main(String[] args) throws Exception {

		try (AnthropicEndpoint ep = new AnthropicEndpoint(); AnthropicChatService bot = ep.getChatService();) {

			AnthropicTokenizer tok = ep.getModelService().getTokenizer(bot.getModel());

//			bot.complete("Hi");
//			System.out.println(">>> " + tok.count(bot.getDefaultReq()));

			bot.addCapability(DEFAULT_CAPABILITY);
			bot.complete("Hi");
			System.out.println(">>> " + tok.count(bot.getDefaultReq()));
		}
	}
}