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

import io.github.mzattera.predictivepowers.util.ExtractionUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Abstract {@link EmbeddingService} that can be sub-classed to easily create
 * other services faster (hopefully).
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractEmbeddingService implements EmbeddingService {

	@NonNull
	@Getter
	@Setter
	private String model;

	@Getter
	private int defaultChunkTokens = 800; // Just because OpenAI uses 800 tokens and chunk_overlap_tokens of 400.

	@Override
	public void setDefaultChunkTokens(int defaultTokens) {
		if (defaultTokens < 1)
			throw new IllegalArgumentException(
					"Embedded text needs to be of at least 1 token in size: " + defaultTokens);

		this.defaultChunkTokens = defaultTokens;
	}

	@Override
	public List<EmbeddedText> embed(@NonNull String text) {
		return embed(text, defaultChunkTokens, 1, 1);
	}

	@Override
	public List<EmbeddedText> embed(@NonNull Collection<String> text) {
		return embed(text, defaultChunkTokens, 1, 1);
	}

	@Override
	public List<EmbeddedText> embed(@NonNull Collection<String> text, int chunkSize, int windowSize, int stride) {
		List<EmbeddedText> result = new ArrayList<>();
		for (String s : text)
			result.addAll(embed(s, chunkSize, windowSize, stride));
		return result;

	}

	@Override
	public List<EmbeddedText> embedFile(@NonNull File file) throws IOException, SAXException, TikaException {
		return embed(ExtractionUtil.fromFile(file));
	}

	@Override
	public Map<File, List<EmbeddedText>> embedFolder(@NonNull File folder) throws IOException, SAXException, TikaException {
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
	public List<EmbeddedText> embedURL(@NonNull String url)
			throws MalformedURLException, IOException, SAXException, TikaException, URISyntaxException {
		return embedURL((new URI(url)).toURL());
	}

	@Override
	public List<EmbeddedText> embedURL(@NonNull URL url) throws IOException, SAXException, TikaException {
		return embed(ExtractionUtil.fromUrl(url));
	}

	@Override
	public void close() {
	}
}
