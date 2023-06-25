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
public interface EmbeddingService extends Service {
	
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
	 * As there is a default maximum length for text, it might be split into several
	 * parts before embeddings are returned.
	 */
	List<EmbeddedText> embed(String text);

	/**
	 * Create embeddings for given text.
	 * 
	 * As there is a default maximum length for text, each element might be split
	 * into several parts before embeddings are returned.
	 */
	List<EmbeddedText> embed(Collection<String> text);

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