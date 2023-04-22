/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client.edits;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Single choice returned by OpenAi /edits API.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@Getter
@Setter
@ToString
public class EditsChoice {

	String text;
	int index;
}
