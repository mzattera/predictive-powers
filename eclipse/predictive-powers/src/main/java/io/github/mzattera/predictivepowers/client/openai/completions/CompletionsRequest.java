/**
 * 
 */
package io.github.mzattera.predictivepowers.client.openai.completions;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Request for /completions API.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@Getter
@Setter
@ToString
public final class CompletionsRequest extends CompletionsParameters {

	private final static ObjectMapper mapper;
	static {
		mapper = new ObjectMapper();
		mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
	}

	/**
	 * ID of the model to use. You can use the List models API to see all of your
	 * available models, or see our Model overview for descriptions of them.
	 */	
	@NonNull String model;

	/**
	 * Defaults to <|endoftext|> The prompt(s) to generate completions for, encoded
	 * as a string, array of strings, array of tokens, or array of token arrays.
	 * 
	 * Note that <|endoftext|> is the document separator that the model sees during
	 * training, so if a prompt is not specified the model will generate as if from
	 * the beginning of a new document.
	 */
	@NonNull String prompt;

	public static CompletionsRequest fromParameters(CompletionsParameters params) {
		try {
			return mapper.readValue(mapper.writeValueAsString(params), CompletionsRequest.class);
		} catch (Exception e) {
			return null;
		}
	}
}
