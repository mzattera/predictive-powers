/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client.audio;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Response from OpenAi /audio API.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@Getter
@Setter
@ToString
public class AudioResponse {

	String text;
}
