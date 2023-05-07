package io.github.mzattera.predictivepowers.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.util.ModelUtil;
import io.github.mzattera.predictivepowers.openai.util.TokenUtil;
import io.github.mzattera.util.LlmUtil;
import lombok.Getter;
import lombok.NonNull;

/**
 * This class provides method to extract questions from a text, in different
 * formats.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class QuestionExtractionService {

	public QuestionExtractionService(OpenAiEndpoint ep) {
		this(ep, ep.getChatService());

		// TODO test best settings.
		completionService.getDefaultReq().setTemperature(0.2);
	}

	public QuestionExtractionService(OpenAiEndpoint ep, ChatService completionService) {
		this.ep = ep;
		this.completionService = completionService;
		maxContextTokens = Math.max(ModelUtil.getContextSize(this.completionService.getDefaultReq().getModel()), 2046)
				* 3 / 4;
	}

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
	 * This underlying service is used for executing required prompts.
	 */
	@NonNull
	@Getter
	private final ChatService completionService;

	/**
	 * Maximum number of tokens to keep in the context when extracting questions.
	 * 
	 * As there is no Java code available for an exact calculation, this is
	 * approximated.
	 */
	@Getter
	private int maxContextTokens;

	public void setMaxContextTokens(int n) {
		if (n < 1)
			throw new IllegalArgumentException("Must keep at least 1 token.");
		maxContextTokens = n;
	}

	/**
	 * Extracts question/answer pairs from given text.
	 */
	public List<QnAPair> getQuestions(String text) {

		// Provides instructions and examples
		List<ChatMessage> instructions = new ArrayList<>();
		instructions.add(new ChatMessage("system",
				"You are a teacher and you are preparing an assessment from some text materials."));
		instructions.add(new ChatMessage("user",
				"Given a context, extract a set of questions and corresponding answers, then format them as a JSON array. Some examples are provided below."));
		instructions.add(new ChatMessage("user", "Context:\n'''\n" //
				+ "Mount Everest  is Earth's highest mountain above sea level, located in the Mahalangur Himal sub-range of the Himalayas. The China–Nepal border runs across its summit point. Its elevation (snow height) of 8,848.86 m (29,031 ft 8+1⁄2 in) was most recently established in 2020 by the Chinese and Nepali authorities.\n" //
				+ "Mount Everest attracts many climbers, including highly experienced mountaineers. There are two main climbing routes, one approaching the summit from the southeast in Nepal (known as the 'standard route') and the other from the north in Tibet. While not posing substantial technical climbing challenges on the standard route, Everest presents dangers such as altitude sickness, weather, and wind, as well as hazards from avalanches and the Khumbu Icefall. As of 2019, over 300 people have died on Everest, many of whose bodies remain on the mountain.\n" //
				+ "'''"));
		instructions.add(new ChatMessage("assistant", "[\n" //
				+ "   {\n" //
				+ "      \"question\":\"What is the highest mountain on Earth?\",\n" //
				+ "      \"answer\":\"Mount Everest is Earth's highest mountain above sea level, located in the Mahalangur Himal sub-range of the Himalayas.\"\n" //
				+ "   },\n" //
				+ "   {\n" //
				+ "      \"question\":\"What are the two main climbing routes for Mount Everest?\",\n" //
				+ "      \"answer\":\"There are two main climbing routes, one approaching the summit from the southeast in Nepal (known as the 'standard route') and the other from the north in Tibet.\"\n" //
				+ "   },\n" //
				+ "   {\n" //
				+ "      \"question\":\"How many people have died on Everest as of 2019?\",\n" //
				+ "      \"answer\":\"As of 2019, over 300 people have died on Everest.\"\n" //
				+ "   }\n" //
				+ "]"));

		return getQuestions(instructions, text);
	}

	/**
	 * Extracts true/false type of questions from given text.
	 */
	public List<QnAPair> getTFQuestions(String text) {

		// Provides instructions and examples
		List<ChatMessage> instructions = new ArrayList<>();
		instructions.add(new ChatMessage("system",
				"You are a teacher and you are preparing an assessment from some text materials."));
		instructions.add(new ChatMessage("user",
				"Given a context, extract a set of true/false exercise and corresponding answers; make sure some questions require a 'true' answer and  some require a 'false' answer, then format them as a JSON array. Some examples are provided below."));
		instructions.add(new ChatMessage("user", "Context:\n'''\n" //
				+ "Mount Everest  is Earth's highest mountain above sea level, located in the Mahalangur Himal sub-range of the Himalayas. The China–Nepal border runs across its summit point. Its elevation (snow height) of 8,848.86 m (29,031 ft 8+1⁄2 in) was most recently established in 2020 by the Chinese and Nepali authorities.\n" //
				+ "Mount Everest attracts many climbers, including highly experienced mountaineers. There are two main climbing routes, one approaching the summit from the southeast in Nepal (known as the 'standard route') and the other from the north in Tibet. While not posing substantial technical climbing challenges on the standard route, Everest presents dangers such as altitude sickness, weather, and wind, as well as hazards from avalanches and the Khumbu Icefall. As of 2019, over 300 people have died on Everest, many of whose bodies remain on the mountain.\n" //
				+ "'''"));
		instructions.add(new ChatMessage("assistant", "[\n" //
				+ "   {\n" //
				+ "      \"question\":\"Mount Everest is the highest mountain on Earth.\",\n" //
				+ "      \"answer\":\"true\"\n" //
				+ "   },\n" //
				+ "   {\n" //
				+ "      \"question\":\"The so-called 'standard route' is one of the main climbing routes.\",\n" //
				+ "      \"answer\":\"true\"\n" //
				+ "   },\n" //
				+ "   {\n" //
				+ "      \"question\":\"As of 2019, less than 300 people have died on Everest.\",\n" //
				+ "      \"answer\":\"false\"\n" //
				+ "   }\n" //
				+ "]"));

		List<QnAPair> result = getQuestions(instructions, text);
		Iterator<QnAPair> it = result.iterator();
		while (it.hasNext()) {
			QnAPair q = it.next();
			q.setAnswer(q.getAnswer().trim().toLowerCase());
			if (!q.getAnswer().equals("true") && !q.getAnswer().equals("false")) // bad result
				it.remove();
		}

		return result;
	}

	/**
	 * Extracts "fill the blank" type of questions from given text.
	 */
	public List<QnAPair> getFillQuestions(String text) {
		completionService.getDefaultReq().setTemperature(0.7);

		// Provides instructions and examples
		List<ChatMessage> instructions = new ArrayList<>();
		instructions.add(new ChatMessage("system",
				"You are a teacher and you are preparing an assessment from some text materials."));
		instructions.add(new ChatMessage("user",
				"Create 'fill the blank' exercises with corresponding fill words from the given context, and format them as a JSON array. Make sure to generate questions where a missing word is replaced with a blank, denoted as '______', and provide the missing word as the answer."));
		instructions.add(new ChatMessage("user", "Context:\r\n" + "'''\r\n"
				+ "Mount Everest  is Earth's highest mountain above sea level, located in the Mahalangur Himal sub-range of the Himalayas. The China\u2013Nepal border runs across its summit point. Its elevation (snow height) of 8,848.86 m (29,031 ft 8+1\u20442 in) was most recently established in 2020 by the Chinese and Nepali authorities.\r\n"
				+ "'''"));
		instructions.add(new ChatMessage("assistant", "[\r\n"
				+ "   {\r\n"
				+ "      \"question\":\"Which is Earth's highest mountain above sea level?\",\r\n"
				+ "      \"answer\":\"Mount Everest\"\r\n"
				+ "   }\r\n"
				+ "]"));
		instructions.add(new ChatMessage("user", "This is wrong, this is not a  'fill the blank' exercises. Try again."));
		instructions.add(new ChatMessage("assistant", "[\r\n"
				+ "   {\r\n"
				+ "      \"question\":\"Mount ______ is Earth's highest mountain above sea level.\",\r\n"
				+ "      \"answer\":\"Everest\"\r\n"
				+ "   }\r\n"
				+ "]"));
		instructions.add(new ChatMessage("user", "This is correct."));

		List<QnAPair> result = getQuestions(instructions, text);
		Iterator<QnAPair> it = result.iterator();
		while (it.hasNext()) {
			QnAPair q = it.next();
			q.setAnswer(q.getAnswer().trim());

			if (!q.getQuestion().contains("___")) {
				it.remove();
				continue;
			}

			// how many words in the answer? Not more than 2 per question (e.g. "Alan
			// Turing").
			if (q.getAnswer().split("\\s").length > 2) {
				System.out.println(q.toString());
				it.remove();
				continue;
			}
		}

		return result;
	}

	/**
	 * Extracts multiple-choice questions from given text.
	 */
	public List<QnAPair> getMCQuestions(String text) {

		// Provides instructions and examples
		List<ChatMessage> instructions = new ArrayList<>();
		instructions.add(new ChatMessage("system",
				"You are a teacher and you are preparing an assessment from some text materials."));
		instructions.add(new ChatMessage("user",
				"Given a context, extract a set of multiple-choice questions, corresponding answers, and a list of options for each question, then format them as a JSON array. Make sure the options for one question are all different. Some examples are provided below."));
		instructions.add(new ChatMessage("user", "Context:\n'''\n" //
				+ "Mount Everest  is Earth's highest mountain above sea level, located in the Mahalangur Himal sub-range of the Himalayas. The China–Nepal border runs across its summit point. Its elevation (snow height) of 8,848.86 m (29,031 ft 8+1⁄2 in) was most recently established in 2020 by the Chinese and Nepali authorities.\n" //
				+ "Mount Everest attracts many climbers, including highly experienced mountaineers. There are two main climbing routes, one approaching the summit from the southeast in Nepal (known as the 'standard route') and the other from the north in Tibet. While not posing substantial technical climbing challenges on the standard route, Everest presents dangers such as altitude sickness, weather, and wind, as well as hazards from avalanches and the Khumbu Icefall. As of 2019, over 300 people have died on Everest, many of whose bodies remain on the mountain.\n" //
				+ "'''"));
		instructions.add(new ChatMessage("assistant", "[\n" //
				+ "   {\n" //
				+ "      \"question\":\"What is the highest mountain on Earth?\",\n" //
				+ "      \"options\":[\n" //
				+ "         \"1. Mount Everest\",\n" //
				+ "         \"2. K2\",\n" //
				+ "         \"3. Mount Kilimanjaro\",\n" //
				+ "         \"4. Mont Blanc\",\n" //
				+ "         \"5. Denali (Mount McKinley)\"\n" //
				+ "      ]," //
				+ "      \"answer\":\"1\"\n" //
				+ "   },\n" //
				+ "   {\n" //
				+ "      \"question\":\"How many people have died on Everest as of 2019?\",\n" //
				+ "      \"options\":[\n" //
				+ "         \"1. Nobody died on Everest\",\n" //
				+ "         \"2. Less than 10\",\n" //
				+ "         \"3. Around 100\",\n" //
				+ "         \"4. Over 300\",\n" //
				+ "         \"5. The number is unknown\"\n" //
				+ "      ],\n" //
				+ "      \"answer\":\"4\"\n" //
				+ "   },\n" //
				+ "   {\n" //
				+ "      \"question\":\"In which country is Mount Everest located?\",\n" //
				+ "      \"options\":[\n" //
				+ "         \"1. In China\",\n" //
				+ "         \"2. In India\",\n" //
				+ "         \"3. On the China–Nepal border.\",\n" //
				+ "         \"4. In Pakistan\",\n" //
				+ "         \"5. On the China–Russia border.\"\n" //
				+ "      ],\n" //
				+ "      \"answer\":\"3\"\n" //
				+ "   }\n" //
				+ "]"));

		List<QnAPair> result = getQuestions(instructions, text);
		Iterator<QnAPair> it = result.iterator();
		while (it.hasNext()) {
			QnAPair q = it.next();
			q.setAnswer(q.getAnswer().trim());

			int c = -1;
			try {
				c = Integer.parseInt(q.getAnswer());
			} catch (Exception e) {
				it.remove();
				continue;
			}

			if ((c <= 0) || (c > q.getOptions().size())) {
				it.remove();
				continue;
			}
		}

		return result;
	}

	/**
	 * Extract questions, following given instructions.
	 * 
	 * It also splits the input text in smaller chunks, if needed.
	 * 
	 * @param instructions
	 * @param text
	 * @return
	 */
	private List<QnAPair> getQuestions(List<ChatMessage> instructions, String text) {

		// Split text, based on prompt size
		int tok = TokenUtil.count(instructions);

		List<QnAPair> result = new ArrayList<>();
		for (String t : LlmUtil.split(text, maxContextTokens - tok - 10)) { // adding a message is additional tokens
			result.addAll(getQuestionsShort(instructions, t));
		}

		return result;
	}

	/**
	 * Extract questions, following given instructions.
	 * 
	 * @param instructions
	 * @param text
	 * @return
	 */
	private List<QnAPair> getQuestionsShort(List<ChatMessage> instructions, String shortText) {

		List<ChatMessage> prompt = new ArrayList<>(instructions);
		prompt.add(new ChatMessage("user", "Context:\n'''\n" //
				+ shortText //
				+ "\n'''"));

		String json = completionService.complete(prompt).getText();
		QnAPair[] result = null;
		try {
			result = mapper.readValue(json, QnAPair[].class);
		} catch (JsonProcessingException e) {
			// TODO do something here?
		}
		for (QnAPair r : result) {
			r.getContext().add(shortText);
		}

		return Arrays.asList(result);
	}
}
