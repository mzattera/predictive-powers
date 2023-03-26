/**
 * 
 */
package io.github.mzattera.predictivepowers.client.openai.completions;

import java.util.List;

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
    Long created;
    String model;
    List<CompletionsChoice> choices;
    Usage usage;
}
