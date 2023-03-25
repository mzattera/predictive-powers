/**
 * 
 */
package io.github.mzattera.predictivepowers.client;

import lombok.ToString;

/**
 * OpenAI Model data.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@ToString
public class Model {
	public String id;
	public String object;
	public String ownedBy;
//    public List<Permission> permission;
}