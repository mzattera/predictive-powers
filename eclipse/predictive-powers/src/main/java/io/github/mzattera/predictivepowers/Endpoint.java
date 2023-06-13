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

package io.github.mzattera.predictivepowers;

import java.io.Closeable;

import io.github.mzattera.predictivepowers.services.ChatService;
import io.github.mzattera.predictivepowers.services.CompletionService;
import io.github.mzattera.predictivepowers.services.EmbeddingService;
import io.github.mzattera.predictivepowers.services.ImageGenerationService;
import io.github.mzattera.predictivepowers.services.QuestionAnsweringService;
import io.github.mzattera.predictivepowers.services.QuestionExtractionService;

/**
 * This interface represents an endpoint providing GenAI capabilities in form of
 * services.
 * 
 * At the moment it has methods to provide all available services, in the future
 * this interface might be broken down into smaller pieces.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public interface Endpoint extends Closeable {

	// TODO always ensure it return service interfaces

	ApiClient getClient();

	CompletionService getCompletionService();

	EmbeddingService getEmbeddingService();

	ChatService getChatService();

	ChatService getChatService(String personality);

	QuestionExtractionService getQuestionExtractionService();

	QuestionAnsweringService getQuestionAnsweringService();

	ImageGenerationService getImageGenerationService();
}