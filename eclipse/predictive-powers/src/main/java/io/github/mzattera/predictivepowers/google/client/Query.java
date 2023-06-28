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

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * A query, as defined in Google search API
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@Getter
@Setter
@ToString
public class Query {
	String title;
	String totalResults;
	String searchTerms;
	int count;
	int startIndex;
	int startPage;
	String language;
	String inputEncoding;
	String outputEncoding;
	String safe;
	String cx;
	String sort;
	String filter;
	String gl;
	String cr;
	String googleHost;
	String disableCnTwTranslation;
	String hq;
	String hl;
	String siteSearch;
	String siteSearchFilter;
	String exactTerms;
	String excludeTerms;
	String linkSite;
	String orTerms;
	String relatedSite;
	String dateRestrict;
	String lowRange;
	String highRange;
	String fileType;
	String rights;
	String searchType;
	String imgSize;
	String imgType;
	String imgColorType;
	String imgDominantColor;
}