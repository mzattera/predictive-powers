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

package io.github.mzattera.predictivepowers.openai.client;

import io.github.mzattera.predictivepowers.AiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiAgentService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiCompletionService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiEmbeddingService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiImageGenerationService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiQuestionAnsweringService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiQuestionExtractionService;
import lombok.NonNull;

/**
 * This interface is used for OpenAI endpoints that can be implemented over the
 * OpenAI API or an Azure OpenIA Service resource.
 * 
 * @author Massimiliano "Maxi" Zattera
 */
public interface OpenAiEndpoint extends AiEndpoint {

	@Override
	OpenAiClient getClient();

	@Override
	OpenAiModelService getModelService();

	@Override
	OpenAiCompletionService getCompletionService();

	@Override
	OpenAiCompletionService getCompletionService(@NonNull String model);

	@Override
	OpenAiEmbeddingService getEmbeddingService();

	@Override
	OpenAiEmbeddingService getEmbeddingService(@NonNull String model);

	@Override
	OpenAiChatService getChatService();

	@Override
	OpenAiChatService getChatService(@NonNull String model);

	@Override
	OpenAiAgentService getAgentService();

	@Override
	OpenAiAgentService getAgentService(@NonNull String model);

	@Override
	OpenAiQuestionExtractionService getQuestionExtractionService();

	@Override
	OpenAiQuestionExtractionService getQuestionExtractionService(@NonNull String model);

	@Override
	OpenAiQuestionAnsweringService getQuestionAnsweringService();

	@Override
	OpenAiQuestionAnsweringService getQuestionAnsweringService(@NonNull String model);

	@Override
	OpenAiImageGenerationService getImageGenerationService();

	@Override
	OpenAiImageGenerationService getImageGenerationService(@NonNull String model);

	@Override
	void close();
}