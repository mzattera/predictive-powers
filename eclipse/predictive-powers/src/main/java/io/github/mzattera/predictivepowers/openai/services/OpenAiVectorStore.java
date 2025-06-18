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

import io.github.mzattera.predictivepowers.openai.client.OpenAiEndpoint;
import lombok.Getter;
import lombok.NonNull;

/**
 * This wraps a OpenAI Vector Store providing some convenience methods.
 */
public class OpenAiVectorStore {

	@Getter
	private final String id;

	private final OpenAiEndpoint endpoint;

	public OpenAiVectorStore(@NonNull OpenAiEndpoint endpoint, @NonNull String id) {
		this.id = id;
		this.endpoint = endpoint;
	}

	/**
	 * Creates a new vector store and returns it.
	 * 
	 * @param endpoint
	 * @return
	 */
	public static OpenAiVectorStore create(@NonNull OpenAiEndpoint endpoint, String name) {
		VectorStoreCreateParams.Builder b = VectorStoreCreateParams.builder();
		if (name != null)
			b.name(name);
		return create(endpoint, b.build());
	}

	/**
	 * Creates a new vector store and returns it.
	 * 
	 * @param endpoint
	 * @return
	 */
	public static OpenAiVectorStore create(@NonNull OpenAiEndpoint endpoint, String name,
			List<? extends OpenAiFilePart> files) {
		OpenAiVectorStore vs = create(endpoint, name);
		if (files != null) { // Upload files
			for (OpenAiFilePart file : files)
				endpoint.getClient().vectorStores().files().create(FileCreateParams.builder() //
						.fileId(file.getFileId()) //
						.vectorStoreId(vs.getId()).build() //
				);
		}
		return vs;
	}

	/**
	 * Creates a new vector store and returns it.
	 * 
	 * @param endpoint
	 * @return
	 */
	public static OpenAiVectorStore create(@NonNull OpenAiEndpoint endpoint, VectorStoreCreateParams params) {
		return new OpenAiVectorStore(endpoint, endpoint.getClient().vectorStores().create(params).id());
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
