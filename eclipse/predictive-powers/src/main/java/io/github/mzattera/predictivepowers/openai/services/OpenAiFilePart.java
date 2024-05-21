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
package io.github.mzattera.predictivepowers.openai.services;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import io.github.mzattera.predictivepowers.openai.client.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.files.File;
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

	/**
	 * If this instance was built from a {@link File}, this is a reference to it.
	 */
	private File openAiFile;

	private OpenAiEndpoint endpoint;

	public OpenAiFilePart(ContentType contentType, @NonNull String fileId) {
		this(contentType, fileId, null, null);
	}

	public OpenAiFilePart(@NonNull String fileId, OpenAiEndpoint endpoint) {
		this(ContentType.GENERIC, fileId, null, endpoint);
	}

	public OpenAiFilePart(ContentType contentType, @NonNull String fileId, OpenAiEndpoint endpoint) {
		this(contentType, fileId, null, endpoint);
	}

	public OpenAiFilePart(ContentType contentType, @NonNull File file) {
		this(contentType, file.getId(), file, null);
	}

	public OpenAiFilePart(@NonNull File file, OpenAiEndpoint endpoint) {
		this(ContentType.GENERIC, file.getId(), file, endpoint);
	}

	public OpenAiFilePart(ContentType contentType, @NonNull File file, OpenAiEndpoint endpoint) {
		this(contentType, file.getId(), file, endpoint);
	}

	@Override
	public String getContent() {
		return "[OpenAI File: " + fileId + ", Content: " + contentType + "]";
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
	public String getName() {
		if (openAiFile != null)
			return (openAiFile.getFilename() == null) ? fileId : openAiFile.getFilename();
		return fileId;
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
				throw new IOException("To use this method, a valid endpoint must be provided in constructor.");
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
