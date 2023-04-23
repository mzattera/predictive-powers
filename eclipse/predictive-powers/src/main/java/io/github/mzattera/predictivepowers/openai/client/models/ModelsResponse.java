/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Response from /models API.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
@Getter
@Setter
@ToString
public class ModelsResponse {
	Model[] data;
	String object;
}
