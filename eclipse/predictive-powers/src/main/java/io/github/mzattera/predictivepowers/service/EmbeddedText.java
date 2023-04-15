package io.github.mzattera.predictivepowers.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * A piece of text that has been embedded.
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
	String text;

	/**
	 * The actual embedding of the text.
	 */
	double[] embedding;

	/**
	 * Model used to embed the text.
	 */
	String model;

	/**
	 * 
	 * @param other
	 * @return cosine similarity between this embedding and other.
	 */
	public static double similarity(@NonNull EmbeddedText one, @NonNull EmbeddedText other) {
		return one.similarity(other);
	}

	/**
	 * 
	 * @param other
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
}
