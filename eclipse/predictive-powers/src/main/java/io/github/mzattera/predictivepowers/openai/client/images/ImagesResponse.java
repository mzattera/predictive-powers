/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client.images;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Response from OpenAi /images API.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@Getter
@Setter
@ToString
public class ImagesResponse {

	long created;
    Image[] data;
}
