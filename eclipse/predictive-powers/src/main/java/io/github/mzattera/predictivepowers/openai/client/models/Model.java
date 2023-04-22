/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * OpenAI Model data.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@Getter
@Setter
@ToString
public final class Model {
	String id;
	String object;
	String ownedBy;
	// TODO add permissions
}