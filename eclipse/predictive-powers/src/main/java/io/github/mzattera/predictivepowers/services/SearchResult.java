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

package io.github.mzattera.predictivepowers.services;

import java.io.Serializable;
import java.net.URL;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * A single result from an online search.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
public class SearchResult implements Serializable {

	private static final long serialVersionUID = -7975999709641989742L;

	String title;

	@NonNull
	URL link;

	String mime;
	String fileFormat;

	@Override
	public String toString() {
		return (title == null ? "" : title + " ") + "[" + link + "]";
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof SearchResult)
			return this.link.equals(((SearchResult) other).link);
		else
			return false;
	}

	@Override
	public int hashCode() {
		return link.hashCode();
	}
}
