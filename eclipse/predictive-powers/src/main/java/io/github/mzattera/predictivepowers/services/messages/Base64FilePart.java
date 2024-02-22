/**
 * 
 */
package io.github.mzattera.predictivepowers.services.messages;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Base64;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * This is a {@link FilePart} that contains a base64 encoding of a file.
 * 
 * @author Massimiliano "Maxi" Zattera.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Getter
@Setter
@ToString
public class Base64FilePart extends FilePart {

	/** Conent encoded as base64, */
	@Getter
	private String encodedContent;

	@Getter
	private String name;

	public Base64FilePart(@NonNull File file) throws IOException {
		this(file, file.getName(), ContentType.GENERIC);
	}

	public Base64FilePart(@NonNull File file, ContentType contentType) throws IOException {
		this(file, file.getName(), contentType);
	}

	public Base64FilePart(@NonNull File file, @NonNull String name, ContentType contentType) throws IOException {
		super(file, contentType);
		encodedContent = Base64.getEncoder().encodeToString(getInputStream().readAllBytes());
		this.name = name;
	}

	public Base64FilePart(@NonNull URL url) throws IOException {
		this(url, url.toString(), ContentType.GENERIC);
	}

	public Base64FilePart(@NonNull URL url, ContentType contentType) throws IOException {
		this(url, url.toString(), contentType);
	}

	public Base64FilePart(@NonNull URL url, @NonNull String name, ContentType contentType) throws IOException {
		super(url, contentType);
		encodedContent = Base64.getEncoder().encodeToString(getInputStream().readAllBytes());
		this.name = name;
	}

	public Base64FilePart(byte[] bytes, String name, ContentType contentType) {
		setContentType(contentType);
		encodedContent = Base64.getEncoder().encodeToString(bytes);
		this.name = name;
	}

	@Override
	public File getFile() {
		return null;
	}

	@Override
	public URL getUrl() {
		return null;
	}

	@Override
	public boolean isLocalFile() {
		return false;
	}

	@Override
	public boolean isWebFile() {
		return false;
	}

	@Override
	public InputStream getInputStream() {
		return new ByteArrayInputStream(Base64.getDecoder().decode(encodedContent));
	}

	@Override
	public String getContent() {
		return "[File (base64): " + getName() + ", Content: " + getContentType() + "]";
	}
}
