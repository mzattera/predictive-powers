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
package io.github.mzattera.predictivepowers.openai.services;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.openai.core.MultipartField;
import com.openai.core.http.HttpResponse;
import com.openai.models.chat.completions.ChatCompletionContentPart.File;
import com.openai.models.files.FileContentParams;
import com.openai.models.files.FileCreateParams;
import com.openai.models.files.FileObject;
import com.openai.models.files.FilePurpose;

import io.github.mzattera.predictivepowers.services.messages.FilePart;
import io.github.mzattera.predictivepowers.util.FileUtil;
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
			return openAiFile.file().filename().isEmpty() ? fileId : openAiFile.file().filename().get();
		return fileId;
	}

	/**
	 * 
	 * @return An stream to read content of the file.
	 * 
	 * @throws IOException If a valid endpoint was not provided in constructor.
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		try {
			if (endpoint == null)
				throw new IOException("To use this method, a valid endpoint must be provided in constructor.");

			java.io.File tmp = java.io.File.createTempFile("OpenAI_file_", ".dnload");
			FileContentParams params = FileContentParams.builder().fileId(fileId).build();
			try (HttpResponse response = endpoint.getClient().files().content(params)) {
				Files.copy(response.body(), tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (Exception e) {
				System.out.println("Something went wrong!");
				throw new RuntimeException(e);
			}

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
		this.fileId = openAiFile.file().fileId().get();
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
		this.fileId = fileId;
		this.openAiFile = null;
		this.endpoint = endpoint;
		setMimeType(mimeType);
	}

	/**
	 * Uploads given file and returns corresponding part.
	 * 
	 * @param endpoint
	 * @param purpose
	 * @param inFile
	 * @return
	 */
	public static OpenAiFilePart create(@NonNull OpenAiEndpoint endpoint, java.io.File inFile, FilePurpose purpose) {
		try {
			return create(endpoint, inFile.getName(), new FileInputStream(inFile), purpose);
		} catch (FileNotFoundException e) {
			return null;
		}
	}

	/**
	 * Uploads given content as a file and returns corresponding part.
	 * 
	 * @param endpoint
	 * @param purpose
	 * @return
	 */
	public static OpenAiFilePart create(@NonNull OpenAiEndpoint endpoint, String fileName, byte[] bytes,
			FilePurpose purpose) {
		return create(endpoint, fileName, new ByteArrayInputStream(bytes), purpose);
	}

	/**
	 * /** Uploads given content as a file and returns corresponding part.
	 * 
	 * @param endpoint
	 * @param purpose
	 */
	public static OpenAiFilePart create(@NonNull OpenAiEndpoint endpoint, String fileName, InputStream in,
			FilePurpose purpose) {

		// File upload does not work without specifying a name; maybe it is used to
		// determine the file type...
		MultipartField<InputStream> f = MultipartField.<InputStream>builder() //
				.value(in) //
				.filename(fileName) //
				.build();
		
		FileCreateParams params = FileCreateParams.builder().file(f).purpose(purpose).build();
		FileObject fileObject = endpoint.getClient().files().create(params);
		return new OpenAiFilePart(fileObject.id(), FileUtil.getMimeType(in), endpoint);
	}
}
