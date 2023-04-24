/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client.finetunes;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * A fine-tuines event, as defined in the OpenAi API.
 * 
 * @author Massimiliano "Maxi" Zattera
 */
@Getter
@Setter
@ToString
public class FineTuneEvent {

	String object;
	long createdAt;
	String level;
	String message;
}
