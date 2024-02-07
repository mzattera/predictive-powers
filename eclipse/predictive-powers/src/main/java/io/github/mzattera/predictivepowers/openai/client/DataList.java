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

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Sometimes OpenAI API returns list of data in this format.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@ToString
public class DataList<T> {

	@Getter
	@Setter
	private String object;
	
	@Getter
	@Setter
	private List<T> data;

	@Setter
	private Boolean hasMore;

	public boolean hasMore() {
		if (hasMore == null)
			return false;
		return hasMore.booleanValue();
	}

	@Getter
	@Setter
	private String firstId;
	
	@Getter
	@Setter
	private String lastId;
}
