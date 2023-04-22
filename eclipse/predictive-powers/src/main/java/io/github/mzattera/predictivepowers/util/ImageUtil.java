/**
 * 
 */
package io.github.mzattera.predictivepowers.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;

/**
 * Image utilities to read and write images.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
public final class ImageUtil {

	private ImageUtil() {
	}

	/**
	 * Reads Java image from file.
	 * 
	 * @throws IOException
	 */
	public static BufferedImage fromFile(File file) throws IOException {
		return ImageIO.read(file);
	}

	/**
	 * Reads Java image from file.
	 */
	public static BufferedImage fromFile(String fileName) throws IOException {
		return ImageIO.read(new File(fileName));
	}

	/**
	 * Reads Java image from a URL.
	 */
	public static BufferedImage fromUrl(String url) throws IOException {
		return fromUrl(new URL(url));
	}

	/**
	 * Reads Java image from a URL.
	 * 
	 * @throws IOException
	 */
	public static BufferedImage fromUrl(URL url) throws IOException {
		return ImageIO.read(url);
	}

	/**
	 * Reads Java image from its base64 representation.
	 */
	public static BufferedImage fromBase64(String base64Image) throws IOException {
		return ImageIO.read(new ByteArrayInputStream(DatatypeConverter.parseBase64Binary(base64Image)));
	}

	/**
	 * Write Java image into a file.
	 */
	public static void write(String fileName, BufferedImage image) throws IOException {
		write(new File(fileName), image);
	}

	/**
	 * Write Java image into a file.
	 */
	public static void write(File file, BufferedImage image) throws IOException {
		ImageIO.write(image, getExtension(file), file);
	}

	private static String getExtension(File f) {
		String name = f.getName();
		int p = name.lastIndexOf('.');
		if ((p == -1) || (p == name.length() - 1))
			return "";
		return name.substring(p + 1);
	}
}
