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

/**
 * 
 */
package io.github.mzattera.predictivepowers.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import lombok.NonNull;

/**
 * Utilities to access things on the web.
 */
public final class WebUtil {

	private WebUtil() {
	}

	/**
	 * Get a stream connected to given URL this because URL.openStream() sometimes
	 * cannot read files because agents without an agent ID are blocked.
	 * 
	 * @throws IOException
	 */
	public static InputStream getInputStream(@NonNull URL url) throws IOException {
		return getInputStream(url, -1);
	}

	/**
	 * Get a stream connected to given URL this because URL.openStream() sometimes
	 * cannot read files because agents without an agent ID are blocked.
	 * 
	 * @param timeoutMillis Timeout (millisecond) to download content from given
	 *                      url. If this is less than or equal to 0 it will be ignored.
	 * @throws IOException
	 */
	public static InputStream getInputStream(@NonNull URL url, int timeoutMillis) throws IOException {
		// This doesn't work in many cases
		// return url.openStream();

		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestProperty("User-Agent",
				"proactive-powers Java Library for Agents (https://github.com/mzattera/predictive-powers)");
		if (timeoutMillis > 0) {
			connection.setConnectTimeout(timeoutMillis);
			connection.setReadTimeout(timeoutMillis);
		}
		return connection.getInputStream();
	}
}