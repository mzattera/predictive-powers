/**
 * 
 */
package io.github.mzattera.predictivepowers.client.openai.models;

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
// TODO: Add permissions[]
}