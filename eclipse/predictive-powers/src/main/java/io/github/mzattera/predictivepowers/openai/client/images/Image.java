/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client.images;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * An image as returned by OpenAi /images API.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@Getter
@Setter
@ToString
public class Image {

	String url;
	String b64Json;
}
