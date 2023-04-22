package io.github.mzattera.predictivepowers.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * A text that has been embedded.
 * 
 * In addition to the text itself, and its embedding, a set of arbitrary
 * properties can be attached to the embedded text.
 * 
 * Notice that two instances with same text are qual, regardless of their
 * properties.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@Getter
@Setter
@AllArgsConstructor
@Builder
@ToString
public final class EmbeddedText {

	/**
	 * The piece of text that was embedded.
	 */
	@NonNull
	private String text;

	/**
	 * The actual embedding of the text.
	 */
	@NonNull
	private double[] embedding;

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
	 * 
	 * @param other
	 * @throws IllegalArgumentException if the two embeddings were not created by
	 *                                  using same model.
	 * @return cosine similarity between two embeddings.
	 */
	public static double similarity(@NonNull EmbeddedText one, @NonNull EmbeddedText other) {
		return one.similarity(other);
	}

	/**
	 * 
	 * @param other
	 * @throws IllegalArgumentException if the two embeddings were not created by
	 *                                  using same model.
	 * @return cosine similarity between this embedding and other.
	 */
	public double similarity(@NonNull EmbeddedText other) {
		if (!model.equals(other.model))
			throw new IllegalArgumentException("Embedding from twoi different models");

		double a2 = 0.0, b2 = 0.0, ab = 0.0;
		for (int i = 0; i < embedding.length; ++i) {
			a2 += embedding[i] * embedding[i];
			b2 += other.embedding[i] * other.embedding[i];
			ab += embedding[i] * other.embedding[i];
		}

		double similarity = ab / Math.sqrt(a2 * b2);
		if (Double.isNaN(similarity))
			return 0.0;

		return similarity;
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
}
