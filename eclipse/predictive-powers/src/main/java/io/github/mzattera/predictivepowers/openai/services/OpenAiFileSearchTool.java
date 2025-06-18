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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * This is the File Search tool available to OpenAI assistants. This tool is
 * meant to only live inside {@link OpenAiAssistantTools}.
 * 
 * @author Massimiliano "Maxi" Zattera
 */
public class OpenAiFileSearchTool extends OpenAiAssistantTool {

	// TODO Urgent add search parameters

	public final static String ID = OpenAiAssistantTools.ID + ".file_search";

	// TODO URGENT Not needed ass it is part of FileSearch that we must keep7 to
	// configure parameters
	@Getter
	@Setter
	private Collection<String> vectorStoreIds = new HashSet<>();

	OpenAiFileSearchTool() {
		// TODO Add proper parameters?
		super(ID, "OpenAI File Search Tool.");
	}

	// TODO URGENT add Vector Store as a6 class with convenience methods

	@Override
	public OpenAiFileSearchTool enable() {
		return (OpenAiFileSearchTool) super.enable();
	}

	OpenAiFileSearchTool(@NonNull List<String> vectorStoreIds) {
		this();
		addAllVectorStores(vectorStoreIds);
	}

	public void addVectorStore(@NonNull String vectorStoreId) {
		vectorStoreIds.add(vectorStoreId);
	}

	public void addAllVectorStores(@NonNull List<String> vectorStoreIds) {
		this.vectorStoreIds.addAll(vectorStoreIds);
	}

	public void removeVectorStore(@NonNull String vectorStoreId) {
		vectorStoreIds.remove(vectorStoreId);
	}

	public void removeAllVectorStores(@NonNull List<String> vectorStoreIds) {
		this.vectorStoreIds.removeAll(vectorStoreIds);
	}
}
