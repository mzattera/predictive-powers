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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

import io.github.mzattera.util.ExtractionUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Abstract {@link EmbeddingService} that can be sub-classed to create other
 * services faster (hopefully).
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public abstract class AbstractEmbeddingService implements EmbeddingService {

	@NonNull
	@Getter
	@Setter
	private String model;

	@Getter
	private int defaultTextTokens = 150; // Assuming a page is 500 words in 4 paragraphs

	@Override
	public void setDefaultTextTokens(int maxTextTokens) {
		if (maxTextTokens < 1)
			throw new IllegalArgumentException(
					"Embedded text needs to be of at least 1 token in size: " + maxTextTokens);

		this.defaultTextTokens = maxTextTokens;
	}

	@Override
	public List<EmbeddedText> embed(String text) {
		return embed(text, defaultTextTokens, 1, 1);
	}

	@Override
	public List<EmbeddedText> embed(Collection<String> text) {
		return embed(text, defaultTextTokens, 1, 1);
	}

	@Override
	public List<EmbeddedText> embed(Collection<String> text, int chunkSize, int windowSize, int stride) {
		List<EmbeddedText> result = new ArrayList<>();
		for (String s : text)
			result.addAll(embed(s, chunkSize, windowSize, stride));
		return result;

	}

	@Override
	public List<EmbeddedText> embedFile(File file) throws IOException, SAXException, TikaException {
		return embed(ExtractionUtil.fromFile(file));
	}

	@Override
	public Map<File, List<EmbeddedText>> embedFolder(File folder) throws IOException, SAXException, TikaException {
		if (!folder.isDirectory() || !folder.canRead()) {
			throw new IOException("Cannot read folder: " + folder.getCanonicalPath());
		}

		Map<File, List<EmbeddedText>> result = new HashMap<>();
		for (File f : folder.listFiles()) {
			if (f.isFile())
				result.put(f, embedFile(f));
			else
				result.putAll(embedFolder(f));
		}

		return result;
	}

	@Override
	public List<EmbeddedText> embedURL(String url)
			throws MalformedURLException, IOException, SAXException, TikaException, URISyntaxException {
		return embedURL((new URI(url)).toURL());
	}

	@Override
	public List<EmbeddedText> embedURL(URL url) throws IOException, SAXException, TikaException {
		return embed(ExtractionUtil.fromUrl(url));
	}
}
