/*
 * Copyright 2024 Massimiliano "Maxi" Zattera
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

import io.github.mzattera.util.ImageUtil;
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

	public Base64FilePart(@NonNull File file) throws IOException {
		super(file);
		init(super.getInputStream().readAllBytes(), super.getName());
	}

	public Base64FilePart(@NonNull File file, String mimeType) throws IOException {
		super(file, mimeType);
		init(super.getInputStream().readAllBytes(), super.getName());
	}

	public Base64FilePart(@NonNull URL url) throws IOException {
		super(url);
		init(super.getInputStream().readAllBytes(), super.getName());
	}

	public Base64FilePart(@NonNull URL url, String mimeType) throws IOException {
		super(url, mimeType);
		init(super.getInputStream().readAllBytes(), super.getName());
	}

	public Base64FilePart(String encodedContent, String name, String mimeType) {
		super(mimeType);
		init(Base64.getDecoder().decode(encodedContent), name);
	}

	public Base64FilePart(InputStream in, String name, String mimeType) throws IOException {
		super(mimeType);
		init(in.readAllBytes(), name);
	}

	public Base64FilePart(byte[] bytes, String name, String mimeType) {
		super(mimeType);
		init(bytes, name);
	}

	public Base64FilePart(FilePart file) throws IOException {
		super(file.getMimeType());
		init(file.getInputStream().readAllBytes(), file.getName());
	}

	private void init(byte[] bytes, String name) {
		encodedContent = Base64.getEncoder().encodeToString(bytes);
		this.name = name;
	}

	/**
	 * Return a image scaled down for OpenAI vision models.
	 * 
	 * As the image is scaled down anyway before being submitted to the model, it
	 * makes sense to scale down local images before sending them to the API. This
	 * saves tokens and reduces latency (see
	 * {@linkplain https://platform.openai.com/docs/guides/vision}).
	 * 
	 * @return The same image if it is already scaled down, or its scaled down
	 *         version.
	 * @throws IOException
	 */
	public static Base64FilePart forOpenAiImage(FilePart file) throws IOException {

		BufferedImage img = ImageUtil.fromBytes(file.getInputStream());

		int w = img.getWidth();
		int h = img.getHeight();
		double scale1 = 2000d / w;
		double scale2 = 768d / h;
		double scale = Math.min(scale1, scale2);

		if (scale < 1.0d) {
			w *= scale;
			h *= scale;
			BufferedImage resizedImage = new BufferedImage(w, h, img.getType());
			Graphics2D graphics2D = resizedImage.createGraphics();
			graphics2D.drawImage(img, 0, 0, w, h, null);
			graphics2D.dispose();
			return new Base64FilePart(ImageUtil.toBytes("png", resizedImage), file.getName(), "image/png");
		} else {
			return new Base64FilePart(file);
		}
	}

	/**
	 * Return a image scaled down for Anthropic vision models.
	 * 
	 * As the image is scaled down anyway before being submitted to the model, it
	 * makes sense to scale down local images before sending them to the API. This
	 * saves tokens and reduces latency (see
	 * {@linkplain https://docs.anthropic.com/claude/docs/vision}).
	 * 
	 * @return The same image, if it is already scaled down, otherwise its scaled
	 *         down version.
	 * @throws IOException
	 */
	public static Base64FilePart forAnthropicImage(FilePart file) throws IOException {

		BufferedImage img = ImageUtil.fromBytes(file.getInputStream());

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
			return new Base64FilePart(ImageUtil.toBytes("png", resizedImage), file.getName(), "image/png");
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
