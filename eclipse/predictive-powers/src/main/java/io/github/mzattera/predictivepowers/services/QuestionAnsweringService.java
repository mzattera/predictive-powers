package io.github.mzattera.predictivepowers.services;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

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

	// TODO
	/*
	 * Provide a ground truth for the API. If you provide the API with a body of
	 * text to answer questions about (like a Wikipedia entry) it will be less
	 * likely to confabulate a response.
	 * 
	 * Use a low probability and show the API how to say "I don't know". If the API
	 * understands that in cases where it's less certain about a response that
	 * saying "I don't know" or some variation is appropriate, it will be less
	 * inclined to make up answers.
	 */

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
	private int maxContextTokens = 3 * 1024;

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
	 *                is eventually truncated so at most maxContextTokens are
	 *                considered.
	 */
	public QnAPair answer(String question, String context) {

		List<String> l = new ArrayList<>();
		l.add(LlmUtil.split(context, maxContextTokens).get(0));

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

		return answer(question, l);
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

		ChatMessage qMsg = new ChatMessage("user", "Q: " + question);

		// Provides instructions and examples
		// TODO probably you do not need to write "Context" each time...it saves tokens.
		List<ChatMessage> instructions = new ArrayList<>();
		instructions.add(new ChatMessage("system",
				"You are an assistant and you must respond to questions truthfully considering only the provided context. If the answer cannot be found in the context, reply with \"I do not know.\""));
		instructions.add(new ChatMessage("user", "Context: Biglydoos are small rodent similar to mice."));
		instructions.add(new ChatMessage("user", "Context: Biglydoos eat cranberries."));
		instructions.add(new ChatMessage("user", "Q: What color are biglydoos?"));
		instructions.add(new ChatMessage("assistant", "A: I do not know."));
		instructions.add(new ChatMessage("user", "Context: " + context.get(0)));

		int tok = TokenUtil.count(qMsg) + TokenUtil.count(instructions);

		// TODO it adds one too many
		int i = 1;
		for (; i < context.size(); ++i) {
			ChatMessage m = new ChatMessage("user", "Context: " + context.get(i));
			tok += TokenUtil.count(m);
			if (tok > maxContextTokens)
				break;
			instructions.add(m);
		}
		instructions.add(qMsg);

		TextResponse answer = completionService.complete(instructions);
		String txt = answer.getText().startsWith("A: ") ? answer.getText().substring(3) : answer.getText();
		QnAPair result = QnAPair.builder().question(question).answer(txt).build();
		for (int j = 0; j < i; ++j) {
			result.getContext().add(context.get(j));
		}

		return result;
	}
}