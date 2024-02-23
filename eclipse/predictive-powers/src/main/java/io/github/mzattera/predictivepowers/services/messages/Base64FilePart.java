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
 * This is a {@link FilePart} that contains the base64 encoding of a file.
 * 
 * @author Massimiliano "Maxi" Zattera.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

	public Base64FilePart(@NonNull File file, ContentType contentType) throws IOException {
		super(file, contentType);
		init(super.getInputStream().readAllBytes(), super.getName());
	}

	public Base64FilePart(@NonNull URL url) throws IOException {
		super(url);
		init(super.getInputStream().readAllBytes(), super.getName());
	}

	public Base64FilePart(@NonNull URL url, ContentType contentType) throws IOException {
		super(url, contentType);
		init(super.getInputStream().readAllBytes(), super.getName());
	}

	public Base64FilePart(InputStream in, String name, ContentType contentType) throws IOException {
		init(in.readAllBytes(), name);
		setContentType(contentType);
	}

	public Base64FilePart(byte[] bytes, String name, ContentType contentType) {
		init(bytes, name);
		setContentType(contentType);
	}

	private void init(byte[] bytes, String name) {
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

	@Override
	public String getContent() {
		return "[File (base64): " + getName() + ", Content: " + getContentType() + "]";
	}
}
