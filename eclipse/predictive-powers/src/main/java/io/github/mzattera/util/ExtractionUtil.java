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

/**
 * 
 */
package io.github.mzattera.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

/**
 * Utility methods to extract content from files.
 * 
 * @author Massimiliano "maxi" Zattera
 *
 */
public final class ExtractionUtil {

	/**
	 * 
	 * @param fileName Name of the file from where to get content.
	 * @return The content of given file, as plain text.
	 * 
	 * @throws IOException
	 * @throws SAXException
	 * @throws TikaException
	 */
	public static String fromFile(String fileName) throws IOException, SAXException, TikaException {
		return fromFile(new File(fileName));
	}

	/**
	 * 
	 * @param file The file from where to get content.
	 * @return The content of given file, as plain text.
	 * 
	 * @throws IOException
	 * @throws SAXException
	 * @throws TikaException
	 */
	public static String fromFile(File file) throws IOException, SAXException, TikaException {
		if (!file.isFile() || !file.canRead()) {
			throw new IOException("File cannot be read from: " + file.getCanonicalPath());
		}

		try (FileInputStream in = new FileInputStream(file)) {
			return fromStream(in);
		}
	}

	/**
	 * 
	 * @param url Web page URL.
	 * @return The content of given web page.
	 * 
	 * @throws IOException
	 * @throws SAXException
	 * @throws TikaException
	 */
	public static String fromUrl(String url) throws IOException, SAXException, TikaException {
		return fromUrl(new URL(url));
	}

	/**
	 * 
	 * @param url Web page URL.
	 * @return The content of given web page.
	 * 
	 * @throws IOException
	 * @throws SAXException
	 * @throws TikaException
	 */
	public static String fromUrl(URL url) throws IOException, SAXException, TikaException {
		try (InputStream in = url.openStream()) {
			return fromStream(in);
		}
	}

	/**
	 * 
	 * @param stream Stream to read from.
	 * @return The content of given stream, as plain text.
	 * 
	 * @throws IOException
	 * @throws SAXException
	 * @throws TikaException
	 */
	public static String fromStream(InputStream stream) throws IOException, SAXException, TikaException {
		AutoDetectParser parser = new AutoDetectParser();
		BodyContentHandler handler = new BodyContentHandler(-1);
		Metadata metadata = new Metadata();
		parser.parse(stream, handler, metadata);
		return handler.toString().trim(); // Seems Tika adds a NL at tend of text that is not there 
	}
}