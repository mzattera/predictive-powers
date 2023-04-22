package io.github.mzattera.predictivepowers.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import io.github.mzattera.predictivepowers.LlmUtils;
import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.TokenCalculator;
import io.github.mzattera.predictivepowers.client.openai.chat.ChatMessage;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * This class provides method to answer questions relying on a context that is
 * provided. The service tries to prevent hallucinations (answers that do not
 * use the information in the context).
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@RequiredArgsConstructor
public class AnswerService {

	@NonNull
	private final OpenAiEndpoint ep;

	/**
	 * This underlying service is used for executing required prompts.
	 */
	@NonNull
	@Getter
	private final CompletionService completionService;

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
		return QnAPair.builder().question(question).simpleContext("").answer(answer.getText()).build();
	}

	/**
	 * Answer a question, using only information from provided context.
	 * 
	 * @param context The information to be used to answer the question. Notice this
	 *        is eventually truncated so at most maxContextTokens are considered.
	 */
	public QnAPair answer(String question, String context) {

		// Provides instructions and examples
		List<ChatMessage> instructions = new ArrayList<>();
		instructions.add(new ChatMessage("system",
				"You are an assistant and you must respond to questions truthfully considering only the provided context. If the answer cannot be found in the context, reply with \"I do not know.\""));
		instructions.add(new ChatMessage("user", "Context: Biglydoos are small rodent similar to mice."));
		instructions.add(new ChatMessage("user", "Q: What color are biglydoos?"));
		instructions.add(new ChatMessage("assistant", "A: I do not know."));
		instructions.add(new ChatMessage("user", "Context: " + LlmUtils.split(question, maxContextTokens).get(0)));
		instructions.add(new ChatMessage("user", "Q: " + question));

		TextResponse answer = completionService.complete(instructions);
		return QnAPair.builder().question(question).simpleContext(context).answer(answer.getText()).build();
	}

	/**
	 * Answer a question, using only information from provided context.
	 * 
	 * Notice that context is shortened taking at most maxContextTokens as input,
	 * starting from beginning of provided List.
	 */
	public QnAPair answer(String question, List<Pair<EmbeddedText, Double>> context) {

		// TODO probably you do not need to write "Context" each time...it saves tokens.

		// Provides instructions and examples
		List<ChatMessage> instructions = new ArrayList<>();
		instructions.add(new ChatMessage("system",
				"You are an assistant and you must respond to questions truthfully considering only the provided context. If the answer cannot be found in the context, reply with \"I do not know.\""));
		instructions.add(new ChatMessage("user", "Context: Biglydoos are small rodent similar to mice."));
		instructions.add(new ChatMessage("user", "Context: Biglydoos eat cranberries."));
		instructions.add(new ChatMessage("user", "Q: What color are biglydoos?"));
		instructions.add(new ChatMessage("assistant", "A: I do not know."));
		instructions.add(new ChatMessage("user", "Context: " + context.get(0).getLeft().getText()));
		instructions.add(new ChatMessage("user", "Q: " + question));

		int tok = 0;
		for (ChatMessage m : instructions)
			tok += TokenCalculator.count(m);

		for (Pair<EmbeddedText, Double> p : context) {
			ChatMessage m = new ChatMessage("user", "Context: " + p.getLeft().getText());
			tok += TokenCalculator.count(m);
			if (tok >= maxContextTokens)
				break;
			instructions.add(instructions.size() - 2, m);
		}

		TextResponse answer = completionService.complete(instructions);
		return QnAPair.builder().question(question).kbContext(context).answer(answer.getText()).build();
	}
}