package io.github.mzattera.predictivepowers.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import io.github.mzattera.predictivepowers.LlmUtils;
import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.TokenCalculator;
import io.github.mzattera.predictivepowers.client.openai.embeddings.Embedding;
import io.github.mzattera.predictivepowers.client.openai.embeddings.EmbeddingsRequest;
import io.github.mzattera.predictivepowers.client.openai.embeddings.EmbeddingsResponse;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * This class creates embeddigns for text and documents. *
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@RequiredArgsConstructor
public class EmbeddingService {

	/**
	 * If a piece of text is longer than this number of tokens and no maximum text
	 * length parameter is specified, it is split in chunks of at most this size
	 * before embedding. For lists of text, each element is split separately.
	 */
	public static final int MAX_DEFAULT_TEXT_LENGTH = 100;

	/**
	 * Maximum number of tokens to send for the embedding requests, this parameter
	 * depends on the model you use and must be changed if you do not use the
	 * default embedding model (text-embedding-ada-002);
	 */
	@Getter
	@Setter
	private int maxTokens = EmbeddingsRequest.MAX_DEFAULT_MODEL_TOKENS;

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
	private final EmbeddingsRequest defaultReq;

	/**
	 * Create embeddings for given text.
	 * 
	 * As there is a maximum length for text, it might be split into several parts
	 * and single embedding returned.
	 */
	public List<EmbeddedText> embed(String text) {
		return embed(text, defaultReq, MAX_DEFAULT_TEXT_LENGTH);
	}

	/**
	 * Create embeddings for given text.
	 * 
	 * As there is a maximum length for text, it might be split into several parts
	 * and single embedding returned.
	 */
	public List<EmbeddedText> embed(String text, EmbeddingsRequest req) {
		List<String> txt = new ArrayList<>();
		txt.add(text);
		return embed(txt, req, MAX_DEFAULT_TEXT_LENGTH);
	}

	/**
	 * Create embeddings for given text.
	 * 
	 * As there is a maximum length for text, it might be split into several parts
	 * and single embedding returned.
	 * 
	 * @param maxTextLength Maximum length of each piece of text to be embedded.
	 *                      Text is split accordingly, if needed.
	 */
	public List<EmbeddedText> embed(String text, EmbeddingsRequest req, int maxTextLength) {
		List<String> txt = new ArrayList<>();
		txt.add(text);
		return embed(txt, req, maxTextLength);
	}

	/**
	 * Create embeddings for given text.
	 * 
	 * As there is a maximum length for text, each piece of text might be split into
	 * several parts and single embedding returned.
	 */
	public List<EmbeddedText> embed(String[] txt) {
		return embed(Arrays.asList(txt), defaultReq, MAX_DEFAULT_TEXT_LENGTH);
	}

	/**
	 * Create embeddings for given text.
	 * 
	 * As there is a maximum length for text, each piece of text might be split into
	 * several parts and single embedding returned.
	 */
	public List<EmbeddedText> embed(String[] txt, EmbeddingsRequest req) {
		return embed(Arrays.asList(txt), req, MAX_DEFAULT_TEXT_LENGTH);
	}

	/**
	 * Create embeddings for given text.
	 * 
	 * As there is a maximum length for text, each piece of text might be split into
	 * several parts and single embedding returned.
	 */
	public List<EmbeddedText> embed(String[] txt, EmbeddingsRequest req, int maxTextLength) {
		return embed(Arrays.asList(txt), req, maxTextLength);
	}

	/**
	 * Create embeddings for given text.
	 * 
	 * As there is a maximum length for text, each piece of text might be split into
	 * several parts and single embedding returned.
	 */
	public List<EmbeddedText> embed(Collection<String> text) {
		return embed(text, defaultReq, MAX_DEFAULT_TEXT_LENGTH);
	}

	/**
	 * Create embeddings for given text.
	 * 
	 * As there is a maximum length for text, each piece of text might be split into
	 * several parts and single embedding returned.
	 */
	public List<EmbeddedText> embed(Collection<String> text, EmbeddingsRequest req) {
		return embed(text, req, MAX_DEFAULT_TEXT_LENGTH);
	}

	/**
	 * Create embeddings for given text.
	 * 
	 * As there is a maximum length for text, each piece of text might be split into
	 * several parts and single embedding returned.
	 * 
	 * @param maxTextLength Maximum length of each piece of text to be embedded (in
	 *                      tokens). Text is split accordingly, if needed.
	 */
	public List<EmbeddedText> embed(Collection<String> text, EmbeddingsRequest req, int maxTextLength) {

		req = (EmbeddingsRequest) req.clone();
		req.setInput(new ArrayList<>());

		// Put all pieces of text to be embedded in a list
		List<String> l = new ArrayList<>();
		for (String s : text) {
			l.addAll(LlmUtils.split(s, Math.min(maxTextLength, maxTokens)));
		}

		// Embed as many pieces you can in a single call
		List<EmbeddedText> result = new ArrayList<>();
		int tok = 0;
		while (l.size() > 0) {
			String s = l.remove(0);
			int t = TokenCalculator.count(s);

			if ((tok + t) > maxTokens) {
				// too many tokens, embed what you have
				result.addAll(embed(req));
				req.getInput().clear();
				tok = 0;
			}

			// add to next request
			req.getInput().add(s);
			tok += t;
		}

		// last bit
		if (req.getInput().size() > 0) {
			result.addAll(embed(req));
		}

		return result;
	}

	private List<EmbeddedText> embed(EmbeddingsRequest req) {

		List<EmbeddedText> result = new ArrayList<>();
		EmbeddingsResponse res = ep.getClient().createEmbeddings(req);
		
		for (Embedding e : res.getData()) {
			int index = e.getIndex();
			EmbeddedText et = EmbeddedText.builder().text(req.getInput().get(index)).embedding(e.getEmbedding())
					.model(res.getModel()).build();
			result.add(et);
		}

		return result;
	}
}
