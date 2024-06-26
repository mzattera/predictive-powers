/*
 * Copyright 2023-2024 Massimiliano "Maxi" Zattera
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
import java.net.URISyntaxException;
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
	
	// TODO When using embeddings or list of strings, would be nice to avoid duplicates, e.g. an embedding for a short sentence which is already contained in an embedding for a bigger sentence.

	/**
	 * Get default number of tokens for each piece of text being embedded. This is
	 * used if no chunk size was specified when embedding content.
	 */
	int getDefaultTextTokens();

	/**
	 * Set default number of tokens for each piece of text being embedded. This is
	 * used if no chunk size was specified when embedding content.
	 */
	void setDefaultTextTokens(int defaultTokens);

	/**
	 * Create embeddings for given text. Text is split in chunks of
	 * {@link #getDefaultTextTokens()} before it is embedded.
	 * 
	 * As embedding model might have a maximum length for text being embedded, the
	 * input might be split into several parts before embedding.
	 */
	List<EmbeddedText> embed(String text);

	/**
	 * Create embeddings for given text. Before embedding, the text is chunked
	 * following the algorithm described in
	 * {@link io.github.mzattera.util.ChunkUtil}.
	 * 
	 * As embedding model might have a maximum length for text being embedded, each
	 * resulting chunk might be further split into several parts before embedding.
	 */
	List<EmbeddedText> embed(String text, int chunkSize, int windowSize, int stride);

	/**
	 * Create embeddings for given set of texts. Each text is split in chunks of
	 * {@link #getDefaultTextTokens()} before it is embedded.
	 * 
	 * As embedding model might have a maximum length for text being embedded, each
	 * chunk might be further split into several parts before embeddings are
	 * returned.
	 */
	List<EmbeddedText> embed(Collection<String> text);

	/**
	 * Create embeddings for given set of texts. Before embedding, each text in the
	 * input set is chunked following the algorithm described in
	 * {@link io.github.mzattera.util.ChunkUtil#split(String, int, int, int, io.github.mzattera.predictivepowers.services.ModelService.Tokenizer)}.
	 * 
	 * As embedding model might have a maximum length for text being embedded, each
	 * chunk might be further split into several parts before embeddings are
	 * returned.
	 */
	List<EmbeddedText> embed(Collection<String> text, int chunkSize, int windowSize, int stride);

	// TODO add the same methods below but with sliding window for chunking?

	/**
	 * Same as calling {@link #embed(String)} using content of given file as input.
	 */
	List<EmbeddedText> embedFile(File file) throws IOException, SAXException, TikaException;

	/**
	 * Same as calling {@link #embed(String)} using content of each file in given
	 * folder, including contents of its sub-folders.
	 */
	Map<File, List<EmbeddedText>> embedFolder(File folder) throws IOException, SAXException, TikaException;

	/**
	 * Same as calling {@link #embed(String)} using content at given URL.
	 */
	List<EmbeddedText> embedURL(String url) throws IOException, SAXException, TikaException, URISyntaxException;

	/**
	 * Same as calling {@link #embed(String)} using content at given URL.
	 */
	List<EmbeddedText> embedURL(URL url) throws IOException, SAXException, TikaException;
}