/**
 * 
 */
package io.github.mzattera.predictivepowers.services.messages;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

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
//@RequiredArgsConstructor
@SuperBuilder
@Getter
@Setter
@ToString
public class FilePart implements MessagePart {

	public static enum ContentType {
		GENERIC, IMAGE, AUDIO 
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

	public FilePart(@NonNull File file) {
		this(file, ContentType.GENERIC);
	}

	public FilePart(@NonNull File file, ContentType contentType) {
		if (!file.isFile() || !file.canRead())
			throw new IllegalArgumentException("File must be a readable normal file: " + file.getName());
		this.contentType=contentType;
		this.file = file;
		this.url = null;
	}

	public FilePart(@NonNull URL url) {
		this(url, ContentType.GENERIC);
	}

	public FilePart(@NonNull URL url, ContentType contentType) {
		this.contentType=contentType;
		this.url = url;
		this.file = null;
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
	 * @return True if the file is a web file that is accessible through {@link #getFile()}.
	 */
	public boolean isLocalFile() {
		return (file != null);
	}

	/**
	 * 
	 * @return True if the file is a web file that is accessible through {@link #getUrl()}.
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
		if (isLocalFile())
			return new FileInputStream(file);
		else
			return url.openStream();
	}
	
	@Override
	public String getContent() {
		if (isLocalFile()) {
			try {
				return "[File (local) " + file.getCanonicalPath() + ", Content: "+ contentType + "]";
			} catch (IOException e) {
				return "[File (local): " + file.getName() + ", Content: "+ contentType + "]";
			}
		}
		return "[File (online): " + url.toString() + ", Content: "+ contentType + "]";
	}
}
