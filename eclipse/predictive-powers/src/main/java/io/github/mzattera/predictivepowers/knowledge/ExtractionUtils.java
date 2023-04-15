/**
 * 
 */
package io.github.mzattera.predictivepowers.knowledge;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

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
		AutoDetectParser parser = new AutoDetectParser();
		BodyContentHandler handler = new BodyContentHandler();
		Metadata metadata = new Metadata();

		try (InputStream stream = new FileInputStream(new File(fileName))) {
			parser.parse(stream, handler, metadata);
		}

		return handler.toString();

//			System.out.println("Contents of the document:" + handler.toString());
//			System.out.println("Metadata of the document:");
//			String[] metadataNames = metadata.names();
//			for (String name : metadataNames) {
//				System.out.println(name + " : " + metadata.get(name));
//			}
	}
}
