/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Response from /files DELETE API.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
@Getter
@Setter
@ToString
public class DeleteResponse {

	String id;
	String object;
	boolean deleted;
}
