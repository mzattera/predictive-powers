/**
 * 
 */
package io.github.mzattera.predictivepowers.client;

import java.util.List;

/**
 * A list of objects, as sometimes returned by OpenAI API.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
public class OpenAiList<T> {
	public List<T> data;
}
