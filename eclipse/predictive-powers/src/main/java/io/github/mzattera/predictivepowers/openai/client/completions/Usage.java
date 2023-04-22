package io.github.mzattera.predictivepowers.openai.client.completions;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Usage data returned within a /completions response.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@Getter
@Setter
@ToString
public class Usage {
    Long promptTokens;
    Long completionTokens;
    Long totalTokens;
}
