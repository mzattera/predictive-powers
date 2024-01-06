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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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
	 * Returns a file on given folder.
	 * 
	 * @param input  If this is a folder, it is the folder where the returned file
	 *               resides. If it is a file, the returned file will be in same
	 *               folder of this one.
	 * @param string
	 * @return A File that is in same folder as input but has given filename.
	 */
	public static File newFile(@NonNull String folder, @NonNull String fileName) {
		return newFile(new File(folder), fileName);
	}

	/**
	 * Returns a file on given folder.
	 * 
	 * @param input  If this is a folder, it is the folder where the returned file
	 *               resides. If it is a file, the returned file will be in same
	 *               folder of this one.
	 * @param string
	 * @return A File that is in same folder as input but has given filename.
	 */
	public static File newFile(@NonNull File folder, @NonNull String fileName) {
		File path = folder.isDirectory() ? folder
				: (folder.getParentFile() == null ? new File(".") : folder.getParentFile());
		return new File(path, fileName);
	}

	/**
	 * @return Extension for given file (excluding '.').
	 */
	public static String getExtension(@NonNull File f) {
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

	/**
	 * @return Name of given file without its extension.
	 */
	public static String removeExtension(@NonNull File f) {
		return removeExtension(f.getName());
	}

	/**
	 * @return Name of given file without its extension.
	 */
	public static String removeExtension(@NonNull String fileName) {
		int pos = fileName.lastIndexOf('.');
		if (pos == -1)
			return fileName;
		return fileName.substring(0, pos);
	}

	/**
	 * @return Name of given file with new extension.
	 */
	public static String replaceExtension(@NonNull File f, @NonNull String ext) {
		return replaceExtension(f.getName(), ext);
	}

	/**
	 * @return Name of given file without its extension.
	 */
	public static String replaceExtension(@NonNull String fileName, @NonNull String ext) {
		String result = removeExtension(fileName);
		if (ext.startsWith("."))
			return result + ext;
		return result + "." + ext;
	}

	/**
	 * Reads content of a file, assumed to be a UTF-8 string.
	 * 
	 * @param fileName
	 * @return
	 * @throws IOException
	 */
	public static String readFile(@NonNull String fileName) throws IOException {
		return readFile(new File(fileName));
	}

	/**
	 * Reads content of a file, assumed to be a UTF-8 string.
	 * 
	 * @param fileName
	 * @return
	 * @throws IOException
	 */
	public static String readFile(@NonNull File f) throws IOException {
		Path path = f.toPath();
		StringBuilder content = new StringBuilder();
		try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			String line;
			while ((line = br.readLine()) != null) {
				content.append(line).append(System.lineSeparator());
			}
		}
		return content.toString();
	}

	/**
	 * Reads content of a stream, assuming it contains a UTF-8 string.
	 */
	public static String readStream(@NonNull InputStream stream) throws IOException {
		StringBuilder content = new StringBuilder();
		try (InputStreamReader in = new InputStreamReader(stream, StandardCharsets.UTF_8);
				BufferedReader br = new BufferedReader(in)) {
			String line;
			while ((line = br.readLine()) != null) {
				content.append(line).append(System.lineSeparator());
			}
		}
		return content.toString();
	}

	/**
	 * Write text to given file, in UTF-8 encoding.
	 * 
	 * @param fileName
	 * @param text
	 * @throws IOException
	 * 
	 */
	public static void writeFile(@NonNull String fileName, @NonNull String text) throws IOException {
		writeFile(new File(fileName), text);
	}

	/**
	 * Write text to given file, in UTF-8 encoding.
	 * 
	 * @param fileName
	 * @param text
	 * @throws IOException
	 * 
	 */
	public static void writeFile(@NonNull File file, @NonNull String text) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
			writer.write(text);
		}
	}
}
