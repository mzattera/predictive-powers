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
package io.github.mzattera.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates unique long IDs.
 * IDs are guaranteed to be unique inside the JVM.ight not be unique over subsequent runs.
 */
public final class IdUtil {

	private static final AtomicLong counter = new AtomicLong(System.nanoTime());

	private IdUtil() {
	}

	public static long getLongId() {
		return counter.getAndIncrement();
	}

	public static String getStringId() {
		return Long.toString(getLongId());
	}
}