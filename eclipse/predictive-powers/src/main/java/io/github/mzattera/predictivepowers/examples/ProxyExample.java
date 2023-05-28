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

package io.github.mzattera.predictivepowers.examples;

import java.net.InetSocketAddress;
import java.net.Proxy;

import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;
import okhttp3.OkHttpClient;

public class ProxyExample {

	public static void main(String[] args) {
		String host = "<Your proxy goes here>";
		int port = 80; // your proxy port goes here

		Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
		OkHttpClient http = OpenAiClient.getDefaultHttpClient(null) // get API key from system environment
				.newBuilder()
				.proxy(proxy)
				.build();
		OpenAiClient cli = new OpenAiClient(http);
		
		//... use client here ...
		
		cli.close();
	}
}
