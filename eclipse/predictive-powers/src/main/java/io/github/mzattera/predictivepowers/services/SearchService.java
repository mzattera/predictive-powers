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

package io.github.mzattera.predictivepowers.services;

import java.util.List;

import io.github.mzattera.predictivepowers.SearchEndpoint;
import lombok.NonNull;

/**
 * This service provides methods for online search.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public interface SearchService extends Service {

	/**
	 * Endpoint used for this service.
	 */
	@Override
	SearchEndpoint getEndpoint();

	/**
	 * Performs an online search.
	 * 
	 * @param query The search to perform.
	 * @return List of search results.
	 */
	List<Link> search(@NonNull String query);

	/**
	 * Performs an online search.
	 * 
	 * @param query The search to perform.
	 * @param n Number of results to return at most.
	 * @return List of search results.
	 */
	List<Link> search(@NonNull String query, int n);
}
