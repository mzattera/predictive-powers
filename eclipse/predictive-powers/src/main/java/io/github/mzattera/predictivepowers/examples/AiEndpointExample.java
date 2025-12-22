/*
 * Copyright 2023-2025 Massimiliano "Maxi" Zattera
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

package io.github.mzattera.predictivepowers.examples;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

import io.github.mzattera.predictivepowers.AiEndpoint;
import io.github.mzattera.predictivepowers.huggingface.services.HuggingFaceEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiEndpoint;

public class AiEndpointExample {

	@SuppressWarnings({ "resource", "unused" })
	public static void main(String[] args) {

		AiEndpoint endpoint;

		// Creates a HuggingFaceEndpoint
		// Get API key from OS environment variable "HUGGING_FACE_API_KEY"
		endpoint = new HuggingFaceEndpoint();

		// Creates a OpenAiEndpoint

		// Get API key from OS environment variable "OPENAI_API_KEY"
		endpoint = new OpenAiEndpoint();

		// Pass API key explicitly (NOT the best practice)
		endpoint = new OpenAiEndpoint("sk-H0a...Yo1");

		// Build endpoint from an existing API client
		// The client is created reading API key from OS environment
		OpenAIClient cli = OpenAIOkHttpClient.fromEnv();
		// Client can be configured here...
		endpoint = new OpenAiEndpoint(cli);
	}
}
