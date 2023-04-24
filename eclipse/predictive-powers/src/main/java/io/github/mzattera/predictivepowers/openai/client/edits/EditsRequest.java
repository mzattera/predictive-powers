/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client.edits;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Requests for OpenAI /edits API.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
@ToString
public class EditsRequest {

	@NonNull
	String model;
	
	String input;
	
	@NonNull
	String instruction;
	
	Integer n;
	Double temperature;
	Double topP;
}
