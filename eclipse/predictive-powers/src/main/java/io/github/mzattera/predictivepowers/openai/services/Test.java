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

package io.github.mzattera.predictivepowers.openai.services;

import com.openai.client.OpenAIClient;

import io.github.mzattera.predictivepowers.openai.client.OpenAiEndpoint;

// TODO URGENT Delete
public class Test {

	public static void main(String[] args) {
		try (OpenAiEndpoint ep = new OpenAiEndpoint();
				OpenAiAgentService svc = ep.getAgentService();
				OpenAiAssistant agent = svc.getAgent();
				) {
			
		}
	}

}
