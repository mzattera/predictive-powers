/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client.completions;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Response from /completions API.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
@Getter
@Setter
@ToString
public class CompletionsResponse {
	
    String id;
    String object;
    long created;
    String model;
    CompletionsChoice[] choices;
    Usage usage;
}
