package io.github.mzattera.predictivepowers;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import io.github.mzattera.predictivepowers.client.openai.chatcompletion.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.client.openai.chatcompletion.ChatMessage;
import lombok.Getter;
import lombok.NonNull;

/**
 * This class provides method to extract questions from a text, in different
 * formats.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class QuestionService {

	// Maps from-to POJO <-> JSON
	private final static ObjectMapper mapper;
	static {
		mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
	}

	@NonNull
	private final OpenAiEndpoint ep;

	/**
	 * This request, with its parameters, is used as default setting for each call.
	 * 
	 * You can change any parameter to change these defaults (e.g. the model used)
	 * and the change will apply to all subsequent calls.
	 */
	@Getter
	@NonNull
	private final ChatCompletionsRequest defaultReq;

	/**
	 * This underlying service is used for executing required prompts.
	 */
	@NonNull
	private final ChatService chat;

	public QuestionService(@NonNull OpenAiEndpoint ep, @NonNull ChatCompletionsRequest defaultReq) {
		this.ep = ep;
		this.defaultReq = defaultReq;

		this.chat = this.ep.getChatService();
		this.chat.setPersonality(null); // should not be necessary
	}

	/**
	 * Extracts question/answer pairs from given text.
	 */
	public List<QnAPair> getQuestions(String text) {
		return getQuestions(text, defaultReq);
	}

	/**
	 * Extracts question/answer pairs from given text.
	 */
	public List<QnAPair> getQuestions(String text, ChatCompletionsRequest req) {

		req = (ChatCompletionsRequest) req.clone();

		// Provides instructions and examples
		List<ChatMessage> instructions = new ArrayList<>();
		instructions.add(new ChatMessage("system",
				"You are a teacher and you are preparing an assessment from some text materials."));
		instructions.add(new ChatMessage("user",
				"Given a context, extract a set of questions and corresponding answers, then format them as a JSON array. Some examples are provided below."));
		instructions.add(new ChatMessage("user", "Context:\n'''\n" //
				+ "Mount Everest  is Earth's highest mountain above sea level, located in the Mahalangur Himal sub-range of the Himalayas. The China–Nepal border runs across its summit point. Its elevation (snow height) of 8,848.86 m (29,031 ft 8+1⁄2 in) was most recently established in 2020 by the Chinese and Nepali authorities.\n" //
				+ "Mount Everest attracts many climbers, including highly experienced mountaineers. There are two main climbing routes, one approaching the summit from the southeast in Nepal (known as the \"standard route\") and the other from the north in Tibet. While not posing substantial technical climbing challenges on the standard route, Everest presents dangers such as altitude sickness, weather, and wind, as well as hazards from avalanches and the Khumbu Icefall. As of 2019, over 300 people have died on Everest, many of whose bodies remain on the mountain.\n" //
				+ "'''"));
		instructions.add(new ChatMessage("assistant", "[\n" //
				+ "   {\n" //
				+ "      \"question\":\"What is the highest mountain on Earth?\",\n" //
				+ "      \"answer\":\"Mount Everest is Earth's highest mountain above sea level, located in the Mahalangur Himal sub-range of the Himalayas.\"\n" //
				+ "   },\n" //
				+ "   {\n" //
				+ "      \"question\":\"What are the two main climbing routes for Mount Everest?\",\n" //
				+ "      \"answer\":\"There are two main climbing routes, one approaching the summit from the southeast in Nepal (known as the \\\"standard route\\\") and the other from the north in Tibet.\"\n" //
				+ "   },\n" //
				+ "   {\n" //
				+ "      \"question\":\"How many people have died on Everest as of 2019?\",\n" //
				+ "      \"answer\":\"As of 2019, over 300 people have died on Everest.\"\n" //
				+ "   }\n" //
				+ "]"));

		return getQuestions(instructions, text, req);
	}

	private List<QnAPair> getQuestions(List<ChatMessage> instructions, String text, ChatCompletionsRequest req) {

		// Split text, based on prompt size
		int tok = 0;
		for (ChatMessage m : instructions) {
			tok += TokenCalculator.count(m);
		}
		int maxSize = req.getMaxTokens() - tok - 100;

		List<QnAPair> result = new ArrayList<>();
		for (String t : LlmUtils.split(text, maxSize)) {
			QnAPair[] questions = getQuestionsShort(instructions, t, req);
			for (int i = 0; i < questions.length; ++i)
				result.add(questions[i]);
		}

		return result;
	}

	private QnAPair[] getQuestionsShort(List<ChatMessage> instructions, String shortText, ChatCompletionsRequest req) {

		List<ChatMessage> prompt = new ArrayList<>(instructions);
		prompt.add(new ChatMessage("user", "Context:\n'''\n" //
				+ shortText //
				+ "'''"));

		// Adjust token limit
		int tok = 0;
		for (ChatMessage m : prompt) {
			tok += TokenCalculator.count(m);
		}
		req.setMaxTokens(req.getMaxTokens() - tok - 100);

		String json = chat.complete(prompt, req).getText();
		QnAPair[] result = new QnAPair[0];
		try {
			result = mapper.readValue(json, QnAPair[].class);
		} catch (JsonProcessingException e) {
		}
		for (QnAPair r : result) {
			r.setContext(shortText);
		}

		return result;
	}
}
