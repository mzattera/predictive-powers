/*
 * Copyright 2023 Massimiliano "Maxi" Zattera
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

import java.util.List;
import java.util.stream.Collectors;

import com.openai.models.vectorstores.VectorStore;
import com.openai.models.vectorstores.VectorStoreCreateParams;
import com.openai.models.vectorstores.files.FileCreateParams;

import lombok.NonNull;

/**
 * Provides methods to handle vector stores.
 */
public final class OpenAiVectorStore {

	private OpenAiVectorStore() {
	}

	/**
	 * 
	 * @param endpoint
	 * @param id
	 * @return The {@link VectorStore} with given ID.
	 */
	public static VectorStore get(@NonNull OpenAiEndpoint endpoint, @NonNull String id) {
		return endpoint.getClient().vectorStores().retrieve(id);
	}

	/**
	 * Creates a new {@link VectorStore} and returns it.
	 */
	public static VectorStore create(@NonNull OpenAiEndpoint endpoint, String name) {
		VectorStoreCreateParams.Builder b = VectorStoreCreateParams.builder();
		if (name != null)
			b.name(name);
		return create(endpoint, b.build());
	}

	/**
	 * Creates a new {@link VectorStore} and returns it. It wait until all files
	 * have been processed before returning.
	 */
	public static VectorStore create(@NonNull OpenAiEndpoint endpoint, String name,
			List<? extends OpenAiFilePart> files) {
		VectorStore vs = create(endpoint, name);
		if (files != null) { // Upload files
			for (OpenAiFilePart file : files)
				endpoint.getClient().vectorStores().files().create(FileCreateParams.builder() //
						.fileId(file.getFileId()) //
						.vectorStoreId(vs.id()).build() //
				);
		}

		// Waits for all files to be processed
		while (endpoint.getClient().vectorStores().retrieve(vs.id()).fileCounts().inProgress() > 0) {
			try {
				Thread.sleep(1 * 1000);
			} catch (InterruptedException e) {
			}
		}

		return vs;
	}

	/**
	 * Creates a new vector store and returns it.
	 * 
	 * @param endpoint
	 * @return
	 */
	public static VectorStore create(@NonNull OpenAiEndpoint endpoint, VectorStoreCreateParams params) {
		return endpoint.getClient().vectorStores().create(params);
	}

	/**
	 * 
	 * @return Available Vector Stores IDs.
	 */
	public static List<String> list(@NonNull OpenAiEndpoint endpoint) {
		return endpoint.getClient().vectorStores().list().autoPager().stream().map(VectorStore::id)
				.collect(Collectors.toList());
	}
}
