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

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.github.mzattera.predictivepowers.openai.client.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.files.File;
import io.github.mzattera.predictivepowers.services.messages.FilePart;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

/**
 * A {@link FilePart} that encapsulates a OpenAI {@link File} id.
 */
@Getter
@ToString
public class OpenAiFilePart extends FilePart {

	@NonNull
	private final String fileId;

	/**
	 * If this instance was built from a {@link File}, this is a reference to it.
	 */
	@JsonIgnore
	private final File openAiFile;

	/**
	 * If an end point is provided, then a stream to the file can be obtained.
	 */
	@JsonIgnore
	private final OpenAiEndpoint endpoint;

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

	public OpenAiFilePart(@NonNull File openAiFile) {
		this(openAiFile, null, null);
	}

	public OpenAiFilePart(@NonNull File openAiFile, String mimeType) {
		this(openAiFile, mimeType, null);
	}

	public OpenAiFilePart(@NonNull File openAiFile, String mimeType, OpenAiEndpoint endpoint) {
		super(mimeType);
		this.fileId = openAiFile.getId();
		this.openAiFile = openAiFile;
		this.endpoint = endpoint;
	}

	public OpenAiFilePart(@NonNull String fileId) {
		this(fileId, null, null);
	}

	public OpenAiFilePart(@NonNull String fileId, String mimeType) {
		this(fileId, mimeType, null);
	}

	public OpenAiFilePart(@NonNull String fileId, String mimeType, OpenAiEndpoint endpoint) {
		super(mimeType);
		this.fileId = fileId;
		this.openAiFile = null;
		this.endpoint = endpoint;
	}
}
