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
				+ "      \"answer\":\"There are two main climbing routes, one approaching the summit from the southeast in Nepal (known as the \"standard route\") and the other from the north in Tibet.\"\n" //
				+ "   },\n" //
				+ "   {\n" //
				+ "      \"question\":\"How many people have died on Everest as of 2019?\",\n" //
				+ "      \"answer\":\"As of 2019, over 300 people have died on Everest.\"\n" //
				+ "   }\n" //
				+ "]"));

		return getQuestions(instructions, text, req);
	}

	/**
	 * Extracts true/false type of questions from given text.
	 */
	public List<QnAPair> getTFQuestions(String text) {
		return getTFQuestions(text, defaultReq);
	}

	/**
	 * Extracts true/false type of questions from given text.
	 */
	public List<QnAPair> getTFQuestions(String text, ChatCompletionsRequest req) {

		// Provides instructions and examples
		List<ChatMessage> instructions = new ArrayList<>();
		instructions.add(new ChatMessage("system",
				"You are a teacher and you are preparing an assessment from some text materials."));
		instructions.add(new ChatMessage("user",
				"Given a context, extract a set of true/false exercise and corresponding answers; make sure some questions require a 'true' answer and  some require a 'false' answer, then format them as a JSON array. Some examples are provided below."));
		instructions.add(new ChatMessage("user", "Context:\n'''\n" //
				+ "Mount Everest  is Earth's highest mountain above sea level, located in the Mahalangur Himal sub-range of the Himalayas. The China–Nepal border runs across its summit point. Its elevation (snow height) of 8,848.86 m (29,031 ft 8+1⁄2 in) was most recently established in 2020 by the Chinese and Nepali authorities.\n" //
				+ "Mount Everest attracts many climbers, including highly experienced mountaineers. There are two main climbing routes, one approaching the summit from the southeast in Nepal (known as the \"standard route\") and the other from the north in Tibet. While not posing substantial technical climbing challenges on the standard route, Everest presents dangers such as altitude sickness, weather, and wind, as well as hazards from avalanches and the Khumbu Icefall. As of 2019, over 300 people have died on Everest, many of whose bodies remain on the mountain.\n" //
				+ "'''"));
		instructions.add(new ChatMessage("assistant", "[\n" //
				+ "   {\n" //
				+ "      \"question\":\"Mount Everest is the highest mountain on Earth.\",\n" //
				+ "      \"answer\":\"true\"\n" //
				+ "   },\n" //
				+ "   {\n" //
				+ "      \"question\":\"The so-called \"standard route\" is one of the main climbing routes.\",\n" //
				+ "      \"answer\":\"true\"\n" //
				+ "   },\n" //
				+ "   {\n" //
				+ "      \"question\":\"As of 2019, less than 300 people have died on Everest.\",\n" //
				+ "      \"answer\":\"false\"\n" //
				+ "   }\n" //
				+ "]"));

		return getQuestions(instructions, text, req);
	}

	/**
	 * Extracts "fill the blank" type of questions from given text.
	 */
	public List<QnAPair> getFillQuestions(String text) {
		return getFillQuestions(text, defaultReq);
	}

	/**
	 * Extracts "fill the blank" type of questions from given text.
	 */
	public List<QnAPair> getFillQuestions(String text, ChatCompletionsRequest req) {

		// Provides instructions and examples
		List<ChatMessage> instructions = new ArrayList<>();
		instructions.add(new ChatMessage("system",
				"You are a teacher and you are preparing an assessment from some text materials."));
		instructions.add(new ChatMessage("user",
				"Given a context, extract a set of 'fill the blank' exercises and corresponding fill-in word, then format them as a JSON array. Some examples are provided below."));
		instructions.add(new ChatMessage("user", "Context:\n'''\n" //
				+ "Mount Everest  is Earth's highest mountain above sea level, located in the Mahalangur Himal sub-range of the Himalayas. The China–Nepal border runs across its summit point. Its elevation (snow height) of 8,848.86 m (29,031 ft 8+1⁄2 in) was most recently established in 2020 by the Chinese and Nepali authorities.\n" //
				+ "Mount Everest attracts many climbers, including highly experienced mountaineers. There are two main climbing routes, one approaching the summit from the southeast in Nepal (known as the \"standard route\") and the other from the north in Tibet. While not posing substantial technical climbing challenges on the standard route, Everest presents dangers such as altitude sickness, weather, and wind, as well as hazards from avalanches and the Khumbu Icefall. As of 2019, over 300 people have died on Everest, many of whose bodies remain on the mountain.\n" //
				+ "'''"));
		instructions.add(new ChatMessage("assistant", "[\n" //
				+ "   {\n" //
				+ "      \"question\":\"Mount ______ is Earth's highest mountain above sea level.\",\n" //
				+ "      \"answer\":\"Everest\"\n" //
				+ "   },\n" //
				+ "   {\n" //
				+ "      \"question\":\"Mount Everest is located in the Mahalangur Himal sub-range of the ______.\",\n" //
				+ "      \"answer\":\"Himalayas\"\n" //
				+ "   },\n" //
				+ "   {\n" //
				+ "      \"question\":\"As of 2019, over ______ people have died on Everest.\",\n" //
				+ "      \"answer\":\"300\"\n" //
				+ "   }\n" //
				+ "]"));

		return getQuestions(instructions, text, req);
	}

	/**
	 * Extracts multiple-choice questions from given text.
	 */
	public List<QnAPair> getMCQuestions(String text) {
		return getMCQuestions(text, defaultReq);
	}

	/**
	 * Extracts multiple-choice questions from given text.
	 */
	public List<QnAPair> getMCQuestions(String text, ChatCompletionsRequest req) {

		// Provides instructions and examples
		List<ChatMessage> instructions = new ArrayList<>();
		instructions.add(new ChatMessage("system",
				"You are a teacher and you are preparing an assessment from some text materials."));
		instructions.add(new ChatMessage("user",
				"Given a context, extract a set of multiple-choice questions, corresponding answers, and a list of options for each question, then format them as a JSON array. Make sure the options for one question are all different. Some examples are provided below."));
		instructions.add(new ChatMessage("user", "Context:\n'''\n" //
				+ "Mount Everest  is Earth's highest mountain above sea level, located in the Mahalangur Himal sub-range of the Himalayas. The China–Nepal border runs across its summit point. Its elevation (snow height) of 8,848.86 m (29,031 ft 8+1⁄2 in) was most recently established in 2020 by the Chinese and Nepali authorities.\n" //
				+ "Mount Everest attracts many climbers, including highly experienced mountaineers. There are two main climbing routes, one approaching the summit from the southeast in Nepal (known as the \"standard route\") and the other from the north in Tibet. While not posing substantial technical climbing challenges on the standard route, Everest presents dangers such as altitude sickness, weather, and wind, as well as hazards from avalanches and the Khumbu Icefall. As of 2019, over 300 people have died on Everest, many of whose bodies remain on the mountain.\n" //
				+ "'''"));
		instructions.add(new ChatMessage("assistant", "[\n" //
				+ "   {\n" //
				+ "      \"question\":\"What is the highest mountain on Earth?\",\n" //
				+ "      \"answer\":\"Mount Everest\",\n" //
				+ "      \"options\":[\n" //
				+ "         \"Mount Everest\",\n" //
				+ "         \"K2\",\n" //
				+ "         \"Mount Kilimanjaro\",\n" //
				+ "         \"Mont Blanc\",\n" //
				+ "         \"Denali (Mount McKinley)\"\n" //
				+ "      ]\n" //
				+ "   },\n" //
				+ "   {\n" //
				+ "      \"question\":\"How many people have died on Everest as of 2019?\",\n" //
				+ "      \"answer\":\"Over 300\",\n" //
				+ "      \"options\":[\n" //
				+ "         \"Nobody died on Everest\",\n" //
				+ "         \"Less than 10\",\n" //
				+ "         \"Around 100\",\n" //
				+ "         \"Over 300\",\n" //
				+ "         \"The number is unknown\"\n" //
				+ "      ]\n" //
				+ "   },\n" //
				+ "   {\n" //
				+ "      \"question\":\"In which country is Mount Everest located?\",\n" //
				+ "      \"answer\":\"On the China–Nepal border.\",\n" //
				+ "      \"options\":[\n" //
				+ "         \"In China\",\n" //
				+ "         \"In India\",\n" //
				+ "         \"On the China–Nepal border.\",\n" //
				+ "         \"In Pakistan\",\n" //
				+ "         \"On the China–Russia border.\"\n" //
				+ "      ]\n" //
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
		int maxSize = req.getMaxTokens()/2 - tok;
		System.out.println("  -> MAX " + maxSize);
	
		List<QnAPair> result = new ArrayList<>();
		for (String t : LlmUtils.split(text, maxSize)) {
			System.out.println("  -> " + TokenCalculator.count(t));
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
	
		String json = chat.complete(prompt, req).getText();
		QnAPair[] result = new QnAPair[0];
		try {
			result = mapper.readValue(json, QnAPair[].class);
		} catch (JsonProcessingException e) {
			System.err.println(json);
		}
		for (QnAPair r : result) {
			r.setContext(shortText);
		}
	
		return result;
	}
}
