/**
 * 
 */
package io.github.mzattera.autobot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.math3.util.Pair;

import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;

import io.github.mzattera.autobot.Document.QnA;

/**
 * This class wraps GPT-3 providing capabilities on top of it.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public final class GPT {

	/** Server timeout in seconds. */
	private static final int TIMEOUT = 30;

	// TODO REMOVE PRIVATE KEY
	private static final OpenAiService SERVICE = new OpenAiService(
			"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", TIMEOUT);

	/**
	 * The token count of your prompt plus returned text cannot exceed the model's
	 * context length. Most models have a context length of 2048 tokens (except for
	 * the newest models, which support 4096).
	 */
	private final static int MAX_CONTEXT_LENGTH = 4000;

	/**
	 * Maximum length of generated text.
	 */
	private final static int MAX_COMPLETION_TOKENS = 256;

	/**
	 * Maximum length of prompt.
	 */
	private final static int MAX_PROMPT_WORDS = (int)((MAX_CONTEXT_LENGTH - MAX_COMPLETION_TOKENS) * 0.5);

	private GPT() {
	}

	private static final String PROMPT3 = "Following, you can find some information. Information starts here:\n\n";
	private static final String PROMPT4 = "\n\nInformation ends here. Based on this information, ";

	/**
	 * 
	 * @param doc The Document we use to extract QnA pairs.
	 * @return A list QnA pairs fro the document.
	 */
	public static List<Document.QnA> createQnAs(Document doc) {

		System.out.println("\n===[ QnA Pairs ]==================================");

		List<QnA> qnas = new ArrayList<>();
		for (Entry<String, List<String>> e : createQuestions(doc.text).entrySet()) {
			String snippet = e.getKey();
			for (Pair<String, String> qnaPair : createQnAForShortText(snippet, e.getValue())) {
//				System.out.println(qnaPair.getKey());
//				System.out.println(qnaPair.getValue());
				System.out.print("\"" + qnaPair.getKey() + "\", ");
				System.out.println("\"" + qnaPair.getValue() + "\"");
				System.out.println("--------------------------------------------------");
				qnas.add(new QnA(snippet, qnaPair.getKey(), qnaPair.getValue()));
			}
		}

		return qnas;
	}

	/**
	 * Get answers for given questions from an input text.
	 * 
	 * @param snippet   Input text used to answer questions.
	 * @param questions List of questions to ask about the given text.
	 * @return List of QnA pairs.
	 */
	private static List<Pair<String, String>> createQnAForShortText(String snippet, List<String> questions) {

		List<Pair<String, String>> result = new ArrayList<>();
		for (String question : questions) {
			String answer = createAnswer(snippet, question);
			if (answer.length() > 0) {
				Pair<String, String> qna = new Pair<>(question, answer);
				result.add(qna);
			}
		}

		return result;
	}

	private static final String PROMPT1 = "Following, you can find some information. Information starts here:\n\n";

	private static final String PROMPT2 = "\n\nInformation ends here. Create exactly twenty questions about the provided information:\n\n"
			+ "Question 1:";

	/**
	 * 
	 * @param input
	 * @return A map from a text snippet into a list of associated questions. The
	 *         input text might be split in several snippets if it is too long.
	 */
	private static Map<String, List<String>> createQuestions(String input) {

		Map<String, List<String>> result = new HashMap<>();
		for (String snippet : split(input, PROMPT1, PROMPT2)) {
			List<String> questions = createQuestionsForShortText(snippet);
			result.put(snippet, questions);
		}

		return result;
	}

	/**
	 * 
	 * @return A list of questions created out of the given input text.
	 */
	private static List<String> createQuestionsForShortText(String snippet) {

		// TODO .stop("?")
		CompletionRequest completionRequest = CompletionRequest.builder().model("text-davinci-002")
				.prompt(PROMPT1 + snippet + PROMPT2).maxTokens(MAX_COMPLETION_TOKENS).temperature(0.3)
				.frequencyPenalty(-0.0).build();
//		System.out.print("\n\n$$$$ " + completionRequest.getPrompt() + "\n");
		CompletionChoice completion = SERVICE.createCompletion(completionRequest).getChoices().get(0);

		completionRequest.
		// TODO check finish reason

		List<String> result = new ArrayList<>();
		Pattern p = Pattern.compile("Question [\\d]+:([^\\n]*)");
		Matcher m = p.matcher("Question 1: " + completion.getText()); // the first question pattern is in the prompt
		while (m.find()) {
			String q = m.group(1).trim();
			if (q.length() > 0) {
				// System.out.println("@@@ QUESTION: [" + q + "]");
				result.add(q);
			}
		}

		return result;
	}

	/**
	 * Get answers for given question from an input text.
	 * 
	 * @param snippet  Input text used to answer the question.
	 * @param question The question to ask about the given text.
	 * @return Answer for the question, as found in given text.
	 */
	private static String createAnswer(String snippet, String question) {

		// TODO .stop("?")
		CompletionRequest completionRequest = CompletionRequest.builder().model("text-davinci-002")
				.prompt(PROMPT3 + snippet + PROMPT4 + question).maxTokens(MAX_COMPLETION_TOKENS).temperature(0.3)
				.frequencyPenalty(-0.0).build();
		CompletionChoice completion = SERVICE.createCompletion(completionRequest).getChoices().get(0);

		// TODO check finish reason

//		System.out.println("@@@ QNA:\tQUESTION [" + question + "]\n\tANSWER[" + completion.getText().trim() + "]");

		return completion.getText().trim();
	}

	private static final String PROMPT5 = "Following, you can find some information. Information starts here:\n\n";
	private static final String PROMPT6 = "\n\nInformation ends here.\n\n" + "This is a question about the above: ";
	private static final String PROMPT7 = "\n\n"
			+ "Based on the provided information, create five variations of the question.\n\n" + "Variation 1:";

	/**
	 * 
	 * @param questions A map from text snippet into questions about that snippet.
	 * @return A map linking each intent name with corresponding list of utterances.
	 */
