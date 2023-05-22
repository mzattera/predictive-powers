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
package io.github.mzattera.autobot;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.extractor.ExtractorFactory;
import org.jsoup.Jsoup;

/**
 * Thsi class contains all data relative to a document, including all
 * augmentaion we did (e.g. QnA pairs associated to the document).
 * 
 * @author Massimiliano "Maxi"
 *
 */
public class Document {

	/**
	 * A question-answer pair.
	 * 
	 * @author Massimiliano "Maxi" Zattera
	 */
	public static class QnA {

		/**
		 * Pointer to the snipplets from where this QnA was generated.
		 */
		public final String snippet;

		public final String question;
		public final String answer;

		public QnA(String snippet, String question, String answer) {
			this.snippet = snippet;
			this.question = question;
			this.answer = answer;
		}

		/**
		 * Variations for the question.
		 */
		public List<String> variations = new ArrayList<>();

		private void setVariations(List<String> variations) {
			this.variations.clear();
			this.variations.addAll(variations);
		}
	}

	public final String text;

	private Document(String text) {
		this.text = GPT.normalizeText(text);
	}

	/**
	 * Factory method to get an instance from a string.
	 */
	public static Document fromString(String text) {
		return new Document(text);
	}

	/**
	 * Factory method to get an instance from a list of strings.
	 */
	public static Document fromString(String[] texts) {
		StringBuffer sb = new StringBuffer();
		for (String s : texts) {
			sb.append(s).append("\n\n");
		}

		return new Document(sb.toString());
	}

	/**
	 * Factory method to get an instance from a web page, by providing its URL.
	 * 
	 * @throws IOException
	 */
	public static Document fromURL(String url) throws IOException {
		return new Document(Jsoup.connect(url).get().text());
	}

	/**
	 * Factory method to get an instance from a list of web pages, by providing its
	 * URL.
	 * 
	 * @throws IOException
	 */
	public static Document fromURL(String[] urls) throws IOException {
		StringBuffer sb = new StringBuffer();
		for (String url : urls) {
			sb.append(Jsoup.connect(url).get().text()).append("\n\n");
		}

		return new Document(sb.toString());
	}

	/**
	 * Factory method to get an instance from an MS Office document.
	 * 
	 * @throws IOException
	 */
	public static Document fromOfficeFile(File fin) throws IOException {
		return new Document(ExtractorFactory.createExtractor(fin).getText());
	}

	/**
	 * Text broken down in pieces that can be handled in GPT-3 prompt.
	 */
	public List<QnA> qnas = new ArrayList<>();

	/**
	 * Adds Question & Answers pairs to this document.
	 */
	public void addQnA() {
		qnas = GPT.createQnAs(this);
	}

	/**
	 * Adds for each QnA pair, a list of variation on the question.
	 */
	public void addQuestionVariations() {
		for (QnA qna : qnas) {
			qna.setVariations(GPT.createUtterances(qna));
		}
	}
}
