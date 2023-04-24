/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Sometimes OpenAi API returns list of data i this format.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@Getter
@Setter
@ToString
public class DataList<T> {

	String object;
	List<T> data;
}
