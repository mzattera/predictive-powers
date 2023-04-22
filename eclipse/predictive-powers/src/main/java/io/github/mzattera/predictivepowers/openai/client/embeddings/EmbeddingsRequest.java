package io.github.mzattera.predictivepowers.openai.client.embeddings;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Parameters for a request to /embeddings API.
 * 
 * @author Massmiliano "Maxi" Zattera.
 *
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
@ToString
public class EmbeddingsRequest implements Cloneable {

	@NonNull
	String model;

	/**
	 * Input text to get embeddings for, encoded as a string.
	 * 
	 * To get embeddings for multiple inputs in a single request, pass multiple
	 * strings.
	 * 
	 * Each input must not exceed 8192 tokens in length.
	 */
	@NonNull
	@Builder.Default
	List<String> input = new ArrayList<>();

	String user;
}
