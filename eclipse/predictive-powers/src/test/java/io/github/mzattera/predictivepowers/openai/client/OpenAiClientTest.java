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
package io.github.mzattera.predictivepowers.openai.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.ApiClient;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsResponse;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class OpenAiClientTest {

	@Test
	public void test01() {

		try (OpenAiClient cli = new OpenAiClient()) {

			// Complete a sentence.
			CompletionsRequest req = CompletionsRequest.builder().model("text-davinci-003").maxTokens(50)
					.prompt("Alan Turing was").build();
			CompletionsResponse resp = cli.createCompletion(req);
			assertEquals(1, resp.getChoices().size());
		}
	}

	@Test
	public void test02() {

		OkHttpClient http = ApiClient
				.getDefaultHttpClient(System.getenv(OpenAiClient.OS_ENV_VAR_NAME), OpenAiClient.DEFAULT_TIMEOUT_MILLIS,
						OpenAiClient.DEFAULT_KEEP_ALIVE_MILLIS, OpenAiClient.DEFAULT_MAX_IDLE_CONNECTIONS)
				.newBuilder().addInterceptor(new Interceptor() {

					@Override
					public Response intercept(Chain chain) throws IOException {
//				System.out.println(chain.request().toString());
						return chain.proceed(chain.request());
					}
				}).build();

		try (OpenAiClient cli = new OpenAiClient(http)) {

			// Complete a sentence.
			CompletionsRequest req = CompletionsRequest.builder().model("text-davinci-003").maxTokens(50)
					.prompt("Alan Turing was").build();
			CompletionsResponse resp = cli.createCompletion(req);
			assertEquals(1, resp.getChoices().size());
		}
	}
}
