/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client.edits;

import java.util.List;

import io.github.mzattera.predictivepowers.openai.client.Usage;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Response from OpenAi /edits API.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@Getter
@Setter
@ToString
public class EditsResponse {

	public String object;
	public long created;
	public List<EditsChoice> choices;
	public Usage usage;
}
