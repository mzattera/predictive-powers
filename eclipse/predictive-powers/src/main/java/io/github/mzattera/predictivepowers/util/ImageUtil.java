/*
 * Copyright 2023-2025 Massimiliano "Maxi" Zattera
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

package io.github.mzattera.predictivepowers.util;

import java.awt.Color;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

import io.github.mzattera.predictivepowers.services.messages.Base64FilePart;
import io.github.mzattera.predictivepowers.services.messages.FilePart;
import lombok.NonNull;

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
	 * Reads Java image from a {@link FilePart}.
	 */
	public static BufferedImage fromFilePart(FilePart file) throws IOException {
		return ImageIO.read(file.getInputStream());
	}

	/**
	 * Reads Java image from its base64 representation.
	 */
	public static BufferedImage fromBase64(String base64Image) throws IOException {
		return ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(base64Image)));	}

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
	public static File toFile(BufferedImage image, String fileName) throws IOException {
		return toFile(image, new File(fileName));
	}

	/**
	 * Write Java image into a file.
	 */
	public static File toFile(BufferedImage image, File file) throws IOException {
		String format = FileUtil.getExtension(file);
		if (format.isBlank())
			format = "png";
		ImageIO.write(image, format, file);
		return file;
	}

	/**
	 * Write Java image into a byte[].
	 * 
	 * @param image
	 * @param ext   An extension specifying the format of the saved image (e.g.
	 *              "jpg" or "png").
	 */
	public static byte[] toBytes(BufferedImage image, String ext) throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ImageIO.write(image, ext, baos);
			return baos.toByteArray();
		}
	}

	/**
	 * @return A list of {@link Base64FilePart} to hold given images.
	 * @throws IOException
	 */
	public static List<FilePart> toFilePart(@NonNull List<BufferedImage> images) throws IOException {
		List<FilePart> result = new ArrayList<>(images.size());
		for (BufferedImage image : images)
			result.add(new Base64FilePart(image, "img_" + UUID.randomUUID()));
		return result;
	}

	/**
	 * @return A {@link Base64FilePart} to hold given image, or null if the image is
	 *         null.
	 * @throws IOException
	 */
	public static Base64FilePart toFilePart(BufferedImage image) throws IOException {
		return (image == null) ? null : new Base64FilePart(image, "img_" + UUID.randomUUID());
	}

	/**
	 * Reads an image from the input stream, pads it with black borders to make it
	 * square, and returns it as a PNG-encoded byte array.
	 *
	 * @author Luna
	 * @param inputStream the input stream containing the image data
	 * @return the squared image as a PNG byte array
	 * @throws IOException if reading or writing fails
	 */
	public static byte[] padImageToSquare(InputStream inputStream) throws IOException {
		BufferedImage original = ImageIO.read(inputStream);
		if (original == null) {
			throw new IOException("Input stream does not contain a valid image.");
		}

		int width = original.getWidth();
		int height = original.getHeight();

		if (width == height)
			return toBytes(original, "png");

		int size = Math.max(width, height);

		// Create square image with black background
		BufferedImage squaredImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = squaredImage.createGraphics();

		// Fill with black
		g2d.setColor(Color.BLACK);
		g2d.fillRect(0, 0, size, size);

		// Draw original image centered
		int x = (size - width) / 2;
		int y = (size - height) / 2;
		g2d.drawImage(original, x, y, null);
		g2d.dispose();

		return toBytes(squaredImage, "png");
	}
}
