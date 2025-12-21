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
package io.github.mzattera.predictivepowers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import io.github.mzattera.predictivepowers.google.services.GoogleEndpoint;
import io.github.mzattera.predictivepowers.huggingface.services.HuggingFaceEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.SearchService;

/**
 * This class is used to configure which JUnit tests are active.
 */
public final class TestConfiguration {

	public final static boolean TEST_GOOGLE_SERVICES = true;

	public final static boolean TEST_HF_SERVICES = false;

	public final static boolean TEST_OPENAI_SERVICES = true;

	public static final boolean TEST_KNOWLEDGE_BASE = false;

	public static List<AiEndpoint> getAiEndpoints() {
		List<AiEndpoint> result = new ArrayList<>();

		if (TEST_OPENAI_SERVICES)
			result.add(new OpenAiEndpoint());
		if (TEST_HF_SERVICES)
			result.add(new HuggingFaceEndpoint());

		return result;
	}

	public static List<SearchEndpoint> getSearchEndpoints() {
		List<SearchEndpoint> result = new ArrayList<>();

		if (TEST_GOOGLE_SERVICES)
			result.add(new GoogleEndpoint());

		return result;
	}

	public static List<Pair<AiEndpoint, String>> getEmbeddingServices() {

		List<Pair<AiEndpoint, String>> result = new ArrayList<>();

		for (AiEndpoint ep : getAiEndpoints()) {
			try {
				result.add(new ImmutablePair<>(ep, ep.getEmbeddingService().getModel()));
			} catch (UnsupportedOperationException e) {
				// Some endpoints might miss that
			}
		}

		return result;
	}

	public static List<Pair<AiEndpoint, String>> getCompletionServices() {

		List<Pair<AiEndpoint, String>> result = new ArrayList<>();

		for (AiEndpoint ep : getAiEndpoints()) {
			try {
				result.add(new ImmutablePair<>(ep, ep.getCompletionService().getModel()));
			} catch (UnsupportedOperationException e) {
				// Some endpoints might miss that
			}
		}

		return result;
	}

	public static List<Pair<AiEndpoint, String>> getChatServices() {

		List<Pair<AiEndpoint, String>> result = new ArrayList<>();

		for (AiEndpoint ep : getAiEndpoints()) {
			try {
				if (ep instanceof OpenAiEndpoint) {
					// Test the default model
					String defModel = OpenAiChatService.DEFAULT_MODEL;
					result.add(new ImmutablePair<>(ep, defModel));

					// Want to test different types of models too
					if (!"gpt-3.5-turbo".equals(defModel))
						result.add(new ImmutablePair<>(ep, "gpt-3.5-turbo")); // FUNCTION Calls (structured) JSON Mode
																				// output
					if (!"gpt-4-0125-preview".equals(defModel))
						result.add(new ImmutablePair<>(ep, "gpt-4-0125-preview")); // TOOL Calls (structured) JSON Mode
																					// output
					if (!"gpt-4o-mini".equals(defModel))
						result.add(new ImmutablePair<>(ep, "gpt-4o-mini")); // TOOL Calls (structured) with structured
																			// output
					if (!"o3-mini".equals(defModel))
						result.add(new ImmutablePair<>(ep, "o3-mini")); // TOOL Calls (non-strucured) with structured
																		// output
					// FUNCTION Calls (non-structured) do not exist
					// TOOL Calls (non-strucured) with JSON Mode output do not exist
				} else {
					result.add(new ImmutablePair<>(ep, ep.getChatService().getModel()));
				}
			} catch (UnsupportedOperationException e) {
				// Some endpoints might miss that
			}
		}

		return result;
	}

	public static List<Pair<AiEndpoint, String>> getAgentServices() {

		List<Pair<AiEndpoint, String>> result = new ArrayList<>();

		for (AiEndpoint ep : getAiEndpoints()) {
			try {
				result.add(new ImmutablePair<>(ep, ep.getAgentService().getModel()));
			} catch (UnsupportedOperationException e) {
				// Some endpoints might miss that
			}
		}

		return result;
	}

	public static List<Pair<AiEndpoint, String>> getImageGenerationServices() {

		List<Pair<AiEndpoint, String>> result = new ArrayList<>();

		for (AiEndpoint ep : getAiEndpoints()) {
			try {
				if (ep instanceof OpenAiEndpoint) {

					// Want to try all 3 models, as they behave differently

					result.add(new ImmutablePair<>(ep, "dall-e-2"));

					String defModel = ep.getImageGenerationService().getModel();
					if (!"dall-e-3".equals(defModel))
						throw new IllegalArgumentException("You must test dall-e-3 too");
					result.add(new ImmutablePair<>(ep, defModel)); // This to test other constructor
//				result.add(new ImmutablePair<>(ep, "dall-e-3"));

					// TODO: Need to approve organisation
//				result.add(new ImmutablePair<>(ep, "gpt-image-1"));
				} else {
					result.add(new ImmutablePair<>(ep, ep.getImageGenerationService().getModel()));
				}
			} catch (UnsupportedOperationException e) {
				// Some endpoints might miss that
			}
		}

		return result;
	}

	public static List<Pair<AiEndpoint, String>> getTTSServices() {

		// TODO Expose as a proper service

		List<Pair<AiEndpoint, String>> result = new ArrayList<>();

		if (TEST_OPENAI_SERVICES)
			result.add(new ImmutablePair<>(new OpenAiEndpoint(), "tts-1"));
		return result;
	}

	public static List<Pair<AiEndpoint, String>> getSTTServices() {

		// TODO Expose as a proper service

		List<Pair<AiEndpoint, String>> result = new ArrayList<>();

		if (TEST_OPENAI_SERVICES)
			result.add(new ImmutablePair<>(new OpenAiEndpoint(), "whisper-1"));
		return result;
	}

	public static void close(List<Endpoint> resources) {
		for (Endpoint r : resources) {
			try {
				r.close();
			} catch (Exception se) {
			}
		}
	}
}
