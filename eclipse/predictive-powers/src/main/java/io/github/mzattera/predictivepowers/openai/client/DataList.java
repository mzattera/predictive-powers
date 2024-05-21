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

package io.github.mzattera.predictivepowers.openai.client;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Sometimes OpenAI API returns list of data in this format.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
@Getter
@Setter
@ToString
public class DataList<T> {

	private String object;

	private List<T> data;

	@Getter(AccessLevel.PROTECTED)
	@Setter
	private Boolean hasMore;

	public boolean hasMore() {
		if (hasMore == null)
			return false;
		return hasMore.booleanValue();
	}

	private String firstId;

	private String lastId;

	/**
	 * This functional interface supports retrieving all items of a given type when
	 * search returns only a page.
	 */
	@FunctionalInterface
	public interface Searcher<T> {

		/**
		 * Performs a search to get a list of items. These items are a partial result
		 * for the search (a page in paginated search). Items must be sorted in in
		 * ascending order.
		 * 
		 * @param after Only returns items after this one.
		 */
		DataList<T> search(String lastId);
	}

	/**
	 * Fetch all items of a given type, by using a paginated search.
	 * 
	 * @param <T>
	 * @param search A method to use to get next page of search result.
	 * @return
	 */
	public static <T> List<T> getCompleteList(Searcher<T> search) {
		List<T> result = new ArrayList<>();
		String lastId = null;
		DataList<T> data;
		do {
			data = search.search(lastId);
			result.addAll(data.getData());
			if (data.getLastId() == null) // Some APIs return all results at once without pagination
				break;
			lastId = data.getLastId();
		} while (data.hasMore());

		return result;
	}
}
