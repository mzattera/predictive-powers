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

package io.github.mzattera.util;

import java.io.File;

import lombok.NonNull;

/**
 * Utility class for handling files.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public final class FileUtil {

	private FileUtil() {
	}

	/**
	 * @return Extension for given file (excluding '.').
	 */
	public static String getExtension(File f) {
		return getExtension(f.getName());
	}

	/**
	 * @return Extension for given file name (excluding '.').
	 */
	public static String getExtension(@NonNull String fileName) {
		int pos = fileName.lastIndexOf('.');
		if ((pos == -1) || (pos == (fileName.length() - 1)))
			return "";
		return fileName.substring(pos + 1);
	}
}
