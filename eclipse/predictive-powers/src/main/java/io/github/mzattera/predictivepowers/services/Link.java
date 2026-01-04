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

package io.github.mzattera.predictivepowers.services;

import java.io.Serializable;
import java.net.URL;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * A link to a web page, including its URL, an optional title and other parameters.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@Getter
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class Link implements Serializable {

	private static final long serialVersionUID = -7975999709641989742L;

	String title;
	String snippet;
	
	@NonNull
	URL url;

	String mime;
	String fileFormat;

	@Override
	public String toString() {
		return (title == null ? "" : title + " ") + "[" + url + "]" + (snippet == null ? "" : " - " + snippet);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Link)
			return this.url.equals(((Link) other).url);
		else
			return false;
	}

	@Override
	public int hashCode() {
		return url.hashCode();
	}
}
