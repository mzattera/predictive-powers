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
package io.github.mzattera.predictivepowers.services;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

/**
 * A text that has been embedded.
 * 
 * In addition to the text itself, and its embedding, a set of arbitrary
 * properties can be attached to the embedded text.
 * 
 * Notice that two instances with same text are equal, regardless of their
 * properties, embeddings, or the model used to embed the text.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmbeddedText implements Serializable {

	private static final long serialVersionUID = -12432423425341702L;

	/**
	 * The piece of text that was embedded.
	 */
	@NonNull
	private String text;

	/**
	 * The actual embedding of the text.
	 */
	@NonNull
	private List<Double> embedding;

	/**
	 * Model used to embed the text.
	 */
	@NonNull
	private String model;

	private final Map<String, Object> properties = new HashMap<>();

	/**
	 * 
	 * @return Names of all properties for this embedded text.
	 */
	public Set<String> listProperties() {
		return properties.keySet();
	}

	/**
	 * 
	 * @param pName Name of the property.
	 * @return The value of given property (or null).
	 */
	public Object get(@NonNull String pName) {
		return properties.get(pName);
	}

	/**
	 * Sets a parameter for the property.
	 * 
	 * @param pName Name of the property.
	 * @param v     Value of the property (can be null).
	 * @return The value of the property.
	 */
	public Object set(@NonNull String pName, Object v) {
		return properties.put(pName, v);
	}

	/**
	 * @throws IllegalArgumentException if the two embeddings were not created by
	 *                                  using same model or have different embedding
	 *                                  length.
	 * @return cosine similarity in range [-1, 1] between this embedding and other.
	 */
	public double similarity(EmbeddedText other) {
		return similarity(this, other);
	}

	/**
	 * @throws IllegalArgumentException if the two embeddings were not created by
	 *                                  using same model or have different embedding
	 *                                  length.
	 * 
	 * @return cosine similarity in range [-1, 1] between two embeddings.
	 */
	public static double similarity(@NonNull EmbeddedText a, @NonNull EmbeddedText b) {

		if (!a.model.equals(b.model))
			throw new IllegalArgumentException(
					"Embedding from two different models [" + a.model + ", " + b.model + "]");
		if (a.embedding.size() != b.embedding.size())
			throw new IllegalArgumentException(
					"Embedding with different size [" + a.embedding.size() + ", " + b.embedding.size() + "]");

		double a2 = 0.0, b2 = 0.0, ab = 0.0;
		for (int i = 0; i < a.embedding.size(); ++i) {
			a2 += a.embedding.get(i) * a.embedding.get(i);
			b2 += b.embedding.get(i) * b.embedding.get(i);
			ab += a.embedding.get(i) * b.embedding.get(i);
		}

		double similarity = ab / Math.sqrt(a2 * b2);
		if (!Double.isFinite(similarity))
			return -1.0d; // Sometimes vectors are too small

		// Fix rounding errors, eventually
		return Math.max(Math.min(similarity, 1.0d), -1.0d);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (!(o instanceof EmbeddedText))
			return false;
		return ((EmbeddedText) o).text.equals(this.text);
	}

	@Override
	public int hashCode() {
		return text.hashCode();
	}

	// Avoid writing a long list of embeddings.
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("EmbeddedText [text=");
		builder.append(text);
		builder.append(", model=");
		builder.append(model);
		builder.append(", properties=");
		builder.append(properties);
		builder.append("]");
		return builder.toString();
	}
}
