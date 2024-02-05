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

/**
 * This is a {@link MessagePart} that contains reference to a file.
 * 
 * The file can be a local file, accessible via a {@link File}, or a file on the
 * web accessible through a {@link URL} or a file stored in some file service.
 * 
 * @author Massimiliano "Maxi" Zattera.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// @RequiredArgsConstructor
@Getter
@Setter
@ToString
public class FilePart implements MessagePart {

	/**
	 * If this is a local file, this points to the file itself.
	 */
	private File file;

	/**
	 * If this is a remote file, this is its URL.
	 */
	private URL url;

	@Override
	public String getContent() {
		if (isLocalFile()) {
			try {
				return "[File (local): " + file.getCanonicalPath() + "]";
			} catch (Exception e) {
				return "[File (remote): " + file.getName() + "]";
			}
		}
		return "[File: " + url.toString() + "]";
	}

	/**
	 * 
	 * @return True if the file is a local file that is accessible as a
	 *         {@link File}.
	 */
	public boolean isLocalFile() {
		return (file != null);
	}

	/**
	 * Deletes the file.
	 * 
	 * @return True if the file was deleted successfully.
	 * 
	 * @throws IOException
	 */
	public boolean deleteFile() throws IOException {
		if (isLocalFile()) {
			try {
				return file.delete();
			} catch (Exception e) {
				throw new IOException(e);
			}
		} else
			return false;
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

	public FilePart(@NonNull File file) {
		if (!file.isFile() || !file.canRead())
			throw new IllegalArgumentException("File must be a readable normal file: " + file.getName());
		this.file = file;
		this.url = null;
	}

	public FilePart(@NonNull URL url) {
		this.url = url;
		this.file = null;
	}
}
