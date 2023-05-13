package io.github.mzattera.predictivepowers.services;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

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
 * This class provides method to answer questions relying on a context that is
 * provided. The service tries to prevent hallucinations (answers that do not
 * use the information in the context).
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class QuestionAnsweringService {

	public QuestionAnsweringService(OpenAiEndpoint ep) {
		this(ep, ep.getChatService());

		// TODO test best settings.
		completionService.getDefaultReq().setTemperature(0.0);
	}

	public QuestionAnsweringService(OpenAiEndpoint ep, ChatService completionService) {
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
	 * Maximum number of tokens to keep in the question context when answering.
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
	 * Answer a question, using completion service.
	 */
	public QnAPair answer(String question) {
		TextResponse answer = completionService.complete(question);
		return QnAPair.builder().question(question).answer(answer.getText()).build();
	}

	/**
	 * Answer a question, using only information from provided context.
	 * 
	 * @param context The information to be used to answer the question. Notice this
	 *                might be shortened if it is too long to fit prompt size.
	 */
	public QnAPair answer(String question, String context) {

		List<String> l = new ArrayList<>();
		l.add(LlmUtil.split(context, maxContextTokens - TokenUtil.count(question) - 400).get(0)); // 400 is to keep
																									// space for
																									// instructions and
																									// examples

		return answer(question, l);
	}

	/**
	 * Answer a question, using only information from provided context.
	 * 
	 * Notice that context is shortened taking at most maxContextTokens as input,
	 * starting from beginning of provided List.
	 */
	public QnAPair answerWithEmbeddings(String question, List<Pair<EmbeddedText, Double>> context) {

		List<String> l = new ArrayList<>(context.size());
		for (Pair<EmbeddedText, Double> p : context)
			l.add(p.getLeft().getText());

		QnAPair result = answer(question, l);

		// Enrich answer with embeddings, as they have in some case useful properties
		for (int i = 0; i < result.getContext().size(); ++i)
			result.getEmbeddingContext().add(context.get(i).getLeft());

		return result;
	}

	/**
	 * Answer a question, using only information from provided context.
	 * 
	 * Notice that context is shortened taking at most {@link #maxContextTokens} as
	 * input, starting from beginning of provided List.
	 */
	public QnAPair answer(String question, List<String> context) {

		// Guard, should never happen
		if (context.size() == 0)
			return QnAPair.builder().question(question).answer("I don't know.").build();

		ChatMessage qMsg = new ChatMessage("user", "Question: " + question);

		// Provides instructions and examples
		List<ChatMessage> instructions = new ArrayList<>();
		if (completionService.getPersonality() == null)
			instructions.add(new ChatMessage("system", "You are an AI assistant answering questions truthfully."));
		instructions.add(new ChatMessage("user",
				"Answer the below questions truthfully, using only the information in the context. " + //
						"When providing an answer, provide your reasoning as well, step by step. " + //
						"If the answer cannot be found in the context, reply with \"I do not know.\". " + //
						"Strictly return the answer and explanation in JSON format, as shown below."));
		instructions.add(new ChatMessage("user", "Context:"));
		instructions.add(new ChatMessage("user", "Biglydoos are small rodent similar to mice."));
		instructions.add(new ChatMessage("user", "Biglydoos eat cranberries."));
		instructions.add(new ChatMessage("user", "Question: What color are biglydoos?"));
		instructions.add(new ChatMessage("assistant", //
				"{\"answer\": \"I do not know.\", \"explanation\": \"1. This information is not provided in the context.\"}"));
		instructions.add(new ChatMessage("user", "Context:"));
		instructions.add(new ChatMessage("user", "Biglydoos are small rodent similar to mice."));
		instructions.add(new ChatMessage("user", "Biglydoos eat cranberries."));
		instructions.add(new ChatMessage("user", "Question: Do biglydoos eat fruits?"));
		instructions.add(new ChatMessage("assistant", //
				"{\"answer\": \"Yes, biglydoos eat fruits.\", " + //
						"\"explanation\": " + //
						"\"1. The context states: \"Biglydoos eat cranberries.\"\\n" + //
						"2. Cranberries are a kind of fruit.\\n" + //
						"3. Therefore, biglydoos eat fruits.\"}"));
		instructions.add(new ChatMessage("user", "Context:"));
		int tok = TokenUtil.count(qMsg) + TokenUtil.count(instructions);

		int i = 0;
		for (; i < context.size(); ++i) {
			ChatMessage m = new ChatMessage("user", context.get(i));
			tok += TokenUtil.count(m);
			if (tok > maxContextTokens)
				break;
			instructions.add(m);
		}
		instructions.add(qMsg);

		TextResponse answerJson = completionService.complete(instructions);
		QnAPair result = null;
		try {
			result = mapper.readValue(answerJson.getText(), QnAPair.class);
			result.setQuestion(question);
		} catch (JsonProcessingException e) {
			// Sometimes API returns only the answer, not as a JSON
			result = QnAPair.builder().question(question).answer(answerJson.getText()).build();
		}

		for (int j = 0; j < i; ++j) {
			result.getContext().add(context.get(j));
		}

		return result;
	}
}