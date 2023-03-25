package io.github.mzattera.predictivepowers.client.openai.completions;

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
    long promptTokens;
    long completionTokens;
    long totalTokens;
}
