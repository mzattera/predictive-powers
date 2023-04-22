package io.github.mzattera.predictivepowers.openai.client.embeddings;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * A single embedding from /embeddings API.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@Getter
@Setter
@ToString
public class Embedding {

	String object;
	double[] embedding;
	int index;
}
