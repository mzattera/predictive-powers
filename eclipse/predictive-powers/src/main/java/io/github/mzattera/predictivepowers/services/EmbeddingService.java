/*
 * Copyright 2023 Massimiliano "Maxi" Zattera
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.mzattera.predictivepowers.services;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

/**
 * A service that provides text embedding.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public interface EmbeddingService extends AiService {

	/**
	 * Maximum number of tokens for each piece of text being embedded. If text is
	 * longer, it is split in multiple parts before embedding.
	 */
	int getMaxTextTokens();

	/**
	 * Maximum number of tokens for each piece of text being embedded. If text is
	 * longer, it is split in multiple parts before embedding.
	 */
	void setMaxTextTokens(int maxTokens);

	/**
	 * Create embeddings for given text.
	 * 
	 * As there is a maximum length for text being embedded, the input might be
	 * split into several parts before embedding.
	 */
	List<EmbeddedText> embed(String text);

	/**
	 * Create embeddings for given text. Before embedding, the text is chunked
	 * following the algorithm described in
	 * {@link io.github.mzattera.util.ChunkUtil}.
	 * 
	 * As there is a maximum length for texts being embedded, each resulting chunk
	 * might be split into several parts before embedding.
	 */
	List<EmbeddedText> embed(String text, int chunkSize, int windowSize, int stride);

	/**
	 * Create embeddings for given set of texts.
	 * 
	 * As there is a maximum length for texts being embedded, each text in the input
	 * set might be split into several parts before embeddings are returned.
	 */
	List<EmbeddedText> embed(Collection<String> text);

	/**
	 * Create embeddings for given set of texts. Before embedding, each text in the
	 * input set is chunked following the algorithm described in
	 * {@link io.github.mzattera.util.ChunkUtil}.
	 * 
	 * As there is a maximum length for texts being embedded, each chunk might be
	 * split into several parts before embeddings are returned.
	 */
	List<EmbeddedText> embed(Collection<String> text, int chunkSize, int windowSize, int stride);

	// TODO add the same methods below but with sliding window for chunking? 
	/**
	 * Embed content of given file.
	 */
	List<EmbeddedText> embedFile(File file) throws IOException, SAXException, TikaException;

	/**
	 * Embeds all files in given folder, including contents of its sub-folders.
	 */
	Map<File, List<EmbeddedText>> embedFolder(File folder) throws IOException, SAXException, TikaException;

	/**
	 * Embeds text of given web page.
	 */
	List<EmbeddedText> embedURL(String url) throws MalformedURLException, IOException, SAXException, TikaException;

	/**
	 * Embeds text of given web page.
	 */
	List<EmbeddedText> embedURL(URL url) throws IOException, SAXException, TikaException;
}