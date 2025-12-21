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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.tika.mime.MimeTypes;

import io.github.mzattera.predictivepowers.util.FileUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

/**
 * This is a {@link MessagePart} that contains reference to a file.
 * 
 * The file can be a local file, accessible via a {@link File}, or a file on the
 * web accessible through a {@link URL} or a file stored in some file service.
 * 
 * @author Massimiliano "Maxi" Zattera.
 */
@SuperBuilder
@Getter
public class FilePart implements MessagePart {

	public static enum ContentType {
		AUDIO, IMAGE, TEXT, VIDEO, GENERIC;

		public static ContentType fromMimeType(@NonNull String mimeType) {
			switch (mimeType.split("/")[0].toLowerCase()) {
			case "audio":
				return AUDIO;
			case "image":
				return IMAGE;
			case "text":
				return TEXT;
			case "video":
				return VIDEO;
			default:
				return GENERIC;
			}
		}
	}

	/**
	 * MIME type for the file contents.
	 */
	@NonNull
	private String mimeType;

	protected void setMimeType(String mimeType) {
		this.mimeType = (mimeType == null ? MimeTypes.OCTET_STREAM : mimeType);
		this.contentType = ContentType.fromMimeType(mimeType);
	}

	/**
	 * Enumeration containing the generic MIME type of the file.
	 */
	@NonNull
	private ContentType contentType;

	/**
	 * If this is a local file, this points to the file itself.
	 */
	private final File file;

	/**
	 * If this is a remote file, this is its URL.
	 */
	private final URL url;
	
	@Override
	public Type getType() {
		return Type.FILE;
	}
	
	protected FilePart() {
		this.file = null;
		this.url = null;
	}

	/**
	 * Constructor. Notice the file is inspected to determine its content type.
	 */
	public FilePart(@NonNull File file) {
		this(file, null);
	}

	/**
	 * Constructor. Notice the file is inspected if mimeType==null to determine its
	 * content type.
	 */
	public FilePart(@NonNull File file, String mimeType) {
		this.file = file;
		this.url = null;
		if (!file.isFile() || !file.canRead())
			throw new IllegalArgumentException("File must be a readable normal file: " + file.getName());
		this.mimeType = (mimeType == null ? FileUtil.getMimeType(file) : mimeType);
		this.contentType = ContentType.fromMimeType(this.mimeType);
	}

	/**
	 * Constructor. Notice the file is inspected to determine its content type.
	 */
	public static FilePart fromFileName(@NonNull String fileName) {
		return new FilePart(new File(fileName), null);
	}

	/**
	 * Constructor. Notice the file is inspected if mimeType==null to determine its
	 * content type.
	 */
	public static FilePart fromFileName(@NonNull String fileName, String mimeType) {
		return new FilePart(new File(fileName), mimeType);
	}

	/**
	 * Constructor. Notice the content at given URL is inspected to determine its
	 * content type.
	 */
	public FilePart(@NonNull URL url) {
		this(url, null);
	}

	/**
	 * Constructor. Notice the content at given URL is inspected to determine its
	 * content type if mimeType==null.
	 */
	public FilePart(@NonNull URL url, @NonNull String mimeType) {
		this.file = null;
		this.url = url;
		this.mimeType = (mimeType == null ? FileUtil.getMimeType(url) : mimeType);
		this.contentType = ContentType.fromMimeType(this.mimeType);
	}

	/**
	 * Constructor. Notice the content at given URL is inspected to determine its
	 * content type.
	 */
	public static FilePart fromUrl(@NonNull String url) throws MalformedURLException, URISyntaxException {
		return new FilePart((new URI(url)).toURL(), null);
	}

	/**
	 * Constructor. Notice the content at given URL is inspected to determine its
	 * content type if mimeType==null.
	 */
	public static FilePart fromUrl(@NonNull String url, String mimeType)
			throws MalformedURLException, URISyntaxException {
		return new FilePart((new URI(url)).toURL(), mimeType);
	}

	/**
	 * @return Display name for the file.
	 */
	public String getName() {
		if (file != null)
			return file.getName();
		if (url != null)
			return url.toString();
		return null;
	}

	/**
	 * 
	 * @return True if the file is a local file, so {@link #getInputStream()} does
	 *         not need a remote connection. Notice that {@link #getFile()} might
	 *         still return null (e.g. for in-memory files like
	 *         {@link Base64FilePart}).
	 */
	public boolean isLocalFile() {
		return (file != null);
	}

	/**
	 * 
	 * @return True if the file is a web file that is accessible through
	 *         {@link #getUrl()}.
	 */
	public boolean isWebFile() {
		return (url != null);
	}

	/**
	 * 
	 * @return An stream to read content of the file.
	 * 
	 * @throws IOException
	 */
	public InputStream getInputStream() throws IOException {
		if (file != null)
			return new FileInputStream(file);
		if (url != null)
			return url.openStream();
		return null;
	}

	/**
	 * 
	 * @return Format of the file (e.g. wav or jpg). The format is inferred from
	 *         file extension, for local files, or from MIME type for other files.
	 */
	public String getFormat() {
		String result = null;
		if (file != null)
			result = FileUtil.getExtension(file).toLowerCase();
		if (result.isBlank())
			return FileUtil.formatFromMimeType(mimeType).toLowerCase();
		return result;
	}

	@Override
	public String getContent() {
		if (file != null) {
			try {
				return "[File: " + file.getCanonicalPath() + ", Content: " + mimeType + "]";
			} catch (IOException e) {
				return "[File: " + file.getName() + ", Content: " + mimeType + "]";
			}
		}
		if (url != null)
			return "[File URL: " + url.toString() + ", Content: " + mimeType + "]";
		return "[File], Content: " + contentType + "]";
	}

	@Override
	public String toString() {
		return getContent();
	}
}