//	public static Map<String, List<String>> createIntents(Map<String, List<String>> questions) {
//		Map<String, List<String>> result = new HashMap<>();
//
//		int pos = 1;
//		for (String snippet : questions.keySet()) {
//
//			// Questions for the snippet
//			List<String> q = questions.get(snippet);
//			for (String question : q) {
//				String intentName = question.trim().replaceAll("[^A-Za-z0-9\\-_]+", "_");
//				if (intentName.length() > 90)
//					intentName = intentName.substring(0, 90);
//				intentName = intentName + (pos++);
//				if (result.containsKey(intentName))
//					System.out.println("Duplicated intent: " + intentName);
//				result.put(intentName, createUtterances(snippet, question));
//			} // for each question
//		} // for each snippet
//
//		// Remove duplicate utterances
//		Set<String> allUtterances = new HashSet<>();
//		for (List<String> utterances : result.values()) {
//			for (int i = 0; i < utterances.size();) {
//				String u = utterances.get(i);
//				if (allUtterances.contains(u)) {
//					utterances.remove(i);
//				} else {
//					allUtterances.add(u);
//					++i;
//				}
//			}
//		}
//
//		return result;
//	}

	/**
	 * 
	 * @return A list of utterances (question variations) corresponding to given QnA
	 *         pair. Notice the list will contain given question too.
	 */
	public static List<String> createUtterances(Document.QnA qna) {
		List<String> result = new ArrayList<>();

		System.out.println("\n===[ Variations ]==================================");
		System.out.println("Question: " + qna.question);

		CompletionRequest completionRequest = CompletionRequest.builder().model("text-davinci-002")
				.prompt(PROMPT5 + qna.snippet + PROMPT6 + qna.question + PROMPT7).maxTokens(MAX_COMPLETION_TOKENS)
				.temperature(0.7).frequencyPenalty(-0.0).build();
		CompletionChoice completion = SERVICE.createCompletion(completionRequest).getChoices().get(0);

		// TODO check finish reason

		Pattern p = Pattern.compile("Variation [\\d]+:([^\\n]*)");
		Matcher m = p.matcher("Variation 1: " + completion.getText()); // the first variation pattern is in the prompt
		while (m.find()) {
			String u = m.group(1).trim();
			if ((u.length() > 0) && !result.contains(u)) {
				System.out.println("\t" + u);
				result.add(u);
			}
		}

		// Variations might or might not include the original question.
		if (!result.contains(qna.question))
			result.add(qna.question);

		return result;
	}

	private static final String PROMPT8 = "Sentence 1: ";
	private static final String PROMPT9 = "\n" + "Sentence 2: ";
	private static final String PROMPT10 = "\n" + " \n" + "Are these sentences the same?";

	/**
	 * Compares two sentences returning true if they have same content.
	 * 
	 * @param s1
	 * @param s2
	 * @return
	 */
	public static boolean compareSentence(String s1, String s2) {

		// TODO .stop("?")
		CompletionRequest completionRequest = CompletionRequest.builder().model("text-davinci-002")
				.prompt(PROMPT8 + s1 + PROMPT9 + s2 + PROMPT10).maxTokens(MAX_COMPLETION_TOKENS).temperature(0.3)
				.frequencyPenalty(-0.0).build();
		CompletionChoice completion = SERVICE.createCompletion(completionRequest).getChoices().get(0);

		// TODO check finish reason
		return completion.getText().startsWith("Yes");
	}

	/**
	 * @return Normalized version of a text to ease keyword searches
	 */
	public static String normalizeText(String txt) {
		StringBuffer result = new StringBuffer();
		boolean space = true;
		for (char c : txt.toCharArray()) {
			if (Character.isWhitespace(c)) {
				if (!space) {
					if ((c == '\r') || (c == '\n'))
						result.append('\n');
					else
						result.append(' ');
					space = true;
				}
			} else {
				result.append(c);
				space = false;
			}
		}

		return result.toString().trim().toLowerCase();
	}

	/**
	 * @return Text split in chunks that can be used in prompts.
	 */
	private static List<String> split(String txt, String promptPrefix, String promptSuffix) {

		// Max number of words in txt that can be in prompt
		int promptLength = promptPrefix.split("\\s").length + promptSuffix.split("\\s").length;
		int maxLen = MAX_PROMPT_WORDS - promptLength;

		String[] words = txt.split("\\s");
		List<String> result = new ArrayList<>();

		StringBuilder sb = new StringBuilder();
		int wordCount = 0;
		for (String word : words) {
			if (wordCount + word.length() > maxLen) {
				result.add(sb.toString().trim());
				sb = new StringBuilder();
				wordCount = 0;
			}
			sb.append(word).append(" ");
			++wordCount;
		}
		if (sb.length() > 0) // last chunk
			result.add(sb.toString().trim());

		return result;
	}
}