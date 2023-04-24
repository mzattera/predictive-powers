package io.github.mzattera.predictivepowers.openai.client.embeddings;

import java.util.List;

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
	List<Double> embedding;
	int index;
}
