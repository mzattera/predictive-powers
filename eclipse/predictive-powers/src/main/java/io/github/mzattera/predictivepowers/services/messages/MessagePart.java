/*
 * Copyright 2024 Massimiliano "Maxi" Zattera
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

/**
 * 
 */
package io.github.mzattera.predictivepowers.services.messages;

/**
 * Marker interface for classes that can be parts of a {@link ChatMessage}.
 * 
 * 
 * @author Massimiliano "Maxi" Zattera.
 */
public interface MessagePart {

	/**
	 * 
	 * @return A string representation of the content of this message part. Notice
	 *         not all parts are easily representable as text (e.g. a file).
	 */
	String getContent();
}
