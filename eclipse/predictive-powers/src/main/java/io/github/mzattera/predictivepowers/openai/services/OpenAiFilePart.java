/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.services;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import io.github.mzattera.predictivepowers.openai.client.files.File;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.messages.FilePart;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * A {@link FilePart} that encapsulates a OpenAI {@link File} id.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Getter
@Setter
@ToString
public class OpenAiFilePart extends FilePart {
	
	private ContentType contentType;

	@NonNull
	private String fileId;

	private OpenAiEndpoint endpoint;

	public OpenAiFilePart(ContentType contentType, @NonNull File file) {
		this(contentType, file.getId(), null);
	}

	public OpenAiFilePart(@NonNull File file, OpenAiEndpoint endpoint) {
		this(ContentType.GENERIC, file.getId(), endpoint);
	}

	public OpenAiFilePart(ContentType contentType, @NonNull File file, OpenAiEndpoint endpoint) {
		this(contentType, file.getId(), endpoint);
	}

	@Override
	public String getContent() {
		return "[OpenAI File: " + fileId + ", Content: "+ contentType + "]";
	}

	@Override
	public boolean isLocalFile() {
		return false;
	}

	@Override
	public boolean isWebFile() {
		return false;
	}

	/**
	 * 
	 * @return An stream to read content of the file.
	 * 
	 * @throws IOException If a valid endpoint was not provided in constructor..
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		try {
			if (endpoint == null)
				throw new IOException("To use this method, a valind endpoint must be provided in constructor.");
			java.io.File tmp = java.io.File.createTempFile("OpenAI_file_", ".dnload");
			endpoint.getClient().retrieveFileContent(fileId, tmp);
			return new FileInputStream(tmp);
		} catch (IOException e) {
			throw e;
		} catch (Exception other) {
			throw new IOException(other);
		}
	}
}
