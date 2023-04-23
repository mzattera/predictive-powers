package io.github.mzattera.predictivepowers.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.knowledge.ExtractionUtils;
import io.github.mzattera.predictivepowers.openai.client.embeddings.Embedding;
import io.github.mzattera.predictivepowers.openai.client.embeddings.EmbeddingsRequest;
import io.github.mzattera.predictivepowers.openai.client.embeddings.EmbeddingsResponse;
import io.github.mzattera.util.LlmUtil;
import io.github.mzattera.util.TokenCalculator;
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
	 * Maximum number of tokens for each piece of text being embedded. If text is
	 * bigger than this, it is split in multiple parts before embedding.
	 * 
	 * This is only a default value, that can be overwritten at each call.
	 * 
	 * Notice it is limited by the model context size.
	 */
	@Getter
	@Setter
	private int defaultMaxTokens = 75;

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
	 * As there is a default maximum length for text, it might be split into several parts
	 * and single embedding returned.
	 */
	public List<EmbeddedText> embed(String text) {
		return embed(text, defaultMaxTokens, defaultReq);
	}

	/**
	 * Create embeddings for given text.
	 * 
	 * As there is a maximum length for text, it might be split into several parts
	 * and single embedding returned.
	 * 
	 * @param maxTokens Maximum length in tokens of each piece of text to be embedded.
	 *                      Text is split accordingly, if needed.
	 */
	public List<EmbeddedText> embed(String text, int maxTokens) {
		return embed(text, maxTokens, defaultReq);
	}

	/**
	 * Create embeddings for given text.
	 * 
	 * As there is a maximum length for text, it might be split into several parts
	 * and single embedding returned.
	 * 
	 * @param maxTokens Maximum length in tokens of each piece of text to be embedded.
	 *                      Text is split accordingly, if needed.
	 */
	public List<EmbeddedText> embed(String text, int maxTokens, EmbeddingsRequest req) {
		List<String> txt = new ArrayList<>();
		txt.add(text);
		return embed(txt, maxTokens, req);
	}

	/**
	 * Create embeddings for given text.
	 * 
	 * As there is a default maximum length for text, each piece of text might be split into
	 * several parts and single embedding returned.
	 */
	public List<EmbeddedText> embed(String[] txt) {
		return embed(Arrays.asList(txt), defaultMaxTokens, defaultReq);
	}

	/**
	 * Create embeddings for given text.
	 * 
	 * As there is a maximum length for text, each piece of text might be split into
	 * several parts and single embedding returned.
	 * 
	 * @param maxTokens Maximum length in tokens of each piece of text to be embedded.
	 *                      Text is split accordingly, if needed.
	 */
	public List<EmbeddedText> embed(String[] txt, int maxTokens) {
		return embed(Arrays.asList(txt), maxTokens, defaultReq);
	}

	/**
	 * Create embeddings for given text.
	 * 
	 * As there is a maximum length for text, each piece of text might be split into
	 * several parts and single embedding returned.
	 * 
	 * @param maxTokens Maximum length in tokens of each piece of text to be embedded.
	 *                      Text is split accordingly, if needed.
	 */
	public List<EmbeddedText> embed(String[] txt, int maxTokens, EmbeddingsRequest req) {
		return embed(Arrays.asList(txt), maxTokens, req);
	}

	/**
	 * Create embeddings for given text.
	 * 
	 * As there is a default maximum length for text, each piece of text might be split into
	 * several parts and single embedding returned.
	 */
	public List<EmbeddedText> embed(Collection<String> text) {
		return embed(text, defaultMaxTokens, defaultReq);
	}

	/**
	 * Create embeddings for given text.
	 * 
	 * As there is a maximum length for text, each piece of text might be split into
	 * several parts and single embedding returned.
	 * 
	 * @param maxTokens Maximum length in tokens of each piece of text to be embedded.
	 *                      Text is split accordingly, if needed.
	 */
	public List<EmbeddedText> embed(Collection<String> text, int maxTokens) {
		return embed(text, maxTokens, defaultReq);
	}

	/**
	 * Create embeddings for given text.
	 * 
	 * As there is a maximum length for text, each piece of text might be split into
	 * several parts and single embedding returned.
	 * 
	 * @param maxTokens Maximum length in tokens of each piece of text to be embedded (in
	 *                      tokens). Text is split accordingly, if needed.
	 */
	public List<EmbeddedText> embed(Collection<String> text, int maxTokens, EmbeddingsRequest req) {

		req.getInput().clear();

		// Put all pieces of text to be embedded in a list
		List<String> l = new ArrayList<>();
		for (String s : text) {
			l.addAll(LlmUtil.split(s, maxTokens));
		}

		// Embed as many pieces you can in a single call
		List<EmbeddedText> result = new ArrayList<>();
		int tok = 0;
		while (l.size() > 0) {
			String s = l.remove(0);
			int t = TokenCalculator.count(s);

			if ((tok + t) > defaultMaxTokens) {
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

	/**
	 * Embeds content of given file.
	 * 
	 * @param fileName
	 * @throws IOException
	 * @throws SAXException
	 * @throws TikaException
	 */
	public List<EmbeddedText> embedFile(String fileName) throws IOException, SAXException, TikaException {
		return embedFile(new File(fileName), defaultMaxTokens);
	}

	/**
	 * Embed content of given file.
	 * 
	 * @param file
	 * @throws IOException
	 * @throws SAXException
	 * @throws TikaException
	 */
	public List<EmbeddedText> embedFile(File file) throws IOException, SAXException, TikaException {
		return embedFile(file, defaultMaxTokens);
	}

	/**
	 * Embed content of given file.
	 * 
	 * @param file
	 * @throws IOException
	 * @throws SAXException
	 * @throws TikaException
	 */
	public List<EmbeddedText> embedFile(File file, int maxTextLength) throws IOException, SAXException, TikaException {
		String content = ExtractionUtils.getText(file);
		return embed(content, maxTextLength);
	}

	/**
	 * Embeds all files in given folder, including contents of its sub-folders.
	 * 
	 * @param folderName
	 * @return
	 * @throws IOException
	 * @throws SAXException
	 * @throws TikaException
	 * @returns A Map from each embedded file into its contents.
	 */
	public Map<File, List<EmbeddedText>> embedFolder(String folderName)
			throws IOException, SAXException, TikaException {
		return embedFolder(new File(folderName), defaultMaxTokens);
	}

	/**
	 * Embeds all files in given folder, including contents of its sub-folders.
	 * 
	 * @param folderName
	 * @return
	 * @throws IOException
	 * @throws SAXException
	 * @throws TikaException
	 * @returns A Map from each embedded file into its contents.
	 */
	public Map<File, List<EmbeddedText>> embedFolder(File folder) throws IOException, SAXException, TikaException {
		return embedFolder(folder, defaultMaxTokens);
	}

	/**
	 * Embeds all files in given folder, including contents of its sub-folders.
	 * 
	 * @param folderName
	 * @return
	 * @throws IOException
	 * @throws SAXException
	 * @throws TikaException
	 * @returns A Map from each embedded file into its contents.
	 */
	public Map<File, List<EmbeddedText>> embedFolder(File folder, int maxTextLength)
			throws IOException, SAXException, TikaException {
		if (!folder.isDirectory() || !folder.canRead()) {
			throw new IOException("Folder cannot be read from: " + folder.getCanonicalPath());
		}

		Map<File, List<EmbeddedText>> result = new HashMap<>();
		for (File f : folder.listFiles()) {
			if (f.isFile())
				result.put(f, embedFile(f, maxTextLength));
			else
				result.putAll(embedFolder(f, maxTextLength));
		}

		return result;
	}

}
