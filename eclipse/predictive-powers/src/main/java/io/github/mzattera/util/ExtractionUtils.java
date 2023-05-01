/**
 * 
 */
package io.github.mzattera.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

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
public final class ExtractionUtils {

	/**
	 * 
	 * @param fileName Name of the file from where to get content.
	 * @return The content of given file, as plain text.
	 * 
	 * @throws IOException
	 * @throws SAXException
	 * @throws TikaException
	 */
	public static String getText(String fileName) throws IOException, SAXException, TikaException {
		return getText(new File(fileName));
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
	public static String getText(File file) throws IOException, SAXException, TikaException {
		if (!file.isFile() || !file.canRead()) {
			throw new IOException("File cannot be read from: " + file.getCanonicalPath());
		}

		try (FileInputStream in = new FileInputStream(file)) {
			return getText(in);
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
	public static String getText(FileInputStream stream) throws IOException, SAXException, TikaException {
		AutoDetectParser parser = new AutoDetectParser();
		BodyContentHandler handler = new BodyContentHandler(-1);
		Metadata metadata = new Metadata();
		parser.parse(stream, handler, metadata);
		return handler.toString();
	}
}