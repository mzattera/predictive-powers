/*
 * Copyright 2024-2025 Massimiliano "Maxi" Zattera
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
package io.github.mzattera.predictivepowers.services.messages;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Base64;

import io.github.mzattera.predictivepowers.util.FileUtil;
import io.github.mzattera.predictivepowers.util.ImageUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * This is a {@link FilePart} that contains the base64 encoding of a file.
 * Notice both {@link #getFile()} and {@link #getUrl()} will return null, whilst
 * {@link #isLocalFile()} returns true, as the file content is stored in this
 * object.
 * 
 * @author Massimiliano "Maxi" Zattera.
 */
@SuperBuilder
@Getter
@Setter
@ToString
public class Base64FilePart extends FilePart {

	/** Content encoded as base64, */
	@Getter
	private String encodedContent;

	@Getter
	private String name;

	@Override
	public Type getType() {
		return Type.BASE64_FILE;
	}

	/**
	 * Constructor from file; notice the file will be inspected to determine MIME
	 * type.
	 */
	public Base64FilePart(@NonNull File file) throws IOException {
		super(file);
		try (InputStream s = super.getInputStream()) {
			init(s.readAllBytes(), super.getName(), super.getMimeType());
		}
	}

	/**
	 * Constructor from file.
	 */
	public Base64FilePart(@NonNull File file, String mimeType) throws IOException {
		super(file, mimeType);
		try (InputStream s = super.getInputStream()) {
			init(s.readAllBytes(), super.getName(), mimeType);
		}
	}

	/**
	 * Constructor from URL; notice the URL will be inspected to determine MIME
	 * type.
	 */
	public Base64FilePart(@NonNull URL url) throws IOException {
		super(url);
		try (InputStream s = super.getInputStream()) {
			init(s.readAllBytes(), super.getName(), super.getMimeType());
		}
	}

	/**
	 * Constructor from URL.
	 */
	public Base64FilePart(@NonNull URL url, String mimeType) throws IOException {
		super(url, mimeType);
		try (InputStream s = super.getInputStream()) {
			init(s.readAllBytes(), super.getName(), mimeType);
		}
	}

	/**
	 * Constructor from image; a name for this file must be provided. The image is
	 * encoded as base64 PNG.
	 */
	public Base64FilePart(BufferedImage image, String name) throws IOException {
		init(ImageUtil.toBytes(image, "png"), name, "image/png");
	}

	/**
	 * Constructor from encoded content; a name for this file must be provided.
	 * Notice the content is inspected to determine MIME type.
	 */
	public Base64FilePart(String encodedContent, String name) {
		init(Base64.getDecoder().decode(encodedContent), name, null);
	}

	/**
	 * Constructor from encoded content; a name for this file must be provided.
	 */
	public Base64FilePart(String encodedContent, String name, String mimeType) {
		init(Base64.getDecoder().decode(encodedContent), name, mimeType);
	}

	/**
	 * Constructor from stream content; a name for this file must be provided.
	 * Notice the content is inspected to determine MIME type.
	 */
	public Base64FilePart(InputStream in, String name) throws IOException {
		init(in.readAllBytes(), name, null);
	}

	/**
	 * Constructor from stream content; a name for this file must be provided
	 */
	public Base64FilePart(InputStream in, String name, String mimeType) throws IOException {
		init(in.readAllBytes(), name, mimeType);
	}

	/**
	 * Constructor from byte content; a name for this file must be provided. Notice
	 * the content is inspected to determine MIME type.
	 */
	public Base64FilePart(byte[] bytes, String name) {
		init(bytes, name, null);
	}

	/**
	 * Constructor from byte content; a name for this file must be provided.
	 */
	public Base64FilePart(byte[] bytes, String name, String mimeType) {
		init(bytes, name, mimeType);
	}

	/**
	 * "Copy" constructor.
	 */
	public Base64FilePart(FilePart file) throws IOException {
		try (InputStream s = file.getInputStream()) {
			init(s.readAllBytes(), file.getName(), file.getMimeType());
		}
	}

	private void init(byte[] bytes, String name, String mimeType) {
		this.name = name;
		this.encodedContent = Base64.getEncoder().encodeToString(bytes);
		if (mimeType == null) {
			this.setMimeType(FileUtil.getMimeType(bytes));
		} else {
			this.setMimeType(mimeType);
		}
	}

	/**
	 * Return a image scaled down for Anthropic vision models.
	 * 
	 * As the image is scaled down anyway before being submitted to the model, it
	 * makes sense to scale down local images before sending them to the API. This
	 * saves tokens and reduces latency.
	 * 
	 * @see <a href="https://docs.anthropic.com/claude/docs/vision">Vision guide</a>
	 * 
	 * @return The same image, if it is already scaled down, otherwise its scaled
	 *         down version.
	 * @throws IOException
	 */
	// TODO URGENT move it out of here
	public static Base64FilePart forAnthropicImage(FilePart file) throws IOException {

		BufferedImage img;
		try (InputStream s = file.getInputStream()) {
			img = ImageUtil.fromBytes(s);
		}

		int w = img.getWidth();
		int h = img.getHeight();

		double scale1 = 1568d / Math.max(w, h); // Longest edge must be < 1568
		double scale2 = Math.sqrt(1_150_000d / (w * h)); // Image < 1.15 Mpixel, or ~1600 tokens
		double scale = Math.min(scale1, scale2);

		if (scale < 1.0d) {
			w *= scale;
			h *= scale;
			BufferedImage resizedImage = new BufferedImage(w, h, img.getType());
			Graphics2D graphics2D = resizedImage.createGraphics();
			graphics2D.drawImage(img, 0, 0, w, h, null);
			graphics2D.dispose();
			return new Base64FilePart(ImageUtil.toBytes(resizedImage, "png"), file.getName(), "image/png");
		} else {
			return new Base64FilePart(file);
		}
	}

	@Override
	public boolean isLocalFile() {
		return true;
	}

	@Override
	public boolean isWebFile() {
		return false;
	}

	@Override
	public InputStream getInputStream() {
		return new ByteArrayInputStream(Base64.getDecoder().decode(encodedContent));
	}
}
