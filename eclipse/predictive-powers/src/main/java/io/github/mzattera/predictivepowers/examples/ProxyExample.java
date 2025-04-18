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

/**
 * 
 */

package io.github.mzattera.predictivepowers.examples;

import java.net.InetSocketAddress;
import java.net.Proxy;

import io.github.mzattera.predictivepowers.ApiClient;
import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;
import okhttp3.OkHttpClient;

public class ProxyExample {

	public static void main(String[] args) {

		// Reads API key from OS environment
		String key = System.getenv(OpenAiClient.OS_ENV_VAR_NAME);
		String host = "<Your proxy host name goes here>";
		int port = 80; // your proxy port goes here

		Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
		OkHttpClient http = ApiClient.getDefaultHttpClient(OpenAiClient.DEFAULT_TIMEOUT_MILLIS, //
				OpenAiClient.DEFAULT_MAX_RETRIES, //
				OpenAiClient.DEFAULT_KEEP_ALIVE_MILLIS, //
				OpenAiClient.DEFAULT_MAX_IDLE_CONNECTIONS) //
				.newBuilder().proxy(proxy).build();
		OpenAiClient cli = new OpenAiClient(key, http);

		// ... use client here ...

		cli.close();
	}
}
