package io.github.mzattera.predictivepowers.client.openai.embeddings;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
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
@AllArgsConstructor
@ToString
public class EmbeddingsRequest implements Cloneable {

	/**
	 * Maximum number of tokens the default model can handle.
	 */
	public static final int MAX_DEFAULT_MODEL_TOKENS = 8192;
	
	@NonNull
	@Builder.Default
	String model = "text-embedding-ada-002";

	/**
	 * Input text to get embeddings for, encoded as a string.
	 * 
	 * To get embeddings for multiple inputs in a single request, pass an array of
	 * strings.
	 * 
	 * Each input must not exceed 8192 tokens in length.
	 */
	@NonNull
	List<String> input;

	String user;

	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) { // shall never happen
			return null;
		}
	}
}
