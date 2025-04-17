/*
 * Copyright 2023-2024 Massimiliano "Maxi" Zattera
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

package io.github.mzattera.predictivepowers.openai.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.mzattera.predictivepowers.AiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiModelMetaData.SupportedApi;
import io.github.mzattera.predictivepowers.services.ModelService;
import lombok.NonNull;

/**
 * This class provides {@link ModelService}s for Azure OpenAI Services.
 * 
 * When calling OpenAi models, the API in Azure requires a OpenAI endpoint and a
 * deployment ID (= a deployed model name). These values are used in building
 * the API URL and they are not passed as request parameters. In our
 * implementation of Azure OpenAI API, we use deployment IDs as model IDs; this
 * allows us to build services which are agnostic about underlying API ("direct"
 * OpenAI or Azure); developers are only required to use deployment IDs as model
 * IDs to invoke deployed models. However, this approach poses a problem when
 * building the model service as it needs to map each deployment ID into
 * corresponding OpenAI base model, to know model metadata. In principle, this
 * can be done through a service API, but it requires Azure authentication,
 * knowing the subscription ID and the region where the Azure OpenAI Service is.
 * This will make the library APIs over complicated and dependent on the
 * endpoint being used.
 * 
 * In order to circumvent this, we leverage the fact that Azure OpenAI Service
 * API returns the ID (name) of the "base" OpenAI model used in each response.
 * Therefore, when a service is created, we issue a "fake" call to the service
 * to read its base model ID. A mapping is then stored between the model ID for
 * the service (that is its deployment ID) and the base model it uses (the
 * underlying OpenAI model); this is handled transparently by OpenAi services
 * (see their register() method)). In this way, it is possible to get OpenAI
 * model metadata for each deployed model in Azure.
 * 
 * The disadvantage of this method (beside the "fake" call) is that this model
 * service will know of a model only after a service using it has been created.
 * 
 * The class is tread-safe and uses a single data repository for all of its
 * instances.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class AzureOpenAiModelService extends OpenAiModelService {

	// TODO URGENT see if you can implement this using the Azure SDK....

	/**
	 * Maps each OpenAI model into its metadata
	 */
	final static Map<String, ModelMetaData> AZURE_MODEL_CONFIG = new HashMap<>(DirectOpenAiModelService.MODEL_CONFIG);
	static {

		// Add all legacy models still used in Azure :-(

		AZURE_MODEL_CONFIG.put("ada", new OpenAiModelMetaData("ada", 2049, SupportedApi.COMPLETIONS));
		AZURE_MODEL_CONFIG.put("babbage", new OpenAiModelMetaData("babbage", 2049, SupportedApi.COMPLETIONS));
		AZURE_MODEL_CONFIG.put("code-search-ada-code-001",
				new OpenAiModelMetaData("code-search-ada-code-001", 2046, SupportedApi.COMPLETIONS));
		AZURE_MODEL_CONFIG.put("code-search-ada-text-001",
				new OpenAiModelMetaData("code-search-ada-text-001", 2046, SupportedApi.COMPLETIONS));
		AZURE_MODEL_CONFIG.put("code-search-babbage-code-001",
				new OpenAiModelMetaData("code-search-babbage-code-001", 2046, SupportedApi.COMPLETIONS));
		AZURE_MODEL_CONFIG.put("code-search-babbage-text-001",
				new OpenAiModelMetaData("code-search-babbage-text-001", 2046, SupportedApi.COMPLETIONS));
		AZURE_MODEL_CONFIG.put("curie", new OpenAiModelMetaData("curie", 2049, SupportedApi.COMPLETIONS));
		AZURE_MODEL_CONFIG.put("davinci", new OpenAiModelMetaData("davinci", 2049, SupportedApi.COMPLETIONS));

		AZURE_MODEL_CONFIG.put("text-ada-001", new OpenAiModelMetaData("text-ada-001", 2049, SupportedApi.COMPLETIONS));
		AZURE_MODEL_CONFIG.put("text-babbage-001",
				new OpenAiModelMetaData("text-babbage-001", 2049, SupportedApi.COMPLETIONS));
		AZURE_MODEL_CONFIG.put("text-curie-001",
				new OpenAiModelMetaData("text-curie-001", 2049, SupportedApi.COMPLETIONS));
		AZURE_MODEL_CONFIG.put("text-davinci-001",
				new OpenAiModelMetaData("text-davinci-001", 2049, SupportedApi.COMPLETIONS));
		AZURE_MODEL_CONFIG.put("text-davinci-002",
				new OpenAiModelMetaData("text-davinci-002", 4093, SupportedApi.COMPLETIONS)); // Documentation says 4097
																								// but it is incorrect
		AZURE_MODEL_CONFIG.put("text-davinci-003",
				new OpenAiModelMetaData("text-davinci-003", 4093, SupportedApi.COMPLETIONS));
		AZURE_MODEL_CONFIG.put("text-embedding-ada-002",
				new OpenAiModelMetaData("text-embedding-ada-002", 8192, SupportedApi.COMPLETIONS));
		AZURE_MODEL_CONFIG.put("text-search-ada-doc-001",
				new OpenAiModelMetaData("text-search-ada-doc-001", 2046, SupportedApi.COMPLETIONS));
		AZURE_MODEL_CONFIG.put("text-search-ada-query-001",
				new OpenAiModelMetaData("text-search-ada-query-001", 2046, SupportedApi.COMPLETIONS));
		AZURE_MODEL_CONFIG.put("text-search-babbage-doc-001",
				new OpenAiModelMetaData("text-search-babbage-doc-001", 2046, SupportedApi.COMPLETIONS));
		AZURE_MODEL_CONFIG.put("text-search-babbage-query-001",
				new OpenAiModelMetaData("text-search-babbage-query-001", 2046, SupportedApi.COMPLETIONS));
		AZURE_MODEL_CONFIG.put("text-search-curie-doc-001",
				new OpenAiModelMetaData("text-search-curie-doc-001", 2046, SupportedApi.COMPLETIONS));
		AZURE_MODEL_CONFIG.put("text-search-curie-query-001",
				new OpenAiModelMetaData("text-search-curie-query-001", 2046, SupportedApi.COMPLETIONS));
		AZURE_MODEL_CONFIG.put("text-search-davinci-doc-001",
				new OpenAiModelMetaData("text-search-davinci-doc-001", 2046, SupportedApi.COMPLETIONS));
		AZURE_MODEL_CONFIG.put("text-search-davinci-query-001",
				new OpenAiModelMetaData("text-search-davinci-query-001", 2046, SupportedApi.COMPLETIONS));
		AZURE_MODEL_CONFIG.put("text-similarity-ada-001",
				new OpenAiModelMetaData("text-similarity-ada-001", 2046, SupportedApi.COMPLETIONS));
		AZURE_MODEL_CONFIG.put("text-similarity-babbage-001",
				new OpenAiModelMetaData("text-similarity-babbage-001", 2046, SupportedApi.COMPLETIONS));
		AZURE_MODEL_CONFIG.put("text-similarity-curie-001",
				new OpenAiModelMetaData("text-similarity-curie-001", 2046, SupportedApi.COMPLETIONS));
		AZURE_MODEL_CONFIG.put("text-similarity-davinci-001",
				new OpenAiModelMetaData("text-similarity-davinci-001", 2046, SupportedApi.COMPLETIONS));
	}

	/**
	 * Every time a new OpenAI service is created, it will call this to map the
	 * service deployment ID into the underlying OpenAI model used by the service.
	 */
	public void map(@NonNull String deploymentId, @NonNull String model) {
		ModelMetaData data = AZURE_MODEL_CONFIG.get(model);
		if (data != null)
			super.put(deploymentId, data);
	}

	public AzureOpenAiModelService() {
		super(new ConcurrentHashMap<>());
	}

	/**
	 * Notice this returns only the list of model deployments known for this
	 * endpoint, that is, those used in services being created so far.
	 */
	@Override
	public List<String> listModels() {
		return new ArrayList<>(data.keySet());
	}

	/**
	 * Always returns null since model services in Azure are indeed singletons, so
	 * they are not connected to a single instance of an endpoint.
	 */
	@Override
	public AiEndpoint getEndpoint() {
		return null;
	}
}
