package io.github.mzattera.predictivepowers.client.openai.embeddings;

import java.util.List;

import io.github.mzattera.predictivepowers.client.openai.Usage;
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
	List<Embedding> data;
	Usage usage;
}
