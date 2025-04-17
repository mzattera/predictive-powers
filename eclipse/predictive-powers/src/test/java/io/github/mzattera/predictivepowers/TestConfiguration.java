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

import io.github.mzattera.predictivepowers.huggingface.client.HuggingFaceEndpoint;
import io.github.mzattera.predictivepowers.openai.client.AzureOpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.DirectOpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.Service;

/**
 * This class is used to configure which JUnit tests are acctive.
 */
public final class TestConfiguration {

	public final static boolean TEST_GOOGLE_SERVICES = false;

	public final static boolean TEST_HF_SERVICES = false;

	public final static boolean TEST_KNOWLEDGE_BASE = false;

	public final static boolean TEST_DIRECT_OPENAI_SERVICES = true;

	public final static boolean TEST_AZURE_OPENAI_SERVICES = false;

	public static List<AiEndpoint> getAiEndpoints() {

		List<AiEndpoint> result = new ArrayList<>();

		if (TEST_DIRECT_OPENAI_SERVICES)
			result.add(new DirectOpenAiEndpoint());
		if (TEST_AZURE_OPENAI_SERVICES)
			result.add(new AzureOpenAiEndpoint());
		if (TEST_HF_SERVICES)
			result.add(new HuggingFaceEndpoint());

		return result;
	}

	// This because Azure OpenAI API SUXXXXX
	public static List<Pair<AiEndpoint, String>> getCompletionServices() {

		List<Pair<AiEndpoint, String>> result = new ArrayList<>();

		for (AiEndpoint ep : getAiEndpoints()) {
			if (ep instanceof AzureOpenAiEndpoint)
				result.add(new ImmutablePair<>(ep, ep.getCompletionService("davinci").getModel()));
			else
				result.add(new ImmutablePair<>(ep, ep.getCompletionService().getModel()));
		}

		return result;
	}

	// This because Azure OpenAI API SUXXXXX
	public static List<Pair<AiEndpoint, String>> getChatServices() {

		List<Pair<AiEndpoint, String>> result = new ArrayList<>();

		for (AiEndpoint ep : getAiEndpoints()) {
			if (ep instanceof AzureOpenAiEndpoint)
				result.add(new ImmutablePair<>(ep, ep.getChatService("gpt4").getModel()));
			else
				result.add(new ImmutablePair<>(ep, ep.getChatService().getModel()));
		}

		return result;
	}

	// This because Azure OpenAI API SUXXXXX
	public static List<Pair<AiEndpoint, String>> getEmbeddingServices() {

		List<Pair<AiEndpoint, String>> result = new ArrayList<>();

		for (AiEndpoint ep : getAiEndpoints()) {
			if (ep instanceof AzureOpenAiEndpoint)
				result.add(new ImmutablePair<>(ep, ep.getEmbeddingService("embedding_ada").getModel()));
			else
				result.add(new ImmutablePair<>(ep, ep.getEmbeddingService().getModel()));
		}

		return result;
	}

	// This because Azure OpenAI API SUXXXXX
	public static List<Pair<AiEndpoint, String>> getImageGenerationServices() {

		List<Pair<AiEndpoint, String>> result = new ArrayList<>();

		for (AiEndpoint ep : getAiEndpoints()) {
			if (ep instanceof AzureOpenAiEndpoint)
				result.add(new ImmutablePair<>(ep, ep.getImageGenerationService("dalle").getModel()));
			else
				result.add(new ImmutablePair<>(ep, ep.getImageGenerationService().getModel()));
		}

		return result;
	}

	// This because Azure OpenAI API SUXXXXX
	public static List<Pair<AiEndpoint, String>> getTTSServices() {

		List<Pair<AiEndpoint, String>> result = new ArrayList<>();

		if (TEST_DIRECT_OPENAI_SERVICES)
			result.add(new ImmutablePair<>(new DirectOpenAiEndpoint(), "tts-1"));
		if (TEST_AZURE_OPENAI_SERVICES) {
			AzureOpenAiEndpoint ep = new AzureOpenAiEndpoint();
			ep.getModelService().map("tts", "tts-1"); // Not really needed, but for completeness...
			result.add(new ImmutablePair<>(ep, "tts"));
		}
		return result;
	}

	// This because Azure OpenAI API SUXXXXX
	public static List<Pair<AiEndpoint, String>> getSTTServices() {

		List<Pair<AiEndpoint, String>> result = new ArrayList<>();

		if (TEST_DIRECT_OPENAI_SERVICES)
			result.add(new ImmutablePair<>(new DirectOpenAiEndpoint(), "whisper-1"));
		if (TEST_AZURE_OPENAI_SERVICES) {
			AzureOpenAiEndpoint ep = new AzureOpenAiEndpoint();
			ep.getModelService().map("whispers", "whisper-1"); // Not really needed, but for completeness...
			result.add(new ImmutablePair<>(ep, "whispers"));
		}
		return result;
	}

	// This because Azure OpenAI API SUXXXXX
	public static List<Pair<AiEndpoint, String>> getAgentServices() {

		List<Pair<AiEndpoint, String>> result = new ArrayList<>();

		for (AiEndpoint ep : getAiEndpoints()) {
			if (ep instanceof AzureOpenAiEndpoint)
				result.add(new ImmutablePair<>(ep, ep.getAgentService("gpt4agents").getModel()));
			else try {
				result.add(new ImmutablePair<>(ep, ep.getAgentService().getModel()));
			} catch (UnsupportedOperationException e) {
				// Some endpoints might miss that
			}
		}

		return result;
	}

	public static void close(List<? extends AutoCloseable> resources) {
		for (AutoCloseable r : resources) {
			try {
				if (r instanceof Service)
					try {
						((Service) r).getEndpoint().close();
					} catch (Exception se) {
					}
				r.close();
			} catch (Exception re) {
			}
		}
	}
}
