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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Base64;

import javax.annotation.Nullable;

import io.github.mzattera.predictivepowers.util.FileUtil;
import io.github.mzattera.predictivepowers.util.ImageUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

/**
 * This is a {@link FilePart} that contains the base64 encoding of a file.
 * Notice both {@link #getFile()} and {@link #getUrl()} will return null, whilst
 * {@link #isLocalFile()} returns true, as the file content is stored in this
 * object.
 * 
 * This class is immutable.
 * 
 * @author Massimiliano "Maxi" Zattera.
 */
@Getter
@ToString
public final class Base64FilePart extends FilePart {

	/** Content encoded as base64, */
	@Getter
	private final @NonNull String encodedContent;

	@Getter
	private final String name;

	@Override
	public Type getType() {
		return Type.BASE64_FILE;
	}

	/**
	 * Constructor from file; notice the file will be inspected to determine MIME
	 * type.
	 */
	public Base64FilePart(@NonNull File file) throws IOException {
		this(file, null);
	}

	/**
	 * Constructor from file.
	 */
	public Base64FilePart(@NonNull File file, @Nullable String mimeType) throws IOException {
		super(file, mimeType);
		this.name = super.getName();
		try (InputStream s = super.getInputStream()) {
			this.encodedContent = Base64.getEncoder().encodeToString(s.readAllBytes());
		}
	}

	/**
	 * Constructor from URL; notice the URL will be inspected to determine MIME
	 * type.
	 */
	public Base64FilePart(@NonNull URL url) throws IOException {
		this(url, null);
	}

	/**
	 * Constructor from URL.
	 */
	public Base64FilePart(@NonNull URL url, @Nullable String mimeType) throws IOException {
		super(url, mimeType);
		this.name = super.getName();
		try (InputStream s = super.getInputStream()) {
			this.encodedContent = Base64.getEncoder().encodeToString(s.readAllBytes());
		}
	}

	/**
	 * Constructor from image; a name for this file must be provided. The image is
	 * encoded as base64 PNG.
	 */
	public Base64FilePart(@NonNull BufferedImage image, @Nullable String name) throws IOException {
		super("image/png");
		this.name = name;
		this.encodedContent = Base64.getEncoder().encodeToString(ImageUtil.toBytes(image, "png"));
	}

	/**
	 * Constructor from encoded content; a name for this file must be provided.
	 * Notice the content is inspected to determine MIME type.
	 */
	public Base64FilePart(@NonNull String encodedContent, @Nullable String name) {
		this(encodedContent, name, FileUtil.getMimeType(Base64.getDecoder().decode(encodedContent)));
	}

	/**
	 * Constructor from encoded content; a name for this file must be provided.
	 */
	public Base64FilePart(@NonNull String encodedContent, @Nullable String name, @Nullable String mimeType) {
		super(mimeType);
		this.name = name;
		this.encodedContent = encodedContent;
	}

	/**
	 * Constructor from stream content; a name for this file must be provided.
	 * Notice the content is inspected to determine MIME type.
	 */
	public Base64FilePart(@NonNull InputStream in, @Nullable String name) throws IOException {
		this(in.readAllBytes(), name);
	}

	/**
	 * Constructor from stream content; a name for this file must be provided
	 */
	public Base64FilePart(@NonNull InputStream in, @Nullable String name, @Nullable String mimeType)
			throws IOException {
		this(in.readAllBytes(), name, mimeType);
	}

	/**
	 * Constructor from byte content; a name for this file must be provided. Notice
	 * the content is inspected to determine MIME type.
	 */
	public Base64FilePart(@NonNull byte[] bytes, @Nullable String name) {
		this(bytes, name, FileUtil.getMimeType(bytes));
	}

	/**
	 * Constructor from byte content; a name for this file must be provided.
	 */
	public Base64FilePart(byte[] bytes, String name, String mimeType) {
		super(mimeType);
		this.name = name;
		this.encodedContent = Base64.getEncoder().encodeToString(bytes);
	}

	/**
	 * "Copy" constructor.
	 */
	public Base64FilePart(@NonNull FilePart file) throws IOException {
		super(file.getMimeType());
		this.name = file.getName();
		try (InputStream s = file.getInputStream()) {
			this.encodedContent = Base64.getEncoder().encodeToString(s.readAllBytes());
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
