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
 * A single result from Google search API
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@Getter
@Setter
@ToString
public class Result {

	// TODO check all parameters are correctly provided

	@Getter
	@Setter
	@ToString
	public class Image {
		String contextLink;
		int height;
		int width;
		int byteSize;
		String thumbnailLink;
		int thumbnailHeight;
		int thumbnailWidth;
	}

	@Getter
	@Setter
	@ToString
	public class Label {
		String name;
		String displayName;
		String labelWithOp;
	}

	String kind;
	String title;
	String htmlTitle;
	String link;
	String displayLink;
	String snippet;
	String htmlSnippet;
	String cacheId;
	String formattedUrl;
	String htmlFormattedUrl;
	Object pagemap;
	String mime;
	String fileFormat;
	Image image;
	List<Label> labels;
}
