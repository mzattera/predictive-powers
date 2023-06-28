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

package io.github.mzattera.predictivepowers.google.client;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Results from Google search API
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@Getter
@Setter
@ToString
public class Search {

	// TODO check all parameters are correctly provided

	@Getter
	@Setter
	@ToString
	public class Url {
		String type;
		String template;
	}

	@Getter
	@Setter
	@ToString
	public class Queries {
		List<Query> previousPage;
		List<Query> request;
		List<Query> nextPage;
	}

	@Getter
	@Setter
	@ToString
	public class Promotion {

		@Getter
		@Setter
		@ToString
		public class BodyLine {
			String title;
			String htmlTitle;
			String url;
			String link;
		}

		@Getter
		@Setter
		@ToString
		public class Image {
			String source;
			int width;
			int height;
		}

		String title;
		String htmlTitle;
		String link;
		String displayLink;
		List<BodyLine> bodyLines;
		Image image;
	}

	@Getter
	@Setter
	@ToString
	public class SearchInformation {
		double searchTime;
		String formattedSearchTime;
		String totalResults;
		String formattedTotalResults;
	}

	@Getter
	@Setter
	@ToString
	public class Spelling {
		String correctedQuery;
		String htmlCorrectedQuery;
	}

	String kind;
	Url url;
	Queries queries;
	List<Promotion> promotions;

	// TODO should add support for this?
	Object context;

	SearchInformation searchInformation;
	Spelling spelling;
	List<Result> items;
}
