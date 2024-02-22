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

package io.github.mzattera.util;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;

import okhttp3.MediaType;
import okhttp3.RequestBody;

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
	 * 
	 * @throws URISyntaxException
	 */
	public static BufferedImage fromUrl(String url) throws IOException, URISyntaxException {
		return fromUrl((new URI(url)).toURL());
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
	 * Reads Java image from a stream.
	 */
	public static BufferedImage fromBytes(InputStream rawImage) throws IOException {
		return ImageIO.read(rawImage);
	}

	/**
	 * Reads Java image from a byte[].
	 */
	public static BufferedImage fromBytes(byte[] rawImage) throws IOException {
		return ImageIO.read(new ByteArrayInputStream(rawImage));
	}

	/**
	 * Write Java image into a file, then return a link to the file itself.
	 */
	public static File toFile(String fileName, BufferedImage image) throws IOException {
		return toFile(new File(fileName), image);
	}

	/**
	 * Write Java image into a file.
	 */
	public static File toFile(File file, BufferedImage image) throws IOException {
		ImageIO.write(image, getExtension(file), file);
		return file;
	}

	private static String getExtension(File f) {
		String name = f.getName();
		int p = name.lastIndexOf('.');
		if ((p == -1) || (p == name.length() - 1))
			return "";
		return name.substring(p + 1);
	}

	/**
	 * Write Java image into an HTTP request body.
	 * 
	 * @param ext   An extension specifying the format of the saved image (e.g.
	 *              "jpg" or "png").
	 * @param image
	 */
	public static RequestBody toRequestBody(String ext, BufferedImage image) throws IOException {
		return RequestBody.create(MediaType.parse("image/" + ext), toBytes(ext, image));
	}

	/**
	 * Write Java image into a byte[].
	 * 
	 * @param ext   An extension specifying the format of the saved image (e.g.
	 *              "jpg" or "png").
	 * @param image
	 */
	public static byte[] toBytes(String ext, BufferedImage image) throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ImageIO.write(image, ext, baos);
			return baos.toByteArray();
		}
	}

	/**
	 * Scale down an image for OpenAI vision models.
	 * 
	 * As the image is scaled down anyway before being sumbitted to the model, it
	 * makes sense to scale down local images before sending them to the API. THis
	 * saves tokens and reduces latency (see
	 * {@linkplain https://platform.openai.com/docs/guides/vision Managing Images}.
	 * 
	 * @return The same image if it is already scaled down, or its scaled down
	 *         version.
	 */
	public static BufferedImage scaleDown(BufferedImage img) {
		int w = img.getWidth();
		int h = img.getHeight();
		double scale1 = 2000d / Math.max(w, h);
		double scale2 = 768d / Math.min(w, h);
		double scale = Math.min(scale1, scale2);

		if (scale < 1.0d) {
			w *= scale;
			h *= scale;
			BufferedImage resizedImage = new BufferedImage(w, h, img.getType());
			Graphics2D graphics2D = resizedImage.createGraphics();
			graphics2D.drawImage(img, 0, 0, w, h, null);
			graphics2D.dispose();
			return resizedImage;
		}

		return img;
	}
}
