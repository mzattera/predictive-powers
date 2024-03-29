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

import io.github.mzattera.util.FileUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * This is a {@link MessagePart} that contains reference to a file.
 * 
 * The file can be a local file, accessible via a {@link File}, or a file on the
 * web accessible through a {@link URL} or a file stored in some file service.
 * 
 * @author Massimiliano "Maxi" Zattera.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Getter
@Setter
@ToString
public class FilePart implements MessagePart {

	public static enum ContentType {
		AUDIO, IMAGE, TEXT, VIDEO, GENERIC;

		public static ContentType fromMimeType(String mimeType) {
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

	private ContentType contentType;

	/**
	 * If this is a local file, this points to the file itself.
	 */
	private File file;

	/**
	 * If this is a remote file, this is its URL.
	 */
	private URL url;

	/**
	 * Constructor. Notice the file is inspected to determine its content type.
	 * GENERIC content type is assumed if the file cannot be read.
	 * 
	 * @param file
	 */
	public FilePart(@NonNull File file) {
		this(file, ContentType.fromMimeType(FileUtil.getMimeType(file)));
	}

	public FilePart(@NonNull File file, ContentType contentType) {
		if (!file.isFile() || !file.canRead())
			throw new IllegalArgumentException("File must be a readable normal file: " + file.getName());
		this.contentType = contentType;
		this.file = file;
		this.url = null;
	}

	/**
	 * Constructor. Notice the file is inspected to determine its content type.
	 * GENERIC content type is assumed if the file cannot be read.
	 */
	public static FilePart fromFileName(@NonNull String fileName) {
		return new FilePart(new File(fileName));
	}

	public static FilePart fromFileName(@NonNull String fileName, ContentType contentType) {
		return new FilePart(new File(fileName), contentType);
	}

	/**
	 * Constructor. Notice the content at given URL is inspected to determine its
	 * content type. GENERIC content type is assumed if the content cannot be read.
	 */
	public FilePart(@NonNull URL url) {
		this(url, ContentType.fromMimeType(FileUtil.getMimeType(url)));
	}

	public FilePart(@NonNull URL url, ContentType contentType) {
		this.contentType = contentType;
		this.url = url;
		this.file = null;
	}

	/**
	 * Constructor. Notice the content at given URL is inspected to determine its
	 * content type. GENERIC content type is assumed if the content cannot be read.
	 */
	public static FilePart fromUrl(@NonNull String url) throws MalformedURLException, URISyntaxException {
		return new FilePart((new URI(url)).toURL());
	}

	public static FilePart fromUrl(@NonNull String url, ContentType contentType)
			throws MalformedURLException, URISyntaxException {
		return new FilePart((new URI(url)).toURL(), contentType);
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

	@Override
	public String getContent() {
		if (file != null) {
			try {
				return "[File: " + file.getCanonicalPath() + ", Content: " + contentType + "]";
			} catch (IOException e) {
				return "[File: " + file.getName() + ", Content: " + contentType + "]";
			}
		}
		if (url != null)
			return "[File URL: " + url.toString() + ", Content: " + contentType + "]";
		return "[File], Content: " + contentType + "]";
	}
}
