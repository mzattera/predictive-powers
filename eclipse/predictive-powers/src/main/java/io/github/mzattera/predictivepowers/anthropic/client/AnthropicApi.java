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

package io.github.mzattera.predictivepowers.anthropic.client;

import io.github.mzattera.predictivepowers.anthropic.client.messages.MessagesRequest;
import io.github.mzattera.predictivepowers.anthropic.client.messages.MessagesResponse;
import io.reactivex.Single;
import lombok.NonNull;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Retrofit definition for Anthrop/c API.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public interface AnthropicApi {

	@POST("messages")
	Single<MessagesResponse> messages(@Body @NonNull MessagesRequest req);
}
