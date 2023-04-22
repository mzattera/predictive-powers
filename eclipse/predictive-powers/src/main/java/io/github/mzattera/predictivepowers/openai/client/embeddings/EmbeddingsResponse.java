package io.github.mzattera.predictivepowers.openai.client.embeddings;

import io.github.mzattera.predictivepowers.openai.client.completions.Usage;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Response from /embeddings API.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
@Getter
@Setter
@ToString
public class EmbeddingsResponse {

	String model;
	String object;
	Embedding[] data;
	Usage usage;
}
