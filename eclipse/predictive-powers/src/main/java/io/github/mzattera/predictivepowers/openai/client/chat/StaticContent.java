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

/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client.chat;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 
 */
@NoArgsConstructor
@ToString
public class StaticContent {

	@RequiredArgsConstructor
	@Builder
	@Getter
	@Setter
	@ToString
	public static class ContentPart {
		private final String text;
		private final String type;
	}

	private String content = null;
	private List<ContentPart> contents = null;
	private final String type = "content";

	/**
	 * 
	 * @return Value set by {@link #setContent(String)}.
	 */
	public String getContent() {
		return content;
	}

	/**
	 * Set content to a single string. Notice that, if this is used, then
	 * {@link #getContents() will return null}.
	 * 
	 * @param content
	 */
	public void setContent(String content) {
		this.content = content;
		contents = null;
	}

	/**
	 * 
	 * @return Value set by {@link #getContents()}.
	 */
	public List<ContentPart> getContents() {
		return contents;
	}

	/**
	 * Set content to a list of content parts. Notice that, if this is used, then
	 * {@link #getContent() will return null}.
	 * 
	 * @param content
	 */
	public void setContents(List<ContentPart> contents) {
		this.contents = contents;
		content = null;
	}

	public static void main(String[] args) throws JsonProcessingException {

		StaticContent c = new StaticContent();

		c.setContent("Simple String");
		System.out.println(OpenAiClient.getJsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(c));

		List<ContentPart> p = new ArrayList<>();
		p.add(new ContentPart("Text 01", "content"));
		p.add(new ContentPart("Text 02", "content"));
		c.setContents(p);
		System.out.println(OpenAiClient.getJsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(c));

	}
}
